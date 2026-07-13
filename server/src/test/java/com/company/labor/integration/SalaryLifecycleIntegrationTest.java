package com.company.labor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.labor.LaborApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.List;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = LaborApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SalaryLifecycleIntegrationTest {
    private static final String ADMIN_USERNAME = "payroll_e2e_admin";
    private static final String ADMIN_PASSWORD = "payroll-e2e-admin-password";
    private static final String EMPLOYEE_USERNAME = "payroll_e2e_employee";
    private static final String EMPLOYEE_PASSWORD = "payroll-e2e-employee-password";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("laborflow_salary_e2e_it")
            .withUsername("laborflow_salary_e2e_it")
            .withPassword("laborflow_salary_e2e_it");

    @Autowired
    private ObjectMapper objectMapper;

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("laborflow.jwt.secret", () -> "salary-e2e-test-jwt-secret-with-more-than-thirty-two-bytes");
        registry.add("laborflow.bootstrap-admin.username", () -> ADMIN_USERNAME);
        registry.add("laborflow.bootstrap-admin.password", () -> ADMIN_PASSWORD);
    }

    @Test
    void completesSalaryLifecycleFromEmployeeOnboardingToPayslipConfirmation() throws Exception {
        String adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        long employeeId = post("/api/employees", Map.of(
                "name", "Payroll E2E Employee",
                "employeeType", "company_employee",
                "entryDate", "2031-08-01",
                "status", "active"), adminToken).path("data").path("id").asLong();
        assertThat(employeeId).isPositive();

        Map<String, Long> roleIds = roleIds(adminToken);
        createUser("payroll_e2e_boss", "payroll-e2e-boss-password", null, roleIds.get("BOSS"), adminToken);
        createUser("payroll_e2e_finance", "payroll-e2e-finance-password", null, roleIds.get("FINANCE"), adminToken);
        createUser(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD, employeeId, roleIds.get("EMPLOYEE"), adminToken);

        post("/api/salary/profiles", Map.of(
                "employeeId", employeeId,
                "effectiveDate", "2031-08-01",
                "status", "active",
                "items", List.of(
                        Map.of("salaryItemName", "Basic salary", "salaryItemType", "earning", "amount", new BigDecimal("1000.00")),
                        Map.of("salaryItemName", "Tax deduction", "salaryItemType", "deduction", "amount", new BigDecimal("100.00")))), adminToken);

        JsonNode monthly = post("/api/salary/monthly", Map.of(
                "salaryMonth", "2031-08",
                "title", "Payroll E2E August",
                "includeFixedSalary", 1,
                "includeAttendanceDeduction", 0,
                "includePerformanceSalary", 0,
                "includeProjectBonus", 0), adminToken).path("data");
        long monthlyId = monthly.path("id").asLong();
        assertThat(monthly.path("status").asText()).isEqualTo("draft");

        post("/api/salary/monthly/" + monthlyId + "/generate", null, adminToken);
        JsonNode detail = get("/api/salary/monthly/" + monthlyId + "/details?page=1&pageSize=20", adminToken)
                .path("data").path("records").get(0);
        long detailId = detail.path("id").asLong();
        assertThat(detail.path("employeeId").asLong()).isEqualTo(employeeId);
        assertThat(detail.path("payableAmount").asDouble()).isEqualTo(1000.0);
        assertThat(detail.path("deductionAmount").asDouble()).isEqualTo(100.0);
        assertThat(detail.path("actualAmount").asDouble()).isEqualTo(900.0);

        post("/api/salary/monthly/" + monthlyId + "/submit", null, adminToken);
        assertThat(get("/api/salary/monthly/" + monthlyId, adminToken).path("data").path("status").asText())
                .isEqualTo("pending_approval");

        String bossToken = login("payroll_e2e_boss", "payroll-e2e-boss-password");
        post("/api/salary/monthly/" + monthlyId + "/approve", Map.of("remark", "E2E approval"), bossToken);
        assertThat(get("/api/salary/monthly/" + monthlyId, adminToken).path("data").path("status").asText())
                .isEqualTo("approved");

        String financeToken = login("payroll_e2e_finance", "payroll-e2e-finance-password");
        post("/api/salary/monthly/" + monthlyId + "/mark-paid", Map.of(
                "paymentDate", "2031-08-31",
                "paymentMethod", "bank_transfer"), financeToken);
        assertThat(get("/api/salary/monthly/" + monthlyId, adminToken).path("data").path("status").asText())
                .isEqualTo("paid");

        String employeeToken = login(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD);
        JsonNode ownHistory = get("/api/statistics/salary/my-history", employeeToken).path("data");
        assertThat(ownHistory.path("records")).hasSize(1);
        assertThat(ownHistory.path("records").get(0).path("detailId").asLong()).isEqualTo(detailId);
        assertThat(ownHistory.path("records").get(0).path("paymentStatus").asText()).isEqualTo("paid");
        assertThat(ownHistory.path("records").get(0).path("payslipStatus").asText()).isEqualTo("unconfirmed");

        JsonNode ownDetail = get("/api/salary/details/" + detailId, employeeToken).path("data");
        assertThat(ownDetail.path("actualAmount").asDouble()).isEqualTo(900.0);
        assertThat(get("/api/salary/details/" + detailId + "/items", employeeToken).path("data")).hasSize(2);
        post("/api/salary/details/" + detailId + "/payslip/confirm", null, employeeToken);
        assertThat(get("/api/statistics/salary/my-history", employeeToken).path("data").path("records").get(0)
                .path("payslipStatus").asText()).isEqualTo("confirmed");

        List<String> actions = StreamSupport.stream(get("/api/salary/monthly/" + monthlyId + "/audits", adminToken)
                        .path("data").spliterator(), false)
                .map(record -> record.path("action").asText())
                .toList();
        assertThat(actions).contains("create_monthly", "generate_monthly", "submit_approval", "approve", "batch_payment", "monthly_paid", "confirm_payslip");
    }

    private Map<String, Long> roleIds(String token) throws Exception {
        return StreamSupport.stream(get("/api/system/roles?page=1&pageSize=20", token).path("data").path("records").spliterator(), false)
                .collect(java.util.stream.Collectors.toMap(
                        role -> role.path("code").asText(),
                        role -> role.path("id").asLong()));
    }

    private void createUser(String username, String password, Long employeeId, Long roleId, String token) throws Exception {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("username", username);
        payload.put("password", password);
        payload.put("nickname", username);
        payload.put("status", "active");
        payload.put("roleIds", List.of(roleId));
        if (employeeId != null) {
            payload.put("employeeId", employeeId);
        }
        post("/api/system/users", payload, token);
    }

    private String login(String username, String password) throws Exception {
        JsonNode body = post("/api/auth/login", Map.of("username", username, "password", password), null);
        String token = body.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private JsonNode get(String path, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build();
        return readSuccess(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    private JsonNode post(String path, Object payload, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        if (payload == null) {
            request.POST(HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json")
                    .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));
        }
        return readSuccess(httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString()));
    }

    private JsonNode readSuccess(HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.path("code").asInt()).isZero();
        return body;
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}

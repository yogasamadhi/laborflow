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
import java.util.LinkedHashMap;
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
class DepartmentalPayrollIntegrationTest {
    private static final String ADMIN_USERNAME = "departmental_payroll_admin";
    private static final String ADMIN_PASSWORD = "departmental-payroll-admin-password";
    private static final String SALARY_MONTH = "2033-01";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("laborflow_departmental_payroll_it")
            .withUsername("laborflow_departmental_payroll_it")
            .withPassword("laborflow_departmental_payroll_it");

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
        registry.add("laborflow.jwt.secret", () -> "departmental-payroll-jwt-secret-with-more-than-thirty-two-bytes");
        registry.add("laborflow.bootstrap-admin.username", () -> ADMIN_USERNAME);
        registry.add("laborflow.bootstrap-admin.password", () -> ADMIN_PASSWORD);
    }

    @Test
    void runsIndependentDepartmentPayrollAndProtectsEmployeeMonthUniqueness() throws Exception {
        String token = login();
        long departmentA = createDepartment("Departmental Payroll A", null, token);
        long parentDepartment = createDepartment("Departmental Payroll Parent", null, token);
        long departmentB = createDepartment("Departmental Payroll B", parentDepartment, token);
        long employeeMoving = createEmployee("Departmental Moving Employee", departmentA, token);
        long employeeStaying = createEmployee("Departmental Staying Employee", departmentA, token);
        long employeeB = createEmployee("Departmental B Employee", departmentB, token);
        long employeeWithoutDepartment = createEmployee("Departmental Unassigned Employee", null, token);

        createProfile(employeeMoving, "1000.00", token);
        createProfile(employeeStaying, "1100.00", token);
        createProfile(employeeB, "1200.00", token);
        JsonNode filteredProfiles = get("/api/salary/profiles?departmentId=" + departmentA
                + "&keyword=Moving&page=1&pageSize=20", token).path("data").path("records");
        assertThat(filteredProfiles).hasSize(1);
        assertThat(filteredProfiles.get(0).path("employeeName").asText()).isEqualTo("Departmental Moving Employee");
        assertThat(filteredProfiles.get(0).path("departmentName").asText()).isEqualTo("Departmental Payroll A");
        assertThat(filteredProfiles.get(0).path("earningAmount").decimalValue()).isEqualByComparingTo("1000.00");
        assertThat(filteredProfiles.get(0).path("itemCount").asInt()).isEqualTo(1);
        assertBusinessError(postRaw("/api/salary/profiles", profileRequest(employeeWithoutDepartment, "900.00"), token),
                "员工未设置直属部门，不能启用工资档案");
        assertBusinessError(postRaw("/api/salary/monthly", monthlyRequest(parentDepartment), token),
                "该部门没有可生成工资的在职员工档案");
        assertBusinessError(postRaw("/api/salary/monthly", Map.of(
                "salaryMonth", SALARY_MONTH,
                "departmentId", departmentA,
                "includeFixedSalary", 0,
                "includeAttendanceDeduction", 0,
                "includePerformanceSalary", 0,
                "includeProjectBonus", 0), token), "至少启用一项工资生成规则");

        JsonNode monthlyA = post("/api/salary/monthly", monthlyRequest(departmentA), token).path("data");
        JsonNode monthlyB = post("/api/salary/monthly", monthlyRequest(departmentB), token).path("data");
        long monthlyAId = monthlyA.path("id").asLong();
        long monthlyBId = monthlyB.path("id").asLong();
        assertThat(monthlyB.path("title").asText()).isEqualTo(SALARY_MONTH + " Departmental Payroll B工资表");
        JsonNode unchangedScope = put("/api/salary/monthly/" + monthlyAId, Map.of(
                "salaryMonth", "2034-02",
                "departmentId", departmentB,
                "title", SALARY_MONTH + " Departmental Payroll A工资表"), token).path("data");
        assertThat(unchangedScope.path("salaryMonth").asText()).isEqualTo(SALARY_MONTH);
        assertThat(unchangedScope.path("departmentId").asLong()).isEqualTo(departmentA);
        JsonNode filteredMonthlies = get("/api/salary/monthly?salaryMonth=" + SALARY_MONTH
                + "&departmentId=" + departmentB, token).path("data").path("records");
        assertThat(filteredMonthlies).hasSize(1);
        assertThat(filteredMonthlies.get(0).path("departmentName").asText()).isEqualTo("Departmental Payroll B");
        assertBusinessError(postRaw("/api/salary/monthly", monthlyRequest(departmentA), token),
                "该部门当月工资表已存在，请勿重复创建");

        post("/api/salary/monthly/" + monthlyAId + "/generate", null, token);
        post("/api/salary/monthly/" + monthlyBId + "/generate", null, token);
        JsonNode detailsA = monthlyDetails(monthlyAId, token);
        JsonNode detailsB = monthlyDetails(monthlyBId, token);
        assertThat(detailsA).hasSize(2);
        assertThat(detailsB).hasSize(1);
        assertThat(StreamSupport.stream(detailsA.spliterator(), false)
                .allMatch(detail -> detail.path("departmentId").asLong() == departmentA)).isTrue();
        assertThat(detailsB.get(0).path("employeeId").asLong()).isEqualTo(employeeB);
        assertThat(detailsB.get(0).path("departmentId").asLong()).isEqualTo(departmentB);

        updateEmployee(employeeMoving, "Departmental Moving Employee", departmentB, token);
        JsonNode movingSnapshot = StreamSupport.stream(detailsA.spliterator(), false)
                .filter(detail -> detail.path("employeeId").asLong() == employeeMoving)
                .findFirst()
                .orElseThrow();
        assertThat(get("/api/salary/details/" + movingSnapshot.path("id").asLong(), token)
                .path("data").path("departmentId").asLong()).isEqualTo(departmentA);
        assertBusinessError(postRaw("/api/salary/monthly/" + monthlyBId + "/generate", null, token),
                "已进入工资表“" + SALARY_MONTH + " Departmental Payroll A工资表”，请先重新生成该工资表移除该员工");
        assertThat(monthlyDetails(monthlyBId, token)).hasSize(1);

        post("/api/salary/monthly/" + monthlyAId + "/generate", null, token);
        post("/api/salary/monthly/" + monthlyBId + "/generate", null, token);
        assertThat(monthlyDetails(monthlyAId, token)).hasSize(1);
        assertThat(monthlyDetails(monthlyBId, token)).hasSize(2);

        for (long monthlyId : List.of(monthlyAId, monthlyBId)) {
            post("/api/salary/monthly/" + monthlyId + "/submit", null, token);
            post("/api/salary/monthly/" + monthlyId + "/approve", Map.of("remark", "Departmental approval"), token);
            post("/api/salary/monthly/" + monthlyId + "/mark-paid", Map.of(
                    "paymentDate", "2033-01-31",
                    "paymentMethod", "bank_transfer"), token);
            assertThat(get("/api/salary/monthly/" + monthlyId, token).path("data").path("status").asText())
                    .isEqualTo("paid");
        }

        long departmentC = createDepartment("Departmental Payroll C", null, token);
        updateEmployee(employeeStaying, "Departmental Staying Employee", departmentC, token);
        long monthlyCId = post("/api/salary/monthly", monthlyRequest(departmentC), token).path("data").path("id").asLong();
        assertBusinessError(postRaw("/api/salary/monthly/" + monthlyCId + "/generate", null, token),
                "已进入工资表“" + SALARY_MONTH + " Departmental Payroll A工资表”（状态：paid），不能在新部门工资表重复生成");

        JsonNode overview = get("/api/statistics/salary/overview", token).path("data");
        assertThat(overview.path("actualAmount").decimalValue()).isEqualByComparingTo("3300.00");
        assertThat(overview.path("paidCount").asInt()).isEqualTo(3);
        JsonNode departmentOverview = get("/api/statistics/salary/overview?departmentId=" + departmentA, token).path("data");
        assertThat(departmentOverview.path("actualAmount").decimalValue()).isEqualByComparingTo("1100.00");
        JsonNode trend = get("/api/statistics/salary/monthly-trend", token).path("data");
        assertThat(trend).hasSize(1);
        assertThat(trend.get(0).path("actualAmount").decimalValue()).isEqualByComparingTo("3300.00");
        JsonNode departmentSummary = get("/api/statistics/salary/department-summary", token).path("data");
        assertThat(departmentSummary).hasSize(2);
        assertThat(StreamSupport.stream(departmentSummary.spliterator(), false)
                .map(row -> row.path("departmentId").asLong()).toList())
                .containsExactlyInAnyOrder(departmentA, departmentB);
        JsonNode dashboard = get("/api/dashboard/overview?month=" + SALARY_MONTH, token).path("data").path("salary");
        assertThat(dashboard.path("actualAmount").decimalValue()).isEqualByComparingTo("3300.00");
        assertBusinessError(deleteRaw("/api/departments/" + departmentA, token), "月度工资表");
    }

    private long createDepartment(String name, Long parentId, String token) throws Exception {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", name);
        request.put("parentId", parentId);
        request.put("status", "active");
        return id(post("/api/departments", request, token));
    }

    private long createEmployee(String name, Long departmentId, String token) throws Exception {
        Map<String, Object> request = employeeRequest(name, departmentId);
        return id(post("/api/employees", request, token));
    }

    private void updateEmployee(long employeeId, String name, Long departmentId, String token) throws Exception {
        put("/api/employees/" + employeeId, employeeRequest(name, departmentId), token);
    }

    private Map<String, Object> employeeRequest(String name, Long departmentId) {
        Map<String, Object> request = new LinkedHashMap<>();
        request.put("name", name);
        request.put("departmentId", departmentId);
        request.put("employeeType", "company_employee");
        request.put("entryDate", "2033-01-01");
        request.put("status", "active");
        return request;
    }

    private void createProfile(long employeeId, String amount, String token) throws Exception {
        post("/api/salary/profiles", profileRequest(employeeId, amount), token);
    }

    private Map<String, Object> profileRequest(long employeeId, String amount) {
        return Map.of(
                "employeeId", employeeId,
                "effectiveDate", "2033-01-01",
                "status", "active",
                "items", List.of(Map.of(
                        "salaryItemName", "Departmental fixed salary",
                        "salaryItemType", "earning",
                        "amount", new BigDecimal(amount))));
    }

    private Map<String, Object> monthlyRequest(long departmentId) {
        return Map.of(
                "salaryMonth", SALARY_MONTH,
                "departmentId", departmentId,
                "includeFixedSalary", 1,
                "includeAttendanceDeduction", 0,
                "includePerformanceSalary", 0,
                "includeProjectBonus", 0);
    }

    private JsonNode monthlyDetails(long monthlyId, String token) throws Exception {
        return get("/api/salary/monthly/" + monthlyId + "/details?page=1&pageSize=20", token)
                .path("data").path("records");
    }

    private long id(JsonNode response) {
        long id = response.path("data").path("id").asLong();
        assertThat(id).isPositive();
        return id;
    }

    private String login() throws Exception {
        JsonNode body = post("/api/auth/login", Map.of(
                "username", ADMIN_USERNAME,
                "password", ADMIN_PASSWORD), null);
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
        return readSuccess(postRaw(path, payload, token));
    }

    private JsonNode put(String path, Object payload, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        return readSuccess(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    private HttpResponse<String> postRaw(String path, Object payload, String token) throws Exception {
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
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> deleteRaw(String path, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + token)
                .DELETE()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private JsonNode readSuccess(HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).as(response.body()).isEqualTo(HttpStatus.OK.value());
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.path("code").asInt()).as(response.body()).isZero();
        return body;
    }

    private void assertBusinessError(HttpResponse<String> response, String message) throws Exception {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.path("code").asInt()).isNotZero();
        assertThat(body.path("message").asText()).contains(message);
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}

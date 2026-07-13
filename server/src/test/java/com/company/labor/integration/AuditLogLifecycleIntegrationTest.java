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
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = LaborApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class AuditLogLifecycleIntegrationTest {
    private static final String ADMIN_USERNAME = "audit_log_e2e_admin";
    private static final String ADMIN_PASSWORD = "audit-log-e2e-admin-password";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("laborflow_audit_log_e2e_it")
            .withUsername("laborflow_audit_log_e2e_it")
            .withPassword("laborflow_audit_log_e2e_it");

    @Autowired
    private JdbcTemplate jdbcTemplate;

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
        registry.add("laborflow.jwt.secret", () -> "audit-log-e2e-jwt-secret-with-more-than-thirty-two-bytes");
        registry.add("laborflow.bootstrap-admin.username", () -> ADMIN_USERNAME);
        registry.add("laborflow.bootstrap-admin.password", () -> ADMIN_PASSWORD);
    }

    @Test
    void recordsQueryableAuditEntriesForSalaryProjectCompensationUserAndWechatActions() throws Exception {
        String adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        long employeeId = id(post("/api/employees", Map.of(
                "name", "Audit Log E2E Employee",
                "employeeType", "company_employee",
                "entryDate", "2032-01-01",
                "status", "active"), adminToken));
        Map<String, Long> roleIds = roleIds(adminToken);
        long userId = createUser("audit_log_e2e_user", employeeId, roleIds.get("EMPLOYEE"), adminToken);
        updateUser(userId, employeeId, roleIds.get("EMPLOYEE"), adminToken);

        createSalaryProfile(employeeId, adminToken);
        long monthlyId = createAndUpdateMonthly(adminToken);
        post("/api/salary/monthly/" + monthlyId + "/generate", null, adminToken);
        post("/api/salary/monthly/" + monthlyId + "/submit", null, adminToken);
        post("/api/salary/monthly/" + monthlyId + "/reject", Map.of("remark", "Audit E2E reject"), adminToken);
        post("/api/salary/monthly/" + monthlyId + "/submit", null, adminToken);
        post("/api/salary/monthly/" + monthlyId + "/approve", Map.of("remark", "Audit E2E approve"), adminToken);
        post("/api/salary/monthly/" + monthlyId + "/mark-paid", Map.of(
                "paymentDate", "2032-01-31",
                "paymentMethod", "bank_transfer"), adminToken);
        getBinary("/api/salary/monthly/" + monthlyId + "/export", adminToken);

        long projectId = id(post("/api/projects", Map.of(
                "name", "Audit Log E2E Project",
                "contractAmount", new BigDecimal("10000.00"),
                "status", "in_progress"), adminToken));
        long rejectedVariationId = createVariation(projectId, "2032-01-10", adminToken);
        post("/api/variation-orders/" + rejectedVariationId + "/submit", null, adminToken);
        post("/api/variation-orders/" + rejectedVariationId + "/reject", Map.of("remark", "Audit E2E project reject"), adminToken);
        long confirmedVariationId = createVariation(projectId, "2032-01-11", adminToken);
        post("/api/variation-orders/" + confirmedVariationId + "/submit", null, adminToken);
        post("/api/variation-orders/" + confirmedVariationId + "/confirm", Map.of(
                "confirmedAmount", new BigDecimal("120.00"),
                "remark", "Audit E2E project confirm"), adminToken);

        long deductionId = id(post("/api/compensation/deductions", Map.of(
                "employeeId", employeeId,
                "deductionMonth", "2032-01",
                "deductionType", "social_security",
                "salaryItemName", "Audit E2E deduction",
                "amount", new BigDecimal("10.00"),
                "status", "active"), adminToken));
        put("/api/compensation/deductions/" + deductionId, Map.of(
                "employeeId", employeeId,
                "deductionMonth", "2032-01",
                "deductionType", "social_security",
                "salaryItemName", "Audit E2E deduction updated",
                "amount", new BigDecimal("12.00"),
                "status", "active"), adminToken);
        long scoreId = id(post("/api/compensation/scores", Map.of(
                "performanceMonth", "2032-01",
                "employeeId", employeeId,
                "score", new BigDecimal("90.00"),
                "performanceAmount", new BigDecimal("200.00")), adminToken));
        post("/api/compensation/scores/" + scoreId + "/confirm", null, adminToken);

        long bindingId = 9_300_001L;
        jdbcTemplate.update("INSERT INTO wechat_user_binding (id, openid, status, deleted) VALUES (?, ?, 'pending', 0)",
                bindingId, "audit-log-e2e-openid");
        post("/api/system/wechat-bindings/" + bindingId + "/bind", Map.of(
                "userId", userId,
                "employeeId", employeeId,
                "nickname", "Audit binding"), adminToken);
        post("/api/system/wechat-bindings/" + bindingId + "/unbind", null, adminToken);

        JsonNode logs = get("/api/system/logs?page=1&pageSize=100", adminToken).path("data").path("records");
        assertLog(logs, "system_user", userId, "create_user");
        assertLog(logs, "system_user", userId, "update_user");
        assertLog(logs, "salary", monthlyId, "create_monthly");
        assertLog(logs, "salary", monthlyId, "update_monthly");
        assertLog(logs, "salary", monthlyId, "generate_monthly");
        assertLog(logs, "salary", monthlyId, "submit_approval");
        assertLog(logs, "salary", monthlyId, "reject");
        assertLog(logs, "salary", monthlyId, "approve");
        assertLog(logs, "salary", monthlyId, "batch_payment");
        assertLog(logs, "salary", monthlyId, "export_monthly");
        assertLog(logs, "project", rejectedVariationId, "submit_variation_order");
        assertLog(logs, "project", rejectedVariationId, "reject_variation_order");
        assertLog(logs, "project", confirmedVariationId, "confirm_variation_order");
        assertLog(logs, "compensation", deductionId, "save_salary_deduction");
        assertLog(logs, "compensation", deductionId, "update_salary_deduction");
        assertLog(logs, "compensation", scoreId, "confirm_score");
        assertLog(logs, "wechat_binding", bindingId, "admin_bind");
        assertLog(logs, "wechat_binding", bindingId, "unbind");
    }

    private long createAndUpdateMonthly(String token) throws Exception {
        Map<String, Object> request = monthlyRequest("Audit Log January");
        long monthlyId = id(post("/api/salary/monthly", request, token));
        put("/api/salary/monthly/" + monthlyId, monthlyRequest("Audit Log January updated"), token);
        return monthlyId;
    }

    private Map<String, Object> monthlyRequest(String title) {
        return Map.of(
                "salaryMonth", "2032-01",
                "title", title,
                "includeFixedSalary", 1,
                "includeAttendanceDeduction", 0,
                "includePerformanceSalary", 0,
                "includeProjectBonus", 0);
    }

    private long createVariation(long projectId, String occurrenceDate, String token) throws Exception {
        return id(post("/api/variation-orders", Map.of(
                "projectId", projectId,
                "occurrenceDate", occurrenceDate,
                "variationType", "现场签证",
                "estimatedAmount", new BigDecimal("150.00")), token));
    }

    private void createSalaryProfile(long employeeId, String token) throws Exception {
        post("/api/salary/profiles", Map.of(
                "employeeId", employeeId,
                "effectiveDate", "2032-01-01",
                "status", "active",
                "items", List.of(Map.of(
                        "salaryItemName", "Audit E2E fixed salary",
                        "salaryItemType", "earning",
                        "amount", new BigDecimal("1000.00")))), token);
    }

    private long createUser(String username, long employeeId, long roleId, String token) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", username);
        payload.put("password", "audit-log-e2e-user-password");
        payload.put("nickname", username);
        payload.put("employeeId", employeeId);
        payload.put("status", "active");
        payload.put("roleIds", List.of(roleId));
        return id(post("/api/system/users", payload, token));
    }

    private void updateUser(long userId, long employeeId, long roleId, String token) throws Exception {
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("username", "audit_log_e2e_user");
        payload.put("nickname", "Audit Log E2E User Updated");
        payload.put("employeeId", employeeId);
        payload.put("status", "active");
        payload.put("roleIds", List.of(roleId));
        put("/api/system/users/" + userId, payload, token);
    }

    private Map<String, Long> roleIds(String token) throws Exception {
        return StreamSupport.stream(get("/api/system/roles?page=1&pageSize=20", token).path("data").path("records").spliterator(), false)
                .collect(Collectors.toMap(role -> role.path("code").asText(), role -> role.path("id").asLong()));
    }

    private void assertLog(JsonNode logs, String module, long businessId, String action) {
        for (JsonNode log : logs) {
            if (module.equals(log.path("module").asText())
                    && businessId == log.path("businessId").asLong()
                    && action.equals(log.path("action").asText())) {
                assertThat(log.path("operatorName").asText()).isEqualTo(ADMIN_USERNAME);
                assertThat(log.path("operatorId").asLong()).isPositive();
                assertThat(log.path("content").asText()).isNotBlank();
                return;
            }
        }
        assertThat(false).withFailMessage("missing audit log %s/%s/%s", module, businessId, action).isTrue();
    }

    private long id(JsonNode response) {
        long id = response.path("data").path("id").asLong();
        assertThat(id).isPositive();
        return id;
    }

    private String login(String username, String password) throws Exception {
        String token = post("/api/auth/login", Map.of("username", username, "password", password), null)
                .path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private JsonNode get(String path, String token) throws Exception {
        HttpResponse<String> response = send(HttpRequest.newBuilder(uri(path)).GET(), token);
        assertThat(response.statusCode()).as("response body: %s", response.body()).isEqualTo(HttpStatus.OK.value());
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.path("code").asInt()).isZero();
        return body;
    }

    private JsonNode post(String path, Object payload, String token) throws Exception {
        return write("POST", path, payload, token);
    }

    private JsonNode put(String path, Object payload, String token) throws Exception {
        return write("PUT", path, payload, token);
    }

    private JsonNode write(String method, String path, Object payload, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path));
        String body = payload == null ? null : objectMapper.writeValueAsString(payload);
        if (body == null) {
            if ("POST".equals(method)) request.POST(HttpRequest.BodyPublishers.noBody());
            else request.PUT(HttpRequest.BodyPublishers.noBody());
        } else {
            request.header("Content-Type", "application/json");
            if ("POST".equals(method)) request.POST(HttpRequest.BodyPublishers.ofString(body));
            else request.PUT(HttpRequest.BodyPublishers.ofString(body));
        }
        HttpResponse<String> response = send(request, token);
        assertThat(response.statusCode()).as("response body: %s", response.body()).isEqualTo(HttpStatus.OK.value());
        JsonNode result = objectMapper.readTree(response.body());
        assertThat(result.path("code").asInt()).isZero();
        return result;
    }

    private void getBinary(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (token != null) request.header("Authorization", "Bearer " + token);
        HttpResponse<byte[]> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofByteArray());
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).isNotEmpty();
    }

    private HttpResponse<String> send(HttpRequest.Builder request, String token) throws Exception {
        if (token != null) request.header("Authorization", "Bearer " + token);
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}

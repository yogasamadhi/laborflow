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
class AccessControlIntegrationTest {
    private static final String ADMIN_USERNAME = "access_control_e2e_admin";
    private static final String ADMIN_PASSWORD = "access-control-e2e-admin-password";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("laborflow_access_control_e2e_it")
            .withUsername("laborflow_access_control_e2e_it")
            .withPassword("laborflow_access_control_e2e_it");

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
        registry.add("laborflow.jwt.secret", () -> "access-control-e2e-jwt-secret-with-more-than-thirty-two-bytes");
        registry.add("laborflow.bootstrap-admin.username", () -> ADMIN_USERNAME);
        registry.add("laborflow.bootstrap-admin.password", () -> ADMIN_PASSWORD);
    }

    @Test
    void enforcesRoleProjectAndOwnSalaryBoundariesWithoutLeakingUserPasswords() throws Exception {
        assertError(getRaw("/api/system/users", null), HttpStatus.UNAUTHORIZED, "未登录或登录已过期");

        String adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        Map<String, Long> roleIds = roleIds(adminToken);
        long departmentId = id(post("/api/departments", Map.of(
                "name", "Access E2E Department",
                "status", "active"), adminToken));
        long employeeA = createEmployee("Access E2E Employee A", departmentId, adminToken);
        long employeeB = createEmployee("Access E2E Employee B", departmentId, adminToken);
        createUser("access_e2e_employee_a", "access-e2e-employee-a-password", employeeA, roleIds.get("EMPLOYEE"), adminToken);
        createUser("access_e2e_employee_b", "access-e2e-employee-b-password", employeeB, roleIds.get("EMPLOYEE"), adminToken);
        createUser("access_e2e_finance", "access-e2e-finance-password", null, roleIds.get("FINANCE"), adminToken);
        createUser("access_e2e_manager", "access-e2e-manager-password", null, roleIds.get("MANAGER"), adminToken);

        long projectId = id(post("/api/projects", Map.of(
                "name", "Access Control E2E Project",
                "contractAmount", new BigDecimal("10000.00"),
                "status", "in_progress"), adminToken));
        jdbcTemplate.update("""
                INSERT INTO project_member (id, project_id, employee_id, role_name, status, deleted)
                VALUES (?, ?, ?, '现场成员', 'active', 0)
                """, 9_200_001L, projectId, employeeA);
        JsonNode member = get("/api/project-members?projectId=" + projectId + "&keyword=Employee+A", adminToken)
                .path("data").path("records").get(0);
        assertThat(member.path("employeeId").asLong()).isEqualTo(employeeA);
        assertThat(member.path("employeeName").asText()).isEqualTo("Access E2E Employee A");
        assertThat(member.path("employeeStatus").asText()).isEqualTo("active");
        assertThat(member.path("joinedAt").asText()).isNotBlank();
        assertThat(member.has("userId")).isFalse();
        assertThat(member.has("deleted")).isFalse();
        assertError(postRaw("/api/project-members", objectMapper.writeValueAsString(Map.of(
                "projectId", projectId,
                "employeeId", employeeA,
                "roleName", "重复成员",
                "status", "active")), adminToken), HttpStatus.BAD_REQUEST, "已是项目成员");

        JsonNode referenceEmployee = get("/api/reference/employees?keyword=Access+E2E+Employee+A", adminToken)
                .path("data").path("records").get(0);
        assertThat(referenceEmployee.path("name").asText()).isEqualTo("Access E2E Employee A");
        assertThat(referenceEmployee.has("phone")).isFalse();
        assertThat(referenceEmployee.has("idCardNo")).isFalse();
        assertThat(referenceEmployee.has("bankAccount")).isFalse();

        createProfile(employeeA, adminToken);
        createProfile(employeeB, adminToken);
        long monthlyId = id(post("/api/salary/monthly", Map.of(
                "salaryMonth", "2031-12",
                "departmentId", departmentId,
                "title", "Access Control E2E",
                "includeFixedSalary", 1,
                "includeAttendanceDeduction", 0,
                "includePerformanceSalary", 0,
                "includeProjectBonus", 0), adminToken));
        post("/api/salary/monthly/" + monthlyId + "/generate", null, adminToken);
        Map<Long, Long> detailIds = StreamSupport.stream(get("/api/salary/monthly/" + monthlyId + "/details?page=1&pageSize=20", adminToken)
                        .path("data").path("records").spliterator(), false)
                .collect(Collectors.toMap(detail -> detail.path("employeeId").asLong(), detail -> detail.path("id").asLong()));

        String employeeAToken = login("access_e2e_employee_a", "access-e2e-employee-a-password");
        assertThat(get("/api/salary/details/" + detailIds.get(employeeA), employeeAToken).path("data").path("employeeId").asLong())
                .isEqualTo(employeeA);
        assertError(getRaw("/api/salary/details/" + detailIds.get(employeeB), employeeAToken), HttpStatus.BAD_REQUEST, "无权查看该工资明细");
        assertError(getRaw("/api/system/users", employeeAToken), HttpStatus.FORBIDDEN, "无权操作");
        assertError(getRaw("/api/salary/monthly/" + monthlyId + "/export", employeeAToken), HttpStatus.FORBIDDEN, "无权操作");
        assertError(getRaw("/api/project-members?page=1&pageSize=20", employeeAToken), HttpStatus.FORBIDDEN, "无权操作");
        assertError(getRaw("/api/reference/employees?page=1&pageSize=20", employeeAToken), HttpStatus.FORBIDDEN, "无权操作");
        JsonNode employeeDashboard = get("/api/dashboard/overview?month=2031-12", employeeAToken).path("data");
        assertThat(employeeDashboard.path("capabilities").path("employeeHandoff").asBoolean()).isTrue();
        assertThat(employeeDashboard.path("capabilities").path("salary").asBoolean()).isFalse();
        assertThat(employeeDashboard.path("salary").isMissingNode() || employeeDashboard.path("salary").isNull()).isTrue();
        assertThat(employeeDashboard.path("projects").isMissingNode() || employeeDashboard.path("projects").isNull()).isTrue();
        assertThat(get("/api/todos?page=1&pageSize=20", employeeAToken)
                .path("data").path("records")).isEmpty();
        List<String> projectManageReadPaths = List.of(
                "/api/machinery-equipments?page=1&pageSize=20",
                "/api/machinery-work-records?page=1&pageSize=20",
                "/api/material-records?page=1&pageSize=20",
                "/api/project-document-archives?page=1&pageSize=20");
        for (String path : projectManageReadPaths) {
            assertError(getRaw(path, employeeAToken), HttpStatus.FORBIDDEN, "无权操作");
        }

        assertThat(post("/api/variation-orders", Map.of(
                "projectId", projectId,
                "occurrenceDate", "2031-12-10",
                "variationType", "现场签证",
                "estimatedAmount", new BigDecimal("100.00")), employeeAToken)
                .path("data").path("status").asText()).isEqualTo("draft");
        String employeeBToken = login("access_e2e_employee_b", "access-e2e-employee-b-password");
        assertError(postRaw("/api/variation-orders", objectMapper.writeValueAsString(Map.of(
                "projectId", projectId,
                "occurrenceDate", "2031-12-10",
                "variationType", "现场签证",
                "estimatedAmount", new BigDecimal("100.00"))), employeeBToken), HttpStatus.BAD_REQUEST, "无权访问该项目");

        String financeToken = login("access_e2e_finance", "access-e2e-finance-password");
        JsonNode departmentReference = get("/api/reference/departments?status=active", financeToken)
                .path("data").path("records").get(0);
        assertThat(departmentReference.path("id").asLong()).isEqualTo(departmentId);
        assertThat(departmentReference.path("name").asText()).isEqualTo("Access E2E Department");
        assertError(postRaw("/api/departments", objectMapper.writeValueAsString(Map.of(
                "name", "Finance cannot create department",
                "status", "active")), financeToken), HttpStatus.FORBIDDEN, "无权操作");
        JsonNode financeDashboard = get("/api/dashboard/overview?month=2031-12", financeToken).path("data");
        assertThat(financeDashboard.path("capabilities").path("salary").asBoolean()).isTrue();
        assertThat(financeDashboard.path("capabilities").path("projects").asBoolean()).isTrue();
        assertThat(financeDashboard.path("capabilities").path("compensation").asBoolean()).isTrue();
        assertError(postRaw("/api/salary/monthly/" + monthlyId + "/approve", "{}", financeToken), HttpStatus.FORBIDDEN, "无权操作");
        for (String path : projectManageReadPaths) {
            assertError(getRaw(path, financeToken), HttpStatus.FORBIDDEN, "无权操作");
        }
        String managerToken = login("access_e2e_manager", "access-e2e-manager-password");
        assertThat(get("/api/dashboard/overview?month=2031-12", managerToken)
                .path("data").path("capabilities").path("projects").asBoolean()).isTrue();
        assertError(getRaw("/api/salary/monthly", managerToken), HttpStatus.FORBIDDEN, "无权操作");
        for (String path : projectManageReadPaths) {
            get(path, managerToken);
        }

        put("/api/employees/" + employeeA + "/status", Map.of("status", "inactive"), adminToken);
        assertError(getRaw("/api/salary/details/" + detailIds.get(employeeA), employeeAToken), HttpStatus.UNAUTHORIZED, "账号已停用");
        assertError(postRaw("/api/auth/login", objectMapper.writeValueAsString(Map.of(
                "username", "access_e2e_employee_a",
                "password", "access-e2e-employee-a-password")), null), HttpStatus.UNAUTHORIZED, "用户已失效");
        put("/api/employees/" + employeeA + "/status", Map.of("status", "active"), adminToken);
        get("/api/salary/details/" + detailIds.get(employeeA), employeeAToken);

        JsonNode users = get("/api/system/users?page=1&pageSize=20", adminToken).path("data").path("records");
        assertThat(users).isNotEmpty();
        for (JsonNode user : users) {
            assertThat(user.has("password")).isFalse();
            assertThat(user.has("lastLoginIp")).isFalse();
        }
    }

    private long createEmployee(String name, long departmentId, String token) throws Exception {
        return id(post("/api/employees", Map.of(
                "name", name,
                "departmentId", departmentId,
                "employeeType", "company_employee",
                "entryDate", "2031-12-01",
                "status", "active"), token));
    }

    private void createProfile(long employeeId, String token) throws Exception {
        post("/api/salary/profiles", Map.of(
                "employeeId", employeeId,
                "effectiveDate", "2031-12-01",
                "status", "active",
                "items", List.of(Map.of(
                        "salaryItemName", "Access E2E fixed salary",
                        "salaryItemType", "earning",
                        "amount", new BigDecimal("1000.00")))), token);
    }

    private Map<String, Long> roleIds(String token) throws Exception {
        return StreamSupport.stream(get("/api/system/roles?page=1&pageSize=20", token).path("data").path("records").spliterator(), false)
                .collect(Collectors.toMap(role -> role.path("code").asText(), role -> role.path("id").asLong()));
    }

    private long createUser(String username, String password, Long employeeId, Long roleId, String token) throws Exception {
        Map<String, Object> payload = new java.util.LinkedHashMap<>();
        payload.put("username", username);
        payload.put("password", password);
        payload.put("nickname", username);
        payload.put("status", "active");
        payload.put("roleIds", List.of(roleId));
        if (employeeId != null) {
            payload.put("employeeId", employeeId);
        }
        return id(post("/api/system/users", payload, token));
    }

    private long id(JsonNode response) {
        long id = response.path("data").path("id").asLong();
        assertThat(id).isPositive();
        return id;
    }

    private String login(String username, String password) throws Exception {
        JsonNode body = post("/api/auth/login", Map.of("username", username, "password", password), null);
        String token = body.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private JsonNode get(String path, String token) throws Exception {
        HttpResponse<String> response = getRaw(path, token);
        assertThat(response.statusCode()).as("response body: %s", response.body()).isEqualTo(HttpStatus.OK.value());
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.path("code").asInt()).isZero();
        return body;
    }

    private JsonNode post(String path, Object payload, String token) throws Exception {
        HttpResponse<String> response = postRaw(path, objectMapper.writeValueAsString(payload), token);
        assertThat(response.statusCode()).as("response body: %s", response.body()).isEqualTo(HttpStatus.OK.value());
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.path("code").asInt()).isZero();
        return body;
    }

    private JsonNode put(String path, Object payload, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)));
        if (token != null) request.header("Authorization", "Bearer " + token);
        HttpResponse<String> response = httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
        assertThat(response.statusCode()).as("response body: %s", response.body()).isEqualTo(HttpStatus.OK.value());
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.path("code").asInt()).isZero();
        return body;
    }

    private HttpResponse<String> getRaw(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (token != null) request.header("Authorization", "Bearer " + token);
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postRaw(String path, String payload, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .header("Accept-Language", "zh-CN")
                .POST(HttpRequest.BodyPublishers.ofString(payload));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private void assertError(HttpResponse<String> response, HttpStatus status, String message) throws Exception {
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(response.statusCode()).isEqualTo(status.value());
        assertThat(body.path("code").asInt()).isEqualTo(-1);
        assertThat(body.path("message").asText()).contains(message);
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}

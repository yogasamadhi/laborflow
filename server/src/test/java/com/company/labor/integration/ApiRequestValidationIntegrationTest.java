package com.company.labor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.labor.LaborApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = LaborApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class ApiRequestValidationIntegrationTest {
    private static final String ADMIN_USERNAME = "validation_admin";
    private static final String ADMIN_PASSWORD = "validation-admin-password";
    private static final long EMPLOYEE_USER_ID = 9_100_101L;
    private static final long EMPLOYEE_ROLE_LINK_ID = 9_100_102L;
    private static final String EMPLOYEE_USERNAME = "validation_employee";
    private static final String EMPLOYEE_PASSWORD = "validation-employee-password";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("laborflow_validation_it")
            .withUsername("laborflow_validation_it")
            .withPassword("laborflow_validation_it");

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
        registry.add("laborflow.jwt.secret", () -> "validation-test-jwt-secret-with-more-than-thirty-two-bytes");
        registry.add("laborflow.bootstrap-admin.username", () -> ADMIN_USERNAME);
        registry.add("laborflow.bootstrap-admin.password", () -> ADMIN_PASSWORD);
    }

    @Test
    void returnsUnifiedBadRequestForMissingRequiredLoginFields() throws Exception {
        assertError(post("/api/auth/login", Map.of(), null), HttpStatus.BAD_REQUEST, "账号必填");
    }

    @Test
    void returnsUnifiedBadRequestForUnreadableRequestBody() throws Exception {
        assertError(postRaw("/api/auth/login", "{", null), HttpStatus.BAD_REQUEST, "请求体格式不正确");
    }

    @Test
    void returnsUnifiedBadRequestForInvalidMonthAmountAndStatus() throws Exception {
        String adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);

        assertError(post("/api/salary/monthly", Map.of("salaryMonth", "2025-13"), adminToken),
                HttpStatus.BAD_REQUEST, "工资月份格式必须为 yyyy-MM");
        assertError(post("/api/projects", Map.of("name", "校验项目", "contractAmount", -0.01), adminToken),
                HttpStatus.BAD_REQUEST, "合同金额不能小于 0");
        assertError(post("/api/projects/1/status", Map.of("status", "invalid-status"), adminToken),
                HttpStatus.BAD_REQUEST, "状态值不合法");
    }

    @Test
    void returnsUnifiedForbiddenResponseWhenRoleCannotManageSalary() throws Exception {
        insertEmployeeUser();
        String employeeToken = login(EMPLOYEE_USERNAME, EMPLOYEE_PASSWORD);

        assertError(post("/api/salary/monthly", Map.of("salaryMonth", "2030-01"), employeeToken),
                HttpStatus.FORBIDDEN, "无权操作");
    }

    private void insertEmployeeUser() {
        jdbcTemplate.update("""
                INSERT INTO sys_user (id, username, password, nickname, status, deleted)
                VALUES (?, ?, ?, '接口校验员工', 'active', 0)
                """, EMPLOYEE_USER_ID, EMPLOYEE_USERNAME, new BCryptPasswordEncoder().encode(EMPLOYEE_PASSWORD));
        jdbcTemplate.update("""
                INSERT INTO sys_user_role (id, user_id, role_id)
                SELECT ?, ?, id
                FROM sys_role
                WHERE code = 'EMPLOYEE'
                """, EMPLOYEE_ROLE_LINK_ID, EMPLOYEE_USER_ID);
    }

    private String login(String username, String password) throws Exception {
        HttpResponse<String> response = post("/api/auth/login", Map.of("username", username, "password", password), null);
        JsonNode body = objectMapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(body.path("code").asInt()).isZero();
        return body.path("data").path("token").asText();
    }

    private void assertError(HttpResponse<String> response, HttpStatus status, String message) throws Exception {
        JsonNode body = objectMapper.readTree(response.body());

        assertThat(response.statusCode()).isEqualTo(status.value());
        assertThat(body.path("code").asInt()).isEqualTo(-1);
        assertThat(body.path("message").asText()).contains(message);
        assertThat(body.path("data").isNull()).isTrue();
    }

    private HttpResponse<String> post(String path, Object body, String token) throws Exception {
        return postRaw(path, objectMapper.writeValueAsString(body), token);
    }

    private HttpResponse<String> postRaw(String path, String body, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(body));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}

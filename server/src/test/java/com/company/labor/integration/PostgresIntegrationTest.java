package com.company.labor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.company.labor.LaborApplication;
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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = LaborApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class PostgresIntegrationTest {
    private static final String ADMIN_USERNAME = "integration_admin";
    private static final String ADMIN_PASSWORD = "integration-admin-password";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("laborflow_it")
            .withUsername("laborflow_it")
            .withPassword("laborflow_it");

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
        registry.add("laborflow.jwt.secret", () -> "integration-test-jwt-secret-with-more-than-thirty-two-bytes");
        registry.add("laborflow.bootstrap-admin.username", () -> ADMIN_USERNAME);
        registry.add("laborflow.bootstrap-admin.password", () -> ADMIN_PASSWORD);
    }

    @Test
    void migratesDatabaseAndAuthenticatesBootstrapAdministrator() throws Exception {
        Integer migrationCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM flyway_schema_history WHERE success = true", Integer.class);
        Integer activeAdminCount = jdbcTemplate.queryForObject("""
                SELECT COUNT(*)
                FROM sys_user u
                JOIN sys_user_role ur ON ur.user_id = u.id
                JOIN sys_role r ON r.id = ur.role_id
                WHERE u.username = ?
                  AND u.status = 'active'
                  AND r.code = 'ADMIN'
                """, Integer.class, ADMIN_USERNAME);

        assertThat(migrationCount).isGreaterThan(0);
        assertThat(activeAdminCount).isEqualTo(1);

        HttpResponse<String> anonymousResponse = send(HttpRequest.newBuilder(uri("/api/system/users")).GET().build());
        assertThat(anonymousResponse.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());

        HttpRequest loginRequest = HttpRequest.newBuilder(uri("/api/auth/login"))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(
                        Map.of("username", ADMIN_USERNAME, "password", ADMIN_PASSWORD))))
                .build();
        HttpResponse<String> loginResponse = send(loginRequest);
        JsonNode loginBody = objectMapper.readTree(loginResponse.body());
        assertThat(loginResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(loginBody.path("code").asInt()).isZero();

        String token = loginBody.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        HttpResponse<String> usersResponse = send(HttpRequest.newBuilder(uri("/api/system/users"))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build());
        JsonNode usersBody = objectMapper.readTree(usersResponse.body());

        assertThat(usersResponse.statusCode()).isEqualTo(HttpStatus.OK.value());
        assertThat(usersBody.path("code").asInt()).isZero();
    }

    private HttpResponse<String> send(HttpRequest request) throws Exception {
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}

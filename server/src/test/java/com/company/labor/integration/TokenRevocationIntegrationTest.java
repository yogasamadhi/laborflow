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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

/**
 * Verifies the JTI-based token blacklist: after calling {@code /api/auth/logout}
 * the revoked token must be rejected on subsequent requests.
 */
@Testcontainers
@SpringBootTest(classes = LaborApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class TokenRevocationIntegrationTest {
    private static final String ADMIN_USERNAME = "token_revoke_e2e_admin";
    private static final String ADMIN_PASSWORD = "token-revoke-e2e-admin-password";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("laborflow_token_revoke_e2e_it")
            .withUsername("laborflow_token_revoke_e2e_it")
            .withPassword("laborflow_token_revoke_e2e_it");

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
        registry.add("laborflow.jwt.secret", () -> "token-revoke-e2e-jwt-secret-with-more-than-thirty-two-bytes");
        registry.add("laborflow.bootstrap-admin.username", () -> ADMIN_USERNAME);
        registry.add("laborflow.bootstrap-admin.password", () -> ADMIN_PASSWORD);
    }

    @Test
    void logoutRevokesTokenSoSubsequentRequestsAreRejected() throws Exception {
        String token = login();

        // Token works before logout.
        assertThat(getRaw("/api/auth/me", token).statusCode()).isEqualTo(HttpStatus.OK.value());

        HttpResponse<String> logoutResponse = postRaw("/api/auth/logout", "{}", token);
        assertThat(logoutResponse.statusCode()).isEqualTo(HttpStatus.OK.value());

        // A blacklist row should have been persisted.
        Integer blacklisted = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM token_blacklist WHERE deleted = 0", Integer.class);
        assertThat(blacklisted).isGreaterThanOrEqualTo(1);

        // The same token must now be rejected.
        HttpResponse<String> after = getRaw("/api/auth/me", token);
        assertThat(after.statusCode()).isEqualTo(HttpStatus.UNAUTHORIZED.value());
        assertThat(after.body()).contains("登录已失效");
    }

    @Test
    void freshTokenStillWorksAfterAnotherTokenIsRevoked() throws Exception {
        String first = login();
        postRaw("/api/auth/logout", "{}", first);

        String second = login();
        // Different jti -> unaffected by the first logout.
        assertThat(getRaw("/api/auth/me", second).statusCode()).isEqualTo(HttpStatus.OK.value());
    }

    private String login() throws Exception {
        HttpResponse<String> response = postRaw(
                "/api/auth/login",
                objectMapper.writeValueAsString(Map.of("username", ADMIN_USERNAME, "password", ADMIN_PASSWORD)),
                null);
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        JsonNode body = objectMapper.readTree(response.body());
        String token = body.path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private HttpResponse<String> getRaw(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (token != null) request.header("Authorization", "Bearer " + token);
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private HttpResponse<String> postRaw(String path, String payload, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(payload));
        if (token != null) request.header("Authorization", "Bearer " + token);
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}

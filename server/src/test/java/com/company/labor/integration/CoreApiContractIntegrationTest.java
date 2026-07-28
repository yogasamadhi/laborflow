package com.company.labor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.labor.LaborApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.InputStream;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
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
class CoreApiContractIntegrationTest {
    private static final String ADMIN_USERNAME = "contract_admin";
    private static final String ADMIN_PASSWORD = "contract-admin-password";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("laborflow_contract_it")
            .withUsername("laborflow_contract_it")
            .withPassword("laborflow_contract_it");

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
        registry.add("laborflow.jwt.secret", () -> "contract-test-jwt-secret-with-more-than-thirty-two-bytes");
        registry.add("laborflow.bootstrap-admin.username", () -> ADMIN_USERNAME);
        registry.add("laborflow.bootstrap-admin.password", () -> ADMIN_PASSWORD);
    }

    @Test
    void coreApiResponsesConformToCheckedInContract() throws Exception {
        JsonNode contract = readContract();
        assertThat(contract.path("contract").asText()).isEqualTo("laborflow-core-api");
        assertThat(contract.path("version").asInt()).isEqualTo(1);

        String token = login(contract);
        for (JsonNode write : contract.withArray("setupWrites")) {
            JsonNode body = post(write.path("path").asText(), write.path("body"), token);
            assertContractResponse(body, contract, write);
        }
        for (JsonNode read : contract.withArray("authenticatedReads")) {
            JsonNode body = get(read.path("path").asText(), token);
            assertContractResponse(body, contract, read);
        }
    }

    private String login(JsonNode contract) throws Exception {
        JsonNode login = contract.path("login");
        JsonNode body = post(login.path("path").asText(), objectMapper.valueToTree(
                java.util.Map.of("username", ADMIN_USERNAME, "password", ADMIN_PASSWORD)), null);
        assertContractResponse(body, contract, login);
        assertThat(body.path("data").path("userId").isTextual()).isTrue();
        assertThat(body.path("data").path("roles")).extracting(JsonNode::asText).contains(login.path("requiredRole").asText());
        return body.path("data").path("token").asText();
    }

    private void assertContractResponse(JsonNode body, JsonNode contract, JsonNode responseContract) {
        JsonNode envelope = contract.path("envelope");
        for (JsonNode field : envelope.withArray("fields")) {
            assertThat(body.has(field.asText())).as(responseContract.path("name").asText() + " envelope field " + field.asText()).isTrue();
        }
        assertThat(body.path("code").asInt()).isEqualTo(envelope.path("successCode").asInt());

        JsonNode data = body.path("data");
        assertFields(data, responseContract.withArray("dataFields"), responseContract.path("name").asText());
        assertNonNullFields(data, responseContract.withArray("nonNullDataFields"), responseContract.path("name").asText());
        if (responseContract.has("recordFields")) {
            assertThat(data.path("records").isArray()).isTrue();
            assertThat(data.path("records")).isNotEmpty();
            assertFields(data.path("records").get(0), responseContract.withArray("recordFields"), responseContract.path("name").asText() + " record");
        }
    }

    private void assertFields(JsonNode data, Iterable<JsonNode> fields, String subject) {
        for (JsonNode field : fields) {
            assertThat(data.has(field.asText())).as(subject + " field " + field.asText()).isTrue();
        }
    }

    private void assertNonNullFields(JsonNode data, Iterable<JsonNode> fields, String subject) {
        for (JsonNode field : fields) {
            assertThat(data.path(field.asText()).isNull()).as(subject + " non-null field " + field.asText()).isFalse();
        }
    }

    private JsonNode post(String path, JsonNode requestBody, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path))
                .header("Content-Type", "application/json")
                .POST(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(requestBody)));
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return readSuccess(httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString()));
    }

    private JsonNode get(String path, String token) throws Exception {
        HttpResponse<String> response = httpClient.send(HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + token)
                .GET()
                .build(), HttpResponse.BodyHandlers.ofString());
        return readSuccess(response);
    }

    private JsonNode readSuccess(HttpResponse<String> response) throws Exception {
        assertThat(response.statusCode()).isEqualTo(HttpStatus.OK.value());
        return objectMapper.readTree(response.body());
    }

    private JsonNode readContract() throws Exception {
        try (InputStream input = getClass().getResourceAsStream("/contracts/core-api-contract.json")) {
            assertThat(input).isNotNull();
            return objectMapper.readTree(input);
        }
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }
}

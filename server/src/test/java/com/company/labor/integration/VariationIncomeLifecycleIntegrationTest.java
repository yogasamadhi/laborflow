package com.company.labor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.labor.LaborApplication;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.io.IOException;
import java.io.UncheckedIOException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.FileSystemUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = LaborApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class VariationIncomeLifecycleIntegrationTest {
    private static final String ADMIN_USERNAME = "variation_income_e2e_admin";
    private static final String ADMIN_PASSWORD = "variation-income-e2e-admin-password";
    private static final Path FILE_STORAGE = createFileStorage();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("laborflow_variation_income_e2e_it")
            .withUsername("laborflow_variation_income_e2e_it")
            .withPassword("laborflow_variation_income_e2e_it");

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
        registry.add("laborflow.jwt.secret", () -> "variation-income-e2e-jwt-secret-with-more-than-thirty-two-bytes");
        registry.add("laborflow.bootstrap-admin.username", () -> ADMIN_USERNAME);
        registry.add("laborflow.bootstrap-admin.password", () -> ADMIN_PASSWORD);
        registry.add("laborflow.file.storage.local.base-dir", () -> FILE_STORAGE.toString());
    }

    @AfterAll
    static void removeFileStorage() throws IOException {
        FileSystemUtils.deleteRecursively(FILE_STORAGE);
    }

    @Test
    void completesVariationAttachmentConfirmationAndIncomeDashboardLifecycle() throws Exception {
        String token = login();
        long projectId = id(post("/api/projects", Map.of(
                "name", "Variation Income E2E",
                "contractAmount", new BigDecimal("50000.00"),
                "status", "in_progress"), token));
        long sectionId = id(post("/api/project-sections", Map.of(
                "projectId", projectId,
                "name", "Variation E2E Section",
                "length", new BigDecimal("100.00")), token));

        JsonNode variation = post("/api/variation-orders", Map.<String, Object>ofEntries(
                Map.entry("projectId", projectId),
                Map.entry("projectSectionId", sectionId),
                Map.entry("occurrenceDate", "2031-10-11"),
                Map.entry("variationType", "现场签证"),
                Map.entry("reason", "E2E variation evidence"),
                Map.entry("responsibleParty", "client"),
                Map.entry("increasedQuantity", new BigDecimal("5.00")),
                Map.entry("estimatedAmount", new BigDecimal("1500.00")),
                Map.entry("laborCount", 3),
                Map.entry("machineryShift", "0.50"),
                Map.entry("materialNote", "E2E evidence material")), token).path("data");
        long variationId = variation.path("id").asLong();
        assertThat(variationId).isPositive();
        assertThat(variation.path("status").asText()).isEqualTo("draft");
        assertThat(variation.path("confirmedAmount").asDouble()).isZero();

        JsonNode uploadedFile = uploadEvidenceFile(token).path("data");
        long fileId = uploadedFile.path("id").asLong();
        assertThat(fileId).isPositive();
        assertThat(uploadedFile.path("originalName").asText()).isEqualTo("variation-evidence.txt");
        assertThat(get("/api/files/" + fileId, token).path("data").path("filePath").asText())
                .isEqualTo("/api/files/" + fileId + "/download");

        post("/api/variation-orders/" + variationId + "/files", Map.of(
                "fileId", fileId,
                "fileType", "photo"), token);
        JsonNode files = get("/api/variation-orders/" + variationId + "/files", token).path("data");
        assertThat(files).hasSize(1);
        assertThat(files.get(0).path("fileId").asLong()).isEqualTo(fileId);
        assertThat(files.get(0).path("fileType").asText()).isEqualTo("photo");

        post("/api/variation-orders/" + variationId + "/submit", null, token);
        JsonNode pendingDashboard = get("/api/project-dashboard/" + projectId, token).path("data");
        assertThat(pendingDashboard.path("pendingVariationAmount").asDouble()).isEqualTo(1500.0);
        assertThat(pendingDashboard.path("confirmedVariationAmount").asDouble()).isZero();
        assertThat(pendingDashboard.path("revenueAmount").asDouble()).isZero();

        post("/api/variation-orders/" + variationId + "/confirm", Map.of(
                "confirmedAmount", new BigDecimal("1250.00"),
                "remark", "E2E commercial confirmation"), token);
        JsonNode confirms = get("/api/variation-orders/" + variationId + "/confirms", token).path("data");
        assertThat(StreamSupport.stream(confirms.spliterator(), false)
                .map(confirm -> confirm.path("confirmStage").asText()))
                .containsExactly("commercial", "submit", "attachment");
        JsonNode confirmedVariation = get("/api/variation-orders?projectId=" + projectId, token)
                .path("data").path("records").get(0);
        assertThat(confirmedVariation.path("status").asText()).isEqualTo("confirmed");
        assertThat(confirmedVariation.path("confirmedAmount").asDouble()).isEqualTo(1250.0);
        assertThat(confirmedVariation.path("confirmRemark").asText()).isEqualTo("E2E commercial confirmation");

        JsonNode dashboard = get("/api/project-dashboard/" + projectId, token).path("data");
        assertThat(dashboard.path("pendingVariationAmount").asDouble()).isZero();
        assertThat(dashboard.path("confirmedVariationAmount").asDouble()).isEqualTo(1250.0);
        assertThat(dashboard.path("revenueAmount").asDouble()).isEqualTo(1250.0);
        assertThat(dashboard.path("receivableAmount").asDouble()).isEqualTo(51250.0);
        assertThat(dashboard.path("revenueBreakdowns").get(1).path("category").asText()).isEqualTo("签证变更收入");
        assertThat(dashboard.path("revenueBreakdowns").get(1).path("amount").asDouble()).isEqualTo(1250.0);
    }

    private JsonNode uploadEvidenceFile(String token) throws Exception {
        String boundary = "----LaborflowVariationE2E";
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"variation-evidence.txt\"\r\n"
                + "Content-Type: text/plain\r\n\r\n"
                + "variation evidence\r\n"
                + "--" + boundary + "--\r\n";
        HttpRequest request = HttpRequest.newBuilder(uri("/api/files/upload"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.getBytes(StandardCharsets.UTF_8)))
                .build();
        return readSuccess(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    private long id(JsonNode response) {
        long id = response.path("data").path("id").asLong();
        assertThat(id).isPositive();
        return id;
    }

    private String login() throws Exception {
        JsonNode body = post("/api/auth/login", Map.of("username", ADMIN_USERNAME, "password", ADMIN_PASSWORD), null);
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
        assertThat(response.statusCode())
                .as("response body: %s", response.body())
                .isEqualTo(HttpStatus.OK.value());
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.path("code").asInt()).isZero();
        return body;
    }

    private URI uri(String path) {
        return URI.create("http://localhost:" + port + path);
    }

    private static Path createFileStorage() {
        try {
            return Files.createTempDirectory("laborflow-variation-e2e-");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}

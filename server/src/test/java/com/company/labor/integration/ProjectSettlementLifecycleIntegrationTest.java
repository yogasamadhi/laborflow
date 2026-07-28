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
class ProjectSettlementLifecycleIntegrationTest {
    private static final String ADMIN_USERNAME = "project_settlement_e2e_admin";
    private static final String ADMIN_PASSWORD = "project-settlement-e2e-admin-password";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("laborflow_project_settlement_e2e_it")
            .withUsername("laborflow_project_settlement_e2e_it")
            .withPassword("laborflow_project_settlement_e2e_it");

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
        registry.add("laborflow.jwt.secret", () -> "project-settlement-e2e-jwt-secret-with-more-than-thirty-two-bytes");
        registry.add("laborflow.bootstrap-admin.username", () -> ADMIN_USERNAME);
        registry.add("laborflow.bootstrap-admin.password", () -> ADMIN_PASSWORD);
    }

    @Test
    void completesDailyReportToTeamSettlementAndPaymentLifecycle() throws Exception {
        String token = login();
        long projectId = id(post("/api/projects", Map.of(
                "name", "Project Settlement E2E",
                "contractAmount", new BigDecimal("50000.00"),
                "status", "in_progress"), token));
        long sectionId = id(post("/api/project-sections", Map.of(
                "projectId", projectId,
                "name", "E2E Section",
                "length", new BigDecimal("100.00")), token));
        long processId = id(post("/api/work-processes", Map.of(
                "name", "E2E Installation",
                "unit", "m",
                "sortOrder", 999,
                "status", "active"), token));
        long teamId = id(post("/api/teams", Map.of(
                "name", "E2E Construction Team",
                "settlementMethod", "quantity",
                "status", "active"), token));

        post("/api/team-unit-prices", Map.of(
                "projectId", projectId,
                "teamId", teamId,
                "workProcessId", processId,
                "unit", "m",
                "unitPrice", new BigDecimal("120.00"),
                "effectiveDate", "2031-09-01",
                "status", "active"), token);
        post("/api/team-unit-prices", Map.of(
                "projectId", projectId,
                "teamId", teamId,
                "workProcessId", processId,
                "unit", "m",
                "unitPrice", new BigDecimal("999.00"),
                "effectiveDate", "2031-10-01",
                "status", "active"), token);
        long contractItemId = id(post("/api/project-contract-items", Map.of(
                "projectId", projectId,
                "projectSectionId", sectionId,
                "workProcessId", processId,
                "itemCode", "E2E-001",
                "itemName", "E2E installation item",
                "unit", "m",
                "contractQuantity", new BigDecimal("100.00"),
                "unitPrice", new BigDecimal("200.00"),
                "status", "active"), token));

        Map<String, Object> pricingRequest = Map.of(
                "workDate", "2031-09-10",
                "projectId", projectId,
                "projectSectionId", sectionId,
                "workProcessId", processId,
                "completedQuantity", new BigDecimal("10.00"),
                "unit", "m");
        JsonNode singleCandidatePricing = post("/api/daily-work-reports/pricing-preview", pricingRequest, token).path("data");
        assertThat(singleCandidatePricing.path("priced").asBoolean()).isTrue();
        assertThat(singleCandidatePricing.path("selectionRequired").asBoolean()).isFalse();
        assertThat(singleCandidatePricing.path("selectedContractItemId").asLong()).isEqualTo(contractItemId);
        assertThat(singleCandidatePricing.path("outputValue").asDouble()).isEqualTo(2000.0);

        post("/api/project-contract-items", Map.of(
                "projectId", projectId,
                "projectSectionId", sectionId,
                "workProcessId", processId,
                "itemCode", "E2E-002",
                "itemName", "E2E alternative installation item",
                "unit", "m",
                "contractQuantity", new BigDecimal("100.00"),
                "unitPrice", new BigDecimal("180.00"),
                "status", "active"), token);
        JsonNode ambiguousPricing = post("/api/daily-work-reports/pricing-preview", pricingRequest, token).path("data");
        assertThat(ambiguousPricing.path("priced").asBoolean()).isFalse();
        assertThat(ambiguousPricing.path("selectionRequired").asBoolean()).isTrue();
        assertThat(ambiguousPricing.path("candidates")).hasSize(2);

        JsonNode report = post("/api/daily-work-reports", Map.of(
                "workDate", "2031-09-10",
                "projectId", projectId,
                "projectSectionId", sectionId,
                "contractItemId", contractItemId,
                "workProcessId", processId,
                "completedQuantity", new BigDecimal("10.00"),
                "unit", "m",
                "teamId", teamId,
                "workerCount", 4,
                "reporterName", "E2E Reporter"), token).path("data");
        long reportId = report.path("id").asLong();
        assertThat(report.path("status").asText()).isEqualTo("draft");
        assertThat(report.path("outputValue").asDouble()).isEqualTo(2000.0);
        assertThat(report.path("pricingBasis").asText()).contains("合同清单ID:" + contractItemId);

        long fallbackProcessId = id(post("/api/work-processes", Map.of(
                "name", "E2E Backfill",
                "unit", "m",
                "sortOrder", 1000,
                "status", "active"), token));
        post("/api/project-process-prices", Map.of(
                "projectId", projectId,
                "workProcessId", fallbackProcessId,
                "unit", "m",
                "unitPrice", new BigDecimal("10.00"),
                "effectiveDate", "2031-09-01",
                "status", "active"), token);
        post("/api/project-process-prices", Map.of(
                "projectId", projectId,
                "workProcessId", fallbackProcessId,
                "unit", "m",
                "unitPrice", new BigDecimal("99.00"),
                "effectiveDate", "2031-10-01",
                "status", "active"), token);
        JsonNode fallbackReport = post("/api/daily-work-reports", Map.of(
                "workDate", "2031-09-10",
                "projectId", projectId,
                "projectSectionId", sectionId,
                "workProcessId", fallbackProcessId,
                "completedQuantity", new BigDecimal("1.00"),
                "unit", "m",
                "workerCount", 1,
                "reporterName", "E2E Reporter"), token).path("data");
        assertThat(fallbackReport.path("outputValue").asDouble()).isEqualTo(10.0);

        long unpricedProcessId = id(post("/api/work-processes", Map.of(
                "name", "E2E Unpriced Work",
                "unit", "m",
                "sortOrder", 1001,
                "status", "active"), token));
        JsonNode unpricedReport = post("/api/daily-work-reports", Map.of(
                "workDate", "2031-09-10",
                "projectId", projectId,
                "projectSectionId", sectionId,
                "workProcessId", unpricedProcessId,
                "completedQuantity", new BigDecimal("2.00"),
                "unit", "m",
                "workerCount", 1,
                "reporterName", "E2E Reporter"), token).path("data");
        assertThat(unpricedReport.path("status").asText()).isEqualTo("draft");
        assertThat(unpricedReport.path("pricingStatus").asText()).isEqualTo("unpriced");
        assertThat(unpricedReport.path("outputValue").asDouble()).isZero();
        HttpResponse<String> unpricedSubmit = postRaw(
                "/api/daily-work-reports/" + unpricedReport.path("id").asLong() + "/submit", null, token);
        assertThat(unpricedSubmit.statusCode()).isEqualTo(HttpStatus.BAD_REQUEST.value());
        assertThat(objectMapper.readTree(unpricedSubmit.body()).path("message").asText()).contains("日报未定价");

        post("/api/daily-work-reports/" + reportId + "/submit", null, token);
        post("/api/daily-work-reports/" + reportId + "/pm-confirm", Map.of("remark", "E2E PM confirmation"), token);
        post("/api/daily-work-reports/" + reportId + "/final-confirm", Map.of("remark", "E2E final confirmation"), token);
        JsonNode confirms = get("/api/daily-work-reports/" + reportId + "/confirms", token).path("data");
        assertThat(StreamSupport.stream(confirms.spliterator(), false)
                .map(confirm -> confirm.path("confirmStage").asText()))
                .containsExactly("commercial", "project_manager", "submit");

        JsonNode settlement = post("/api/team-settlements/generate", Map.of(
                "projectId", projectId,
                "teamId", teamId,
                "settlementMonth", "2031-09",
                "compensationAmount", new BigDecimal("100.00"),
                "deductionAmount", new BigDecimal("50.00"),
                "remark", "E2E settlement"), token).path("data");
        long settlementId = settlement.path("id").asLong();
        assertThat(settlement.path("settlementStatus").asText()).isEqualTo("draft");
        assertThat(settlement.path("confirmedQuantity").asDouble()).isEqualTo(10.0);
        assertThat(settlement.path("settlementAmount").asDouble()).isEqualTo(1200.0);
        assertThat(settlement.path("actualPayAmount").asDouble()).isEqualTo(1250.0);

        post("/api/team-settlements/" + settlementId + "/confirm", Map.of("remark", "E2E settlement confirmation"), token);
        post("/api/team-settlements/" + settlementId + "/mark-paid", Map.of("remark", "E2E bank transfer"), token);
        JsonNode paidSettlement = get("/api/team-settlements?projectId=" + projectId + "&teamId=" + teamId, token)
                .path("data").path("records").get(0);
        assertThat(paidSettlement.path("settlementStatus").asText()).isEqualTo("confirmed");
        assertThat(paidSettlement.path("paymentStatus").asText()).isEqualTo("paid");
        assertThat(paidSettlement.path("paymentRemark").asText()).isEqualTo("E2E bank transfer");

        JsonNode dashboard = get("/api/project-dashboard/" + projectId, token).path("data");
        assertThat(dashboard.path("confirmedQuantity").asDouble()).isEqualTo(10.0);
        assertThat(dashboard.path("completedOutputValue").asDouble()).isEqualTo(2000.0);
        assertThat(dashboard.path("teamPayableAmount").asDouble()).isEqualTo(1250.0);
        assertThat(dashboard.path("teamPaidAmount").asDouble()).isEqualTo(1250.0);
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
        return readSuccess(postRaw(path, payload, token));
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

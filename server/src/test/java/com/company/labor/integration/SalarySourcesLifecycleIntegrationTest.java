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
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = LaborApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class SalarySourcesLifecycleIntegrationTest {
    private static final String ADMIN_USERNAME = "salary_sources_e2e_admin";
    private static final String ADMIN_PASSWORD = "salary-sources-e2e-admin-password";
    private static final String SALARY_MONTH = "2031-11";

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("laborflow_salary_sources_e2e_it")
            .withUsername("laborflow_salary_sources_e2e_it")
            .withPassword("laborflow_salary_sources_e2e_it");

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
        registry.add("laborflow.jwt.secret", () -> "salary-sources-e2e-jwt-secret-with-more-than-thirty-two-bytes");
        registry.add("laborflow.bootstrap-admin.username", () -> ADMIN_USERNAME);
        registry.add("laborflow.bootstrap-admin.password", () -> ADMIN_PASSWORD);
    }

    @Test
    void generatesSalaryFromAttendanceDeductionsPerformanceAndProjectBonus() throws Exception {
        String token = login();
        long departmentId = id(post("/api/departments", Map.of(
                "name", "Salary Sources E2E Department",
                "status", "active"), token));
        long employeeId = id(post("/api/employees", Map.of(
                "name", "Salary Sources E2E Employee",
                "departmentId", departmentId,
                "employeeType", "company_employee",
                "entryDate", "2031-11-01",
                "status", "active"), token));
        long projectId = id(post("/api/projects", Map.of(
                "name", "Salary Sources E2E Project",
                "contractAmount", new BigDecimal("10000.00"),
                "status", "in_progress"), token));

        post("/api/salary/profiles", Map.of(
                "employeeId", employeeId,
                "effectiveDate", "2031-11-01",
                "status", "active",
                "items", List.of(
                        Map.of(
                                "salaryItemName", "Fixed salary excluded by rule",
                                "salaryItemType", "earning",
                                "amount", new BigDecimal("1000.00")),
                        Map.of(
                                "salaryItemId", 2003,
                                "salaryItemName", "绩效工资",
                                "salaryItemType", "earning",
                                "amount", new BigDecimal("1000.00")))), token);

        JsonNode attendance = post("/api/compensation/attendance", Map.of(
                "employeeId", employeeId,
                "attendanceMonth", SALARY_MONTH,
                "projectId", projectId,
                "workDays", new BigDecimal("21.50"),
                "leaveDays", new BigDecimal("0.50"),
                "absentDays", BigDecimal.ZERO), token).path("data");
        long attendanceId = attendance.path("id").asLong();
        assertThat(attendance.path("deductionAmount").asDouble()).isEqualTo(50.0);

        JsonNode statutoryDeduction = post("/api/compensation/deductions", Map.of(
                "employeeId", employeeId,
                "deductionMonth", SALARY_MONTH,
                "deductionType", "social_security",
                "amount", new BigDecimal("120.00")), token).path("data");
        long statutoryDeductionId = statutoryDeduction.path("id").asLong();
        assertThat(statutoryDeduction.path("salaryItemName").asText()).isEqualTo("社保扣款");
        assertThat(statutoryDeduction.path("status").asText()).isEqualTo("active");

        long indicatorId = id(post("/api/compensation/indicators", Map.of(
                "name", "Salary Sources E2E manual score",
                "category", "manual",
                "scoreWeight", new BigDecimal("100.00"),
                "dataSource", "manual",
                "scoringDirection", "higher_better",
                "targetValue", new BigDecimal("100.00"),
                "status", "active"), token));
        long planId = post("/api/compensation/plans", Map.of(
                "name", "Salary Sources E2E plan",
                "performanceMonth", SALARY_MONTH,
                "projectId", projectId,
                "indicatorIds", List.of(indicatorId)), token)
                .path("data").path("plan").path("id").asLong();
        assertThat(planId).isPositive();
        post("/api/compensation/plans/" + planId + "/activate", null, token);
        JsonNode generatedScores = post("/api/compensation/plans/" + planId + "/scores/generate",
                Map.of("employeeIds", List.of(employeeId)), token).path("data");
        long performanceScoreId = generatedScores.get(0).path("score").path("id").asLong();
        long planIndicatorId = generatedScores.get(0).path("items").get(0).path("planIndicatorId").asLong();
        put("/api/compensation/scores/" + performanceScoreId, Map.of(
                "items", List.of(Map.of(
                        "planIndicatorId", planIndicatorId,
                        "finalScore", new BigDecimal("20.00"))),
                "remark", "E2E confirmed performance"), token);
        post("/api/compensation/scores/" + performanceScoreId + "/confirm", null, token);
        assertThat(get("/api/compensation/scores?month=" + SALARY_MONTH + "&employeeId=" + employeeId, token)
                .path("data").path("records").get(0).path("status").asText()).isEqualTo("confirmed");

        long bonusPoolId = id(post("/api/compensation/bonus-pools", Map.of(
                "projectId", projectId,
                "bonusMonth", SALARY_MONTH,
                "finalGrossProfit", new BigDecimal("10000.00"),
                "bonusRatio", new BigDecimal("0.10"),
                "releaseCondition", "E2E source regression"), token));
        post("/api/compensation/bonus-pools/" + bonusPoolId + "/release", null, token);
        long bonusAllocationId = id(post("/api/compensation/bonus-allocations", Map.of(
                "bonusPoolId", bonusPoolId,
                "employeeId", employeeId,
                "roleName", "E2E Project Contributor",
                "allocationAmount", new BigDecimal("300.00"),
                "paymentMonth", SALARY_MONTH), token));
        post("/api/compensation/bonus-allocations/" + bonusAllocationId + "/confirm", null, token);
        assertThat(get("/api/compensation/bonus-allocations?poolId=" + bonusPoolId, token)
                .path("data").path("records").get(0).path("status").asText()).isEqualTo("confirmed");

        JsonNode monthly = post("/api/salary/monthly", Map.of(
                "salaryMonth", SALARY_MONTH,
                "departmentId", departmentId,
                "title", "Salary Sources E2E",
                "includeFixedSalary", 0,
                "includeAttendanceDeduction", 1,
                "includePerformanceSalary", 1,
                "includeProjectBonus", 1), token).path("data");
        long monthlyId = monthly.path("id").asLong();
        post("/api/salary/monthly/" + monthlyId + "/generate", null, token);

        JsonNode detail = get("/api/salary/monthly/" + monthlyId + "/details?page=1&pageSize=20", token)
                .path("data").path("records").get(0);
        long detailId = detail.path("id").asLong();
        assertThat(detail.path("employeeId").asLong()).isEqualTo(employeeId);
        assertThat(detail.path("payableAmount").asDouble()).isEqualTo(500.0);
        assertThat(detail.path("deductionAmount").asDouble()).isEqualTo(170.0);
        assertThat(detail.path("actualAmount").asDouble()).isEqualTo(330.0);

        List<String> itemNames = StreamSupport.stream(get("/api/salary/details/" + detailId + "/items", token)
                        .path("data").spliterator(), false)
                .map(item -> item.path("salaryItemName").asText())
                .toList();
        assertThat(itemNames).containsExactly("考勤扣款", "社保扣款", "绩效奖金", "项目奖金");

        Map<String, JsonNode> sourcesByType = StreamSupport.stream(get("/api/salary/monthly/" + monthlyId + "/sources?detailId=" + detailId, token)
                        .path("data").spliterator(), false)
                .collect(Collectors.toMap(source -> source.path("sourceType").asText(), source -> source));
        assertThat(sourcesByType).containsOnlyKeys(
                "attendance_deduction", "salary_deduction", "performance_salary", "project_bonus");
        assertThat(sourcesByType.get("attendance_deduction").path("sourceId").asLong()).isEqualTo(attendanceId);
        assertThat(sourcesByType.get("attendance_deduction").path("amount").asDouble()).isEqualTo(-50.0);
        assertThat(sourcesByType.get("salary_deduction").path("sourceId").asLong()).isEqualTo(statutoryDeductionId);
        assertThat(sourcesByType.get("salary_deduction").path("amount").asDouble()).isEqualTo(-120.0);
        assertThat(sourcesByType.get("performance_salary").path("sourceId").asLong()).isEqualTo(performanceScoreId);
        assertThat(sourcesByType.get("performance_salary").path("amount").asDouble()).isEqualTo(200.0);
        assertThat(sourcesByType.get("performance_salary").path("remark").asText())
                .contains("绩效方案ID:" + planId,
                        "绩效成绩ID:" + performanceScoreId,
                        "绩效基数快照:1000.00",
                        "加权总分:20.00",
                        "绩效金额:200.00",
                        "确认时间:");
        assertThat(sourcesByType.get("project_bonus").path("sourceId").asLong()).isEqualTo(bonusAllocationId);
        assertThat(sourcesByType.get("project_bonus").path("amount").asDouble()).isEqualTo(300.0);

        JsonNode dashboard = get("/api/compensation/dashboard?month=" + SALARY_MONTH, token).path("data");
        assertThat(dashboard.path("attendanceDeductionAmount").asDouble()).isEqualTo(50.0);
        assertThat(dashboard.path("salaryDeductionAmount").asDouble()).isEqualTo(120.0);
        assertThat(dashboard.path("performanceAmount").asDouble()).isEqualTo(200.0);
        assertThat(dashboard.path("projectBonusAmount").asDouble()).isEqualTo(300.0);
        assertThat(dashboard.path("salaryActualAmount").asDouble()).isEqualTo(330.0);
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

    private JsonNode put(String path, Object payload, String token) throws Exception {
        HttpRequest request = HttpRequest.newBuilder(uri(path))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "application/json")
                .PUT(HttpRequest.BodyPublishers.ofString(objectMapper.writeValueAsString(payload)))
                .build();
        return readSuccess(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
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
}

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
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.springframework.util.FileSystemUtils;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
@SpringBootTest(classes = LaborApplication.class, webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT)
class FileAccessBoundaryIntegrationTest {
    private static final String ADMIN_USERNAME = "file_access_e2e_admin";
    private static final String ADMIN_PASSWORD = "file-access-e2e-admin-password";
    private static final Path FILE_STORAGE = createFileStorage();

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("laborflow_file_access_e2e_it")
            .withUsername("laborflow_file_access_e2e_it")
            .withPassword("laborflow_file_access_e2e_it");

    @Autowired
    private ObjectMapper objectMapper;

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @LocalServerPort
    private int port;

    private final HttpClient httpClient = HttpClient.newHttpClient();

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
        registry.add("laborflow.jwt.secret", () -> "file-access-e2e-jwt-secret-with-more-than-thirty-two-bytes");
        registry.add("laborflow.bootstrap-admin.username", () -> ADMIN_USERNAME);
        registry.add("laborflow.bootstrap-admin.password", () -> ADMIN_PASSWORD);
        registry.add("laborflow.file.storage.local.base-dir", () -> FILE_STORAGE.toString());
    }

    @AfterAll
    static void removeFileStorage() throws IOException {
        FileSystemUtils.deleteRecursively(FILE_STORAGE);
    }

    @Test
    void protectsPrivateDownloadsAndRejectsCrossBusinessAttachmentReuse() throws Exception {
        String adminToken = login(ADMIN_USERNAME, ADMIN_PASSWORD);
        Map<String, Long> roleIds = roleIds(adminToken);
        long projectA = createProject("File Access E2E Project A", adminToken);
        long projectB = createProject("File Access E2E Project B", adminToken);
        long sectionA = id(post("/api/project-sections", Map.of(
                "projectId", projectA,
                "name", "File Access E2E Section A",
                "length", new BigDecimal("100.00")), adminToken));
        long workProcessId = id(post("/api/work-processes", Map.of(
                "name", "File Access E2E Process",
                "unit", "m",
                "status", "active"), adminToken));

        long uploaderEmployeeId = createEmployee("File Access E2E Uploader", adminToken);
        long projectViewerEmployeeId = createEmployee("File Access E2E Project Viewer", adminToken);
        long outsideEmployeeId = createEmployee("File Access E2E Outside Viewer", adminToken);
        long uploaderUserId = createUser("file_access_e2e_uploader", "file-access-e2e-uploader-password",
                uploaderEmployeeId, roleIds.get("EMPLOYEE"), adminToken);
        long projectViewerUserId = createUser("file_access_e2e_project_viewer", "file-access-e2e-project-viewer-password",
                projectViewerEmployeeId, roleIds.get("EMPLOYEE"), adminToken);
        long outsideUserId = createUser("file_access_e2e_outside_viewer", "file-access-e2e-outside-viewer-password",
                outsideEmployeeId, roleIds.get("EMPLOYEE"), adminToken);
        addProjectMember(9_400_001L, projectA, uploaderUserId, uploaderEmployeeId);
        addProjectMember(9_400_002L, projectA, projectViewerUserId, projectViewerEmployeeId);
        addProjectMember(9_400_003L, projectB, outsideUserId, outsideEmployeeId);

        String uploaderToken = login("file_access_e2e_uploader", "file-access-e2e-uploader-password");
        long dailyReportId = id(post("/api/daily-work-reports", Map.of(
                "workDate", "2032-01-12",
                "projectId", projectA,
                "projectSectionId", sectionA,
                "workProcessId", workProcessId,
                "completedQuantity", new BigDecimal("1.00"),
                "unit", "m"), uploaderToken));
        long dailyFileId = id(uploadTextFile("daily-evidence.txt", "daily private evidence", uploaderToken));
        post("/api/daily-work-reports/" + dailyReportId + "/files", Map.of(
                "fileId", dailyFileId,
                "fileType", "photo"), uploaderToken);

        assertDownload(dailyFileId, uploaderToken, "daily private evidence");
        String projectViewerToken = login("file_access_e2e_project_viewer", "file-access-e2e-project-viewer-password");
        assertThat(get("/api/files/" + dailyFileId, projectViewerToken).path("data").path("id").asLong())
                .isEqualTo(dailyFileId);
        assertDownload(dailyFileId, projectViewerToken, "daily private evidence");

        String outsideToken = login("file_access_e2e_outside_viewer", "file-access-e2e-outside-viewer-password");
        assertError(getRaw("/api/files/" + dailyFileId, outsideToken), HttpStatus.BAD_REQUEST, "无权下载该文件");
        assertError(getRaw("/api/files/" + dailyFileId + "/download", outsideToken), HttpStatus.BAD_REQUEST, "无权下载该文件");
        assertDownload(dailyFileId, adminToken, "daily private evidence");

        long variationId = id(post("/api/variation-orders", Map.of(
                "projectId", projectB,
                "occurrenceDate", "2032-01-13",
                "variationType", "现场签证",
                "estimatedAmount", new BigDecimal("100.00")), adminToken));
        assertError(postRaw("/api/variation-orders/" + variationId + "/files", Map.of(
                "fileId", dailyFileId,
                "fileType", "evidence"), adminToken), HttpStatus.BAD_REQUEST, "附件已被其他业务使用");

        long firstSalaryEmployeeId = createEmployee("File Access E2E First Salary Employee", adminToken);
        long secondSalaryEmployeeId = createEmployee("File Access E2E Second Salary Employee", adminToken);
        createProfile(firstSalaryEmployeeId, adminToken);
        createProfile(secondSalaryEmployeeId, adminToken);
        long monthlyId = id(post("/api/salary/monthly", Map.of(
                "salaryMonth", "2032-01",
                "title", "File Access E2E Salary",
                "includeFixedSalary", 1,
                "includeAttendanceDeduction", 0,
                "includePerformanceSalary", 0,
                "includeProjectBonus", 0), adminToken));
        post("/api/salary/monthly/" + monthlyId + "/generate", null, adminToken);
        post("/api/salary/monthly/" + monthlyId + "/submit", null, adminToken);
        post("/api/salary/monthly/" + monthlyId + "/approve", Map.of("remark", "file access e2e approval"), adminToken);
        JsonNode salaryDetails = get("/api/salary/monthly/" + monthlyId + "/details?page=1&pageSize=20", adminToken)
                .path("data").path("records");
        assertThat(salaryDetails).hasSize(2);

        long salaryProofFileId = id(uploadTextFile("salary-proof.txt", "salary private proof", adminToken));
        post("/api/salary/details/batch-mark-paid", Map.of(
                "monthlyId", monthlyId,
                "paymentDate", "2032-01-31",
                "paymentMethod", "bank_transfer",
                "paymentProofUrl", "/api/files/" + salaryProofFileId + "/download"), adminToken);
        JsonNode paidSalaryDetails = get("/api/salary/monthly/" + monthlyId + "/details?page=1&pageSize=20", adminToken)
                .path("data").path("records");
        assertThat(StreamSupport.stream(paidSalaryDetails.spliterator(), false)
                .allMatch(detail -> detail.path("paymentStatus").asText().equals("paid")
                        && detail.path("paymentProofUrl").asText().equals("/api/files/" + salaryProofFileId + "/download")))
                .isTrue();
        assertError(postRaw("/api/project-document-archives", Map.of(
                "projectId", projectA,
                "documentType", "工资证明误用",
                "fileId", salaryProofFileId), adminToken), HttpStatus.BAD_REQUEST, "附件已被其他业务使用");
    }

    private long createProject(String name, String token) throws Exception {
        return id(post("/api/projects", Map.of(
                "name", name,
                "contractAmount", new BigDecimal("10000.00"),
                "status", "in_progress"), token));
    }

    private long createEmployee(String name, String token) throws Exception {
        return id(post("/api/employees", Map.of(
                "name", name,
                "employeeType", "company_employee",
                "entryDate", "2032-01-01",
                "status", "active"), token));
    }

    private void createProfile(long employeeId, String token) throws Exception {
        post("/api/salary/profiles", Map.of(
                "employeeId", employeeId,
                "effectiveDate", "2032-01-01",
                "status", "active",
                "items", List.of(Map.of(
                        "salaryItemName", "File Access E2E fixed salary",
                        "salaryItemType", "earning",
                        "amount", new BigDecimal("1000.00")))), token);
    }

    private Map<String, Long> roleIds(String token) throws Exception {
        return StreamSupport.stream(get("/api/system/roles?page=1&pageSize=20", token).path("data").path("records").spliterator(), false)
                .collect(Collectors.toMap(role -> role.path("code").asText(), role -> role.path("id").asLong()));
    }

    private long createUser(String username, String password, long employeeId, long roleId, String token) throws Exception {
        return id(post("/api/system/users", Map.of(
                "username", username,
                "password", password,
                "nickname", username,
                "status", "active",
                "employeeId", employeeId,
                "roleIds", List.of(roleId)), token));
    }

    private void addProjectMember(long memberId, long projectId, long userId, long employeeId) {
        jdbcTemplate.update("""
                INSERT INTO project_member (id, project_id, user_id, employee_id, role_name, status, deleted)
                VALUES (?, ?, ?, ?, '现场成员', 'active', 0)
                """, memberId, projectId, userId, employeeId);
    }

    private JsonNode uploadTextFile(String filename, String content, String token) throws Exception {
        String boundary = "----LaborflowFileAccessE2E";
        String body = "--" + boundary + "\r\n"
                + "Content-Disposition: form-data; name=\"file\"; filename=\"" + filename + "\"\r\n"
                + "Content-Type: text/plain\r\n\r\n"
                + content + "\r\n"
                + "--" + boundary + "--\r\n";
        HttpRequest request = HttpRequest.newBuilder(uri("/api/files/upload"))
                .header("Authorization", "Bearer " + token)
                .header("Content-Type", "multipart/form-data; boundary=" + boundary)
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.getBytes(StandardCharsets.UTF_8)))
                .build();
        return readSuccess(httpClient.send(request, HttpResponse.BodyHandlers.ofString()));
    }

    private String login(String username, String password) throws Exception {
        String token = post("/api/auth/login", Map.of("username", username, "password", password), null)
                .path("data").path("token").asText();
        assertThat(token).isNotBlank();
        return token;
    }

    private long id(JsonNode response) {
        long id = response.path("data").path("id").asLong();
        assertThat(id).isPositive();
        return id;
    }

    private JsonNode get(String path, String token) throws Exception {
        return readSuccess(getRaw(path, token));
    }

    private JsonNode post(String path, Object payload, String token) throws Exception {
        return readSuccess(postRaw(path, payload, token));
    }

    private HttpResponse<String> getRaw(String path, String token) throws Exception {
        HttpRequest.Builder request = HttpRequest.newBuilder(uri(path)).GET();
        if (token != null) {
            request.header("Authorization", "Bearer " + token);
        }
        return httpClient.send(request.build(), HttpResponse.BodyHandlers.ofString());
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
        assertThat(response.statusCode()).as("response body: %s", response.body()).isEqualTo(HttpStatus.OK.value());
        JsonNode body = objectMapper.readTree(response.body());
        assertThat(body.path("code").asInt()).isZero();
        return body;
    }

    private void assertDownload(long fileId, String token, String expectedContent) throws Exception {
        HttpResponse<String> response = getRaw("/api/files/" + fileId + "/download", token);
        assertThat(response.statusCode()).as("response body: %s", response.body()).isEqualTo(HttpStatus.OK.value());
        assertThat(response.body()).isEqualTo(expectedContent);
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

    private static Path createFileStorage() {
        try {
            return Files.createTempDirectory("laborflow-file-access-e2e-");
        } catch (IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}

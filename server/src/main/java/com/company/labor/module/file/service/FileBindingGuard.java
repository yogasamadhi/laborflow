package com.company.labor.module.file.service;

import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.file.domain.entity.FileRecordDO;
import com.company.labor.module.file.mapper.FileRecordMapper;
import com.company.labor.module.project.service.ProjectAccessService;
import com.company.labor.security.AccessControlSupport;
import com.company.labor.security.AuditUserProvider;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class FileBindingGuard {
    private static final Pattern PRIVATE_DOWNLOAD_URL =
            Pattern.compile("^/(?:api/)?files/(\\d+)/download$");

    public static final String DAILY_WORK_REPORT = "daily_work_report";
    public static final String VARIATION_ORDER = "variation_order";
    public static final String PROJECT_PAYMENT = "project_payment";
    public static final String PROJECT_DOCUMENT = "project_document";
    public static final String SALARY_PAYMENT = "salary_payment";

    private static final String LOCK_FILE_SQL = "SELECT id FROM file_record WHERE id = ? FOR UPDATE";
    private static final String BOUND_BUSINESS_TYPES_SQL = """
            SELECT DISTINCT binding_type
              FROM (
                    SELECT 'daily_work_report' AS binding_type
                      FROM daily_work_report r
                      JOIN daily_work_report_file f ON f.daily_work_report_id = r.id
                     WHERE f.file_id = ? AND r.deleted = 0
                    UNION ALL
                    SELECT 'variation_order' AS binding_type
                      FROM variation_order o
                      JOIN variation_order_file f ON f.variation_order_id = o.id
                     WHERE f.file_id = ? AND o.deleted = 0
                    UNION ALL
                    SELECT 'project_payment' AS binding_type
                      FROM project_payment_record
                     WHERE voucher_file_id = ? AND deleted = 0
                    UNION ALL
                    SELECT 'project_document' AS binding_type
                      FROM project_document_archive
                     WHERE file_id = ? AND deleted = 0
                    UNION ALL
                    SELECT 'salary_payment' AS binding_type
                      FROM salary_monthly_detail
                     WHERE payment_proof_url = ? AND deleted = 0
                   ) bindings
            """;

    private final FileRecordMapper fileRecordMapper;
    private final ProjectAccessService projectAccessService;
    private final AuditUserProvider auditUserProvider;
    private final JdbcTemplate jdbcTemplate;

    public FileRecordDO requireProjectFileBindable(Long fileId, Long projectId) {
        FileRecordDO file = requireFile(fileId);
        if (isCurrentUploader(file) || hasProjectAccess(projectId)) {
            return file;
        }
        throw new BusinessException("无权绑定该附件");
    }

    public String normalizeSalaryProofUrl(String proofUrl) {
        if (!StringUtils.hasText(proofUrl)) {
            return null;
        }
        Long fileId = privateDownloadFileId(proofUrl.trim());
        if (fileId == null) {
            throw new BusinessException("工资凭证必须使用已上传文件");
        }
        FileRecordDO file = requireFile(fileId);
        if (isCurrentUploader(file) || AccessControlSupport.hasSalaryManageRole(authentication())) {
            return "/api/files/" + fileId + "/download";
        }
        throw new BusinessException("无权绑定工资凭证");
    }

    public String normalizeSalaryProofUrlForPayment(String proofUrl) {
        String normalizedProofUrl = normalizeSalaryProofUrl(proofUrl);
        if (normalizedProofUrl != null) {
            requireFileNotBoundToOtherBusiness(privateDownloadFileId(normalizedProofUrl), SALARY_PAYMENT);
        }
        return normalizedProofUrl;
    }

    /**
     * A stored file may be reused within the same business domain (for example, a batch salary proof),
     * but must not become evidence for a different domain.
     */
    public void requireFileNotBoundToOtherBusiness(Long fileId, String businessType) {
        jdbcTemplate.queryForObject(LOCK_FILE_SQL, Long.class, fileId);
        List<String> boundBusinessTypes = jdbcTemplate.queryForList(
                BOUND_BUSINESS_TYPES_SQL,
                String.class,
                fileId,
                fileId,
                fileId,
                fileId,
                "/api/files/" + fileId + "/download");
        if (boundBusinessTypes.stream().anyMatch(type -> !businessType.equals(type))) {
            throw new BusinessException("附件已被其他业务使用");
        }
    }

    private FileRecordDO requireFile(Long fileId) {
        if (fileId == null) {
            throw new BusinessException("附件必填");
        }
        FileRecordDO file = fileRecordMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException("附件不存在");
        }
        return file;
    }

    private boolean isCurrentUploader(FileRecordDO file) {
        Long currentUserId = auditUserProvider.currentUserId();
        return currentUserId != null && currentUserId.equals(file.getCreatedBy());
    }

    private boolean hasProjectAccess(Long projectId) {
        Authentication authentication = authentication();
        if (projectAccessService.hasProjectWideAccess(authentication)) {
            return true;
        }
        if (projectId == null) {
            return false;
        }
        Set<Long> authorizedProjectIds = projectAccessService.authorizedProjectIds(authentication);
        return authorizedProjectIds != null && authorizedProjectIds.contains(projectId);
    }

    private Long privateDownloadFileId(String proofUrl) {
        Matcher matcher = PRIVATE_DOWNLOAD_URL.matcher(proofUrl);
        if (!matcher.matches()) {
            return null;
        }
        return Long.valueOf(matcher.group(1));
    }

    private Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}

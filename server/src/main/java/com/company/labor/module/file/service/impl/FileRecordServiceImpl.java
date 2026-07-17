package com.company.labor.module.file.service.impl;

import com.baomidou.mybatisplus.core.toolkit.IdWorker;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.file.domain.entity.FileRecordDO;
import com.company.labor.module.file.mapper.FileRecordMapper;
import com.company.labor.module.file.service.FileRecordService;
import com.company.labor.module.file.storage.FileStorageService;
import com.company.labor.module.project.service.ProjectAccessService;
import com.company.labor.security.AccessControlSupport;
import java.io.IOException;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class FileRecordServiceImpl extends ServiceImpl<FileRecordMapper, FileRecordDO> implements FileRecordService {
    private static final Map<String, Set<String>> ALLOWED_CONTENT_TYPES = Map.ofEntries(
            Map.entry("jpg", Set.of("image/jpeg")),
            Map.entry("jpeg", Set.of("image/jpeg")),
            Map.entry("png", Set.of("image/png")),
            Map.entry("webp", Set.of("image/webp")),
            Map.entry("pdf", Set.of("application/pdf")),
            Map.entry("txt", Set.of("text/plain")),
            Map.entry("csv", Set.of("text/csv", "application/csv", "application/vnd.ms-excel")),
            Map.entry("xls", Set.of("application/vnd.ms-excel")),
            Map.entry("xlsx", Set.of("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")),
            Map.entry("doc", Set.of("application/msword")),
            Map.entry("docx", Set.of("application/vnd.openxmlformats-officedocument.wordprocessingml.document"))
    );

    @Value("${laborflow.file.max-size-bytes:10485760}")
    private long maxUploadSizeBytes;
    private final JdbcTemplate jdbcTemplate;
    private final ProjectAccessService projectAccessService;
    private final FileStorageService fileStorageService;

    @Override
    public FileRecordDO store(MultipartFile file) throws IOException {
        String original = validate(file);
        String extension = extension(original);
        String fileName = UUID.randomUUID() + suffix(extension);
        fileStorageService.store(file, fileName);

        FileRecordDO record = new FileRecordDO();
        record.setOriginalName(original);
        record.setId(IdWorker.getId());
        record.setFileName(fileName);
        record.setFilePath("/api/files/" + record.getId() + "/download");
        record.setFileSize(file.getSize());
        record.setContentType(file.getContentType());
        record.setCreatedAt(LocalDateTime.now());
        save(record);
        return record;
    }

    @Override
    public FileRecordDO getAuthorizedRecord(Long id, Authentication authentication) {
        FileRecordDO record = getById(id);
        if (record == null) {
            throw new BusinessException("文件不存在");
        }
        requireDownloadAccess(record, authentication);
        return record;
    }

    @Override
    public FileDownload loadForDownload(Long id, Authentication authentication) throws IOException {
        FileRecordDO record = getAuthorizedRecord(id, authentication);
        if (!StringUtils.hasText(record.getFileName())) {
            throw new BusinessException("文件路径不存在");
        }
        return new FileDownload(record, fileStorageService.load(record.getFileName()));
    }

    private String validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("上传文件不能为空");
        }
        if (file.getSize() > maxUploadSizeBytes) {
            throw new BusinessException("文件大小超过限制");
        }
        String original = file.getOriginalFilename();
        if (!StringUtils.hasText(original)) {
            throw new BusinessException("文件名不能为空");
        }
        if (original.contains("/") || original.contains("\\") || original.contains("..") || containsControlChar(original)) {
            log.warn("upload rejected: invalid filename");
            throw new BusinessException("文件名不合法");
        }
        String extension = extension(original);
        if (!ALLOWED_CONTENT_TYPES.containsKey(extension)) {
            log.warn("upload rejected: unsupported extension '{}'", extension);
            throw new BusinessException("文件类型不支持");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)
                || !ALLOWED_CONTENT_TYPES.get(extension).contains(contentType.toLowerCase(Locale.ROOT))) {
            log.warn("upload rejected: mime mismatch ext='{}' contentType='{}'", extension, contentType);
            throw new BusinessException("文件 MIME 类型不匹配");
        }
        return original;
    }

    private String extension(String filename) {
        int dot = filename.lastIndexOf('.');
        if (dot <= 0 || dot == filename.length() - 1) {
            throw new BusinessException("文件扩展名不能为空");
        }
        return filename.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private String suffix(String extension) {
        return "." + extension;
    }

    private boolean containsControlChar(String value) {
        for (int i = 0; i < value.length(); i++) {
            if (Character.isISOControl(value.charAt(i))) {
                return true;
            }
        }
        return false;
    }

    private void requireDownloadAccess(FileRecordDO record, Authentication authentication) {
        Long currentUserId = AccessControlSupport.currentUserId(authentication);
        if (currentUserId != null && currentUserId.equals(record.getCreatedBy())) {
            return;
        }
        List<Long> projectIds = associatedProjectIds(record.getId());
        if (!projectIds.isEmpty()) {
            if (projectAccessService.hasProjectWideAccess(authentication)) {
                return;
            }
            if (projectAccessService.authorizedProjectIds(authentication).stream().anyMatch(projectIds::contains)) {
                return;
            }
        }
        if (currentUserId != null && associatedCreatorIds(record.getId()).contains(currentUserId)) {
            return;
        }
        throw new BusinessException("无权下载该文件");
    }

    private List<Long> associatedProjectIds(Long fileId) {
        String sql = """
                SELECT project_id FROM daily_work_report r
                  JOIN daily_work_report_file f ON f.daily_work_report_id = r.id
                 WHERE f.file_id = ? AND r.deleted = 0
                UNION
                SELECT project_id FROM variation_order o
                  JOIN variation_order_file f ON f.variation_order_id = o.id
                 WHERE f.file_id = ? AND o.deleted = 0
                UNION
                SELECT project_id FROM project_payment_record
                 WHERE voucher_file_id = ? AND deleted = 0
                UNION
                SELECT project_id FROM project_document_archive
                 WHERE file_id = ? AND deleted = 0
                """;
        return jdbcTemplate.queryForList(sql, Long.class, fileId, fileId, fileId, fileId);
    }

    private List<Long> associatedCreatorIds(Long fileId) {
        String sql = """
                SELECT created_by FROM daily_work_report r
                  JOIN daily_work_report_file f ON f.daily_work_report_id = r.id
                 WHERE f.file_id = ? AND r.deleted = 0 AND r.created_by IS NOT NULL
                UNION
                SELECT created_by FROM variation_order o
                  JOIN variation_order_file f ON f.variation_order_id = o.id
                 WHERE f.file_id = ? AND o.deleted = 0 AND o.created_by IS NOT NULL
                """;
        return jdbcTemplate.queryForList(sql, Long.class, fileId, fileId);
    }
}

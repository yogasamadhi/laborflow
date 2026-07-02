package com.company.labor.module.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.file.domain.entity.FileRecordDO;
import com.company.labor.module.file.mapper.FileRecordMapper;
import com.company.labor.module.file.service.FileRecordService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
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

    @Value("${laborflow.upload-dir}")
    private String uploadDir;
    @Value("${laborflow.file.max-size-bytes:10485760}")
    private long maxUploadSizeBytes;

    @Override
    public FileRecordDO store(MultipartFile file) throws IOException {
        String original = validate(file);
        String extension = extension(original);
        Path uploadRoot = Path.of(uploadDir).toAbsolutePath().normalize();
        Files.createDirectories(uploadRoot);
        String fileName = UUID.randomUUID() + suffix(extension);
        Path target = uploadRoot.resolve(fileName).normalize();
        if (!target.startsWith(uploadRoot)) {
            throw new BusinessException("非法文件路径");
        }
        file.transferTo(target);

        FileRecordDO record = new FileRecordDO();
        record.setOriginalName(original);
        record.setFileName(fileName);
        record.setFilePath("/uploads/" + fileName);
        record.setFileSize(file.getSize());
        record.setContentType(file.getContentType());
        record.setCreatedAt(LocalDateTime.now());
        save(record);
        return record;
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
            throw new BusinessException("文件名不合法");
        }
        String extension = extension(original);
        if (!ALLOWED_CONTENT_TYPES.containsKey(extension)) {
            throw new BusinessException("文件类型不支持");
        }
        String contentType = file.getContentType();
        if (!StringUtils.hasText(contentType)
                || !ALLOWED_CONTENT_TYPES.get(extension).contains(contentType.toLowerCase(Locale.ROOT))) {
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
}

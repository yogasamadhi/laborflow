package com.company.labor.module.file.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.labor.module.file.domain.entity.FileRecordDO;
import com.company.labor.module.file.mapper.FileRecordMapper;
import com.company.labor.module.file.service.FileRecordService;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDateTime;
import java.util.UUID;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
public class FileRecordServiceImpl extends ServiceImpl<FileRecordMapper, FileRecordDO> implements FileRecordService {
    @Value("${laborflow.upload-dir}")
    private String uploadDir;

    @Override
    public FileRecordDO store(MultipartFile file) throws IOException {
        Files.createDirectories(Path.of(uploadDir));
        String original = file.getOriginalFilename() == null ? "file" : file.getOriginalFilename();
        String suffix = "";
        int dot = original.lastIndexOf('.');
        if (dot >= 0) {
            suffix = original.substring(dot);
        }
        String fileName = UUID.randomUUID() + suffix;
        Path target = Path.of(uploadDir, fileName);
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
}


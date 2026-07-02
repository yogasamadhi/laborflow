package com.company.labor.module.file.storage;

import com.company.labor.common.exception.BusinessException;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.io.PathResource;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(prefix = "laborflow.file.storage", name = "type", havingValue = "local", matchIfMissing = true)
public class LocalFileStorageService implements FileStorageService {
    @Value("${laborflow.file.storage.local.base-dir:${laborflow.upload-dir:./uploads}}")
    private String baseDir;

    @Override
    public void store(MultipartFile file, String fileName) throws IOException {
        Path root = root();
        Files.createDirectories(root);
        Path target = root.resolve(fileName).normalize();
        if (!target.startsWith(root)) {
            throw new BusinessException("非法文件路径");
        }
        file.transferTo(target);
    }

    @Override
    public Resource load(String fileName) {
        Path root = root();
        Path filePath = root.resolve(fileName).normalize();
        if (!filePath.startsWith(root)) {
            throw new BusinessException("非法文件路径");
        }
        if (!Files.isRegularFile(filePath)) {
            throw new BusinessException("文件不存在或已被移除");
        }
        return new PathResource(filePath);
    }

    private Path root() {
        return Path.of(baseDir).toAbsolutePath().normalize();
    }
}

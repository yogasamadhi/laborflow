package com.company.labor.module.file.storage;

import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

public interface FileStorageService {
    void store(MultipartFile file, String fileName) throws IOException;

    Resource load(String fileName) throws IOException;
}

package com.company.labor.module.file.controller;

import com.company.labor.common.result.Result;
import com.company.labor.module.file.domain.entity.FileRecordDO;
import com.company.labor.module.file.service.FileRecordService;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/files")
@RequiredArgsConstructor
public class FileController {
    private final FileRecordService service;

    @PostMapping("/upload")
    public Result<FileRecordDO> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.success(service.store(file));
    }

    @GetMapping("/{id}")
    public Result<FileRecordDO> detail(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }
}


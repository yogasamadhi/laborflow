package com.company.labor.module.file.controller;

import com.company.labor.common.result.Result;
import com.company.labor.module.file.domain.entity.FileRecordDO;
import com.company.labor.module.file.domain.vo.FileMetadataVO;
import com.company.labor.module.file.service.FileRecordService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
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
    public Result<FileMetadataVO> upload(@RequestParam("file") MultipartFile file) throws IOException {
        return Result.success(FileMetadataVO.from(service.store(file)));
    }

    @GetMapping("/{id}")
    public Result<FileMetadataVO> detail(@PathVariable Long id, Authentication authentication) {
        return Result.success(FileMetadataVO.from(service.getAuthorizedRecord(id, authentication)));
    }

    @GetMapping("/{id}/download")
    public ResponseEntity<Resource> download(@PathVariable Long id, Authentication authentication) throws IOException {
        return fileResponse(id, authentication, true);
    }

    @GetMapping("/{id}/preview")
    public ResponseEntity<Resource> preview(@PathVariable Long id, Authentication authentication) throws IOException {
        return fileResponse(id, authentication, false);
    }

    private ResponseEntity<Resource> fileResponse(Long id, Authentication authentication, boolean attachment) throws IOException {
        FileRecordService.FileDownload download = service.loadForDownload(id, authentication);
        FileRecordDO record = download.record();
        ContentDisposition disposition = attachment
                ? ContentDisposition.attachment().filename(record.getOriginalName(), StandardCharsets.UTF_8).build()
                : ContentDisposition.inline().filename(record.getOriginalName(), StandardCharsets.UTF_8).build();
        ResponseEntity.BodyBuilder builder = ResponseEntity.ok()
                .contentType(mediaType(record.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString());
        if (record.getFileSize() != null) {
            builder.contentLength(record.getFileSize());
        }
        return builder.body(download.resource());
    }

    private MediaType mediaType(String contentType) {
        try {
            return MediaType.parseMediaType(contentType);
        } catch (Exception ex) {
            return MediaType.APPLICATION_OCTET_STREAM;
        }
    }
}

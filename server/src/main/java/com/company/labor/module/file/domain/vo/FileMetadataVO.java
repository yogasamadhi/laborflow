package com.company.labor.module.file.domain.vo;

import com.company.labor.module.file.domain.entity.FileRecordDO;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class FileMetadataVO {
    private Long id;
    private String originalName;
    private String contentType;
    private Long fileSize;
    private LocalDateTime createdAt;
    private String downloadUrl;
    private String previewUrl;

    public static FileMetadataVO from(FileRecordDO record) {
        return new FileMetadataVO(
                record.getId(),
                record.getOriginalName(),
                record.getContentType(),
                record.getFileSize(),
                record.getCreatedAt(),
                "/api/files/" + record.getId() + "/download",
                "/api/files/" + record.getId() + "/preview");
    }
}

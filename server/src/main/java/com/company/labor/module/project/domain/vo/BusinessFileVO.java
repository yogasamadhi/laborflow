package com.company.labor.module.project.domain.vo;

import com.company.labor.module.file.domain.vo.FileMetadataVO;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class BusinessFileVO {
    private Long bindingId;
    private String fileType;
    private LocalDateTime attachedAt;
    private FileMetadataVO file;
}

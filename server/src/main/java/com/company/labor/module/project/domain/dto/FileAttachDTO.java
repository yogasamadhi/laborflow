package com.company.labor.module.project.domain.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class FileAttachDTO {
    @NotNull(message = "附件必填")
    private Long fileId;
    @Pattern(regexp = "photo|evidence|voucher|document|other", message = "附件类型不合法")
    private String fileType;
}

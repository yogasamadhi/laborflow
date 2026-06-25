package com.company.labor.module.file.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import java.time.LocalDateTime;
import lombok.Data;

@Data
@TableName("file_record")
public class FileRecordDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String originalName;
    private String fileName;
    private String filePath;
    private Long fileSize;
    private String contentType;
    private Long createdBy;
    private LocalDateTime createdAt;
}


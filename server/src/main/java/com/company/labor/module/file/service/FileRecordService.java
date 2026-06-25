package com.company.labor.module.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.labor.module.file.domain.entity.FileRecordDO;
import java.io.IOException;
import org.springframework.web.multipart.MultipartFile;

public interface FileRecordService extends IService<FileRecordDO> {
    FileRecordDO store(MultipartFile file) throws IOException;
}


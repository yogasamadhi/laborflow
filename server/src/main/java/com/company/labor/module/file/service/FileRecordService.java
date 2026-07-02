package com.company.labor.module.file.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.labor.module.file.domain.entity.FileRecordDO;
import java.io.IOException;
import org.springframework.core.io.Resource;
import org.springframework.security.core.Authentication;
import org.springframework.web.multipart.MultipartFile;

public interface FileRecordService extends IService<FileRecordDO> {
    record FileDownload(FileRecordDO record, Resource resource) {
    }

    FileRecordDO store(MultipartFile file) throws IOException;

    FileRecordDO getAuthorizedRecord(Long id, Authentication authentication);

    FileDownload loadForDownload(Long id, Authentication authentication) throws IOException;
}

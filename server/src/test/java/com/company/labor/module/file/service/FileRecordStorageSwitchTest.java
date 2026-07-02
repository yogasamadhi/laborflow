package com.company.labor.module.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.labor.module.file.domain.entity.FileRecordDO;
import com.company.labor.module.file.mapper.FileRecordMapper;
import com.company.labor.module.file.service.impl.FileRecordServiceImpl;
import com.company.labor.module.file.storage.FileStorageService;
import com.company.labor.module.project.service.ProjectAccessService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class FileRecordStorageSwitchTest {
    private FileStorageService fileStorageService;
    private FileRecordMapper fileRecordMapper;
    private FileRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        fileStorageService = mock(FileStorageService.class);
        fileRecordMapper = mock(FileRecordMapper.class);
        service = new FileRecordServiceImpl(mock(JdbcTemplate.class), mock(ProjectAccessService.class), fileStorageService);
        ReflectionTestUtils.setField(service, "baseMapper", fileRecordMapper);
        ReflectionTestUtils.setField(service, "maxUploadSizeBytes", 1024L);
    }

    @Test
    void storesThroughConfiguredStorageAndReturnsPrivateDownloadPath() throws Exception {
        when(fileRecordMapper.insert(any(FileRecordDO.class))).thenAnswer(invocation -> {
            FileRecordDO record = invocation.getArgument(0);
            record.setId(99L);
            return 1;
        });
        MockMultipartFile file = new MockMultipartFile("file", "proof.pdf", "application/pdf", "ok".getBytes());

        FileRecordDO record = service.store(file);

        assertThat(record.getFileName()).endsWith(".pdf");
        assertThat(record.getFilePath()).isEqualTo("/api/files/99/download");
        verify(fileStorageService).store(eq(file), eq(record.getFileName()));
        ArgumentCaptor<FileRecordDO> updateCaptor = ArgumentCaptor.forClass(FileRecordDO.class);
        verify(fileRecordMapper).updateById(updateCaptor.capture());
        assertThat(updateCaptor.getValue().getFilePath()).isEqualTo("/api/files/99/download");
    }
}

package com.company.labor.module.file.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.file.service.impl.FileRecordServiceImpl;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

class FileRecordServiceSecurityTest {
    private FileRecordServiceImpl service;

    @BeforeEach
    void setUp() {
        service = new FileRecordServiceImpl();
        ReflectionTestUtils.setField(service, "uploadDir", "/tmp/laborflow-upload-test");
        ReflectionTestUtils.setField(service, "maxUploadSizeBytes", 8L);
    }

    @Test
    void rejectsEmptyFileBeforeDiskOrDatabaseWrite() {
        MockMultipartFile file = new MockMultipartFile("file", "empty.pdf", "application/pdf", new byte[0]);

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("上传文件不能为空");
    }

    @Test
    void rejectsOversizedFileBeforeDiskOrDatabaseWrite() {
        MockMultipartFile file = new MockMultipartFile("file", "large.pdf", "application/pdf", "too-large".getBytes());

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件大小超过限制");
    }

    @Test
    void rejectsDangerousFilenameBeforeDiskOrDatabaseWrite() {
        MockMultipartFile file = new MockMultipartFile("file", "../proof.pdf", "application/pdf", "x".getBytes());

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件名不合法");
    }

    @Test
    void rejectsUnsupportedExtensionBeforeDiskOrDatabaseWrite() {
        MockMultipartFile file = new MockMultipartFile("file", "shell.jsp", "text/plain", "x".getBytes());

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件类型不支持");
    }

    @Test
    void rejectsMimeMismatchBeforeDiskOrDatabaseWrite() {
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "application/pdf", "x".getBytes());

        assertThatThrownBy(() -> service.store(file))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("文件 MIME 类型不匹配");
    }
}

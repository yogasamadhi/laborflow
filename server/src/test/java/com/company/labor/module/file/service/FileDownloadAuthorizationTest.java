package com.company.labor.module.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.file.domain.entity.FileRecordDO;
import com.company.labor.module.file.mapper.FileRecordMapper;
import com.company.labor.module.file.service.impl.FileRecordServiceImpl;
import com.company.labor.module.file.storage.FileStorageService;
import com.company.labor.module.project.service.ProjectAccessService;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.security.LoginUser;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.PathResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.test.util.ReflectionTestUtils;

class FileDownloadAuthorizationTest {
    @TempDir
    Path uploadRoot;

    private FileRecordServiceImpl service;
    private JdbcTemplate jdbcTemplate;
    private ProjectAccessService projectAccessService;
    private FileStorageService fileStorageService;

    @BeforeEach
    void setUp() {
        jdbcTemplate = mock(JdbcTemplate.class);
        projectAccessService = mock(ProjectAccessService.class);
        fileStorageService = mock(FileStorageService.class);
        service = new FileRecordServiceImpl(jdbcTemplate, projectAccessService, fileStorageService);
        ReflectionTestUtils.setField(service, "baseMapper", fileMapper(record(1L, 10L)));
    }

    @Test
    void downloadsOwnFileFromPrivateUploadDirectory() throws IOException {
        Files.writeString(uploadRoot.resolve("proof.pdf"), "ok");
        when(fileStorageService.load("proof.pdf")).thenReturn(new PathResource(uploadRoot.resolve("proof.pdf")));

        FileRecordService.FileDownload download = service.loadForDownload(1L, authentication(10L, null, "EMPLOYEE"));

        assertThat(download.record().getId()).isEqualTo(1L);
        assertThat(download.resource().exists()).isTrue();
        assertThat(download.resource().getFilename()).isEqualTo("proof.pdf");
    }

    @Test
    void downloadsWhenUserHasAssociatedProjectAccess() throws IOException {
        Files.writeString(uploadRoot.resolve("proof.pdf"), "ok");
        Authentication authentication = authentication(20L, null, "EMPLOYEE");
        when(projectAccessService.hasProjectWideAccess(authentication)).thenReturn(false);
        when(projectAccessService.authorizedProjectIds(authentication)).thenReturn(Set.of(99L));
        stubBusinessAssociations(List.of(99L), List.of());
        when(fileStorageService.load("proof.pdf")).thenReturn(new PathResource(uploadRoot.resolve("proof.pdf")));

        FileRecordService.FileDownload download = service.loadForDownload(1L, authentication);

        assertThat(download.resource().exists()).isTrue();
    }

    @Test
    void rejectsProjectWideRoleWhenFileIsNotBusinessAssociated() {
        Authentication authentication = authentication(20L, null, "MANAGER");
        when(projectAccessService.hasProjectWideAccess(authentication)).thenReturn(true);
        stubBusinessAssociations(List.of(), List.of());

        assertThatThrownBy(() -> service.loadForDownload(1L, authentication))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权下载该文件");
    }

    @Test
    void rejectsMetadataWhenUserIsNotAuthorized() {
        Authentication authentication = authentication(20L, null, "EMPLOYEE");
        when(projectAccessService.hasProjectWideAccess(authentication)).thenReturn(false);
        when(projectAccessService.authorizedProjectIds(authentication)).thenReturn(Set.of());
        stubBusinessAssociations(List.of(), List.of());

        assertThatThrownBy(() -> service.getAuthorizedRecord(1L, authentication))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权下载该文件");
    }

    @Test
    void rejectsDownloadWhenUserIsNotOwnerOrBusinessAuthorized() {
        Authentication authentication = authentication(20L, null, "EMPLOYEE");
        when(projectAccessService.hasProjectWideAccess(authentication)).thenReturn(false);
        when(projectAccessService.authorizedProjectIds(authentication)).thenReturn(Set.of());
        stubBusinessAssociations(List.of(), List.of());

        assertThatThrownBy(() -> service.loadForDownload(1L, authentication))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("无权下载该文件");
    }

    private void stubBusinessAssociations(List<Long> projectIds, List<Long> creatorIds) {
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(), any(), any(), any()))
                .thenReturn(projectIds);
        when(jdbcTemplate.queryForList(anyString(), eq(Long.class), any(), any()))
                .thenReturn(creatorIds);
    }

    private FileRecordMapper fileMapper(FileRecordDO record) {
        FileRecordMapper mapper = mock(FileRecordMapper.class);
        when(mapper.selectById(1L)).thenReturn(record);
        return mapper;
    }

    private FileRecordDO record(Long id, Long createdBy) {
        FileRecordDO record = new FileRecordDO();
        record.setId(id);
        record.setCreatedBy(createdBy);
        record.setFileName("proof.pdf");
        record.setOriginalName("proof.pdf");
        record.setFileSize(1L);
        record.setContentType("application/pdf");
        return record;
    }

    private Authentication authentication(Long userId, Long employeeId, String role) {
        SysUserDO user = new SysUserDO();
        user.setId(userId);
        user.setEmployeeId(employeeId);
        user.setUsername("user" + userId);
        user.setStatus("active");
        LoginUser loginUser = new LoginUser(user, List.of(role));
        return new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
    }
}

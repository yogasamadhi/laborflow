package com.company.labor.module.file.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.file.domain.entity.FileRecordDO;
import com.company.labor.module.file.mapper.FileRecordMapper;
import com.company.labor.module.project.service.ProjectAccessService;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.security.AuditUserProvider;
import com.company.labor.security.LoginUser;
import java.util.List;
import java.util.Set;
import org.springframework.jdbc.core.JdbcTemplate;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

class FileBindingGuardTest {
    private FileRecordMapper fileRecordMapper;
    private ProjectAccessService projectAccessService;
    private AuditUserProvider auditUserProvider;
    private JdbcTemplate jdbcTemplate;
    private FileBindingGuard guard;

    @BeforeEach
    void setUp() {
        fileRecordMapper = mock(FileRecordMapper.class);
        projectAccessService = mock(ProjectAccessService.class);
        auditUserProvider = mock(AuditUserProvider.class);
        jdbcTemplate = mock(JdbcTemplate.class);
        guard = new FileBindingGuard(fileRecordMapper, projectAccessService, auditUserProvider, jdbcTemplate);
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    void rejectsMissingProjectAttachmentFile() {
        when(fileRecordMapper.selectById(7L)).thenReturn(null);

        assertThatThrownBy(() -> guard.requireProjectFileBindable(7L, 5L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("附件不存在");
    }

    @Test
    void allowsUploaderToBindProjectAttachment() {
        when(fileRecordMapper.selectById(7L)).thenReturn(file(7L, 20L));
        when(auditUserProvider.currentUserId()).thenReturn(20L);

        assertThat(guard.requireProjectFileBindable(7L, 5L).getId()).isEqualTo(7L);
    }

    @Test
    void allowsAuthorizedProjectUserToBindProjectAttachment() {
        Authentication authentication = authentication(20L, "EMPLOYEE");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(fileRecordMapper.selectById(7L)).thenReturn(file(7L, 99L));
        when(auditUserProvider.currentUserId()).thenReturn(20L);
        when(projectAccessService.hasProjectWideAccess(authentication)).thenReturn(false);
        when(projectAccessService.authorizedProjectIds(authentication)).thenReturn(Set.of(5L));

        assertThat(guard.requireProjectFileBindable(7L, 5L).getId()).isEqualTo(7L);
    }

    @Test
    void normalizesSalaryProofPrivateDownloadUrl() {
        Authentication authentication = authentication(20L, "FINANCE");
        SecurityContextHolder.getContext().setAuthentication(authentication);
        when(fileRecordMapper.selectById(8L)).thenReturn(file(8L, 99L));
        when(auditUserProvider.currentUserId()).thenReturn(20L);

        assertThat(guard.normalizeSalaryProofUrl("/files/8/download")).isEqualTo("/api/files/8/download");
    }

    @Test
    void rejectsExternalSalaryProofUrl() {
        assertThatThrownBy(() -> guard.normalizeSalaryProofUrl("https://example.com/proof.pdf"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("工资凭证必须使用已上传文件");
    }

    private FileRecordDO file(Long id, Long createdBy) {
        FileRecordDO file = new FileRecordDO();
        file.setId(id);
        file.setCreatedBy(createdBy);
        return file;
    }

    private Authentication authentication(Long userId, String role) {
        SysUserDO user = new SysUserDO();
        user.setId(userId);
        user.setUsername("user" + userId);
        user.setStatus("active");
        LoginUser loginUser = new LoginUser(user, List.of(role));
        return new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
    }
}

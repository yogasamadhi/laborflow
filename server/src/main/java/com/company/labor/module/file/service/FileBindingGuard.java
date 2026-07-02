package com.company.labor.module.file.service;

import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.file.domain.entity.FileRecordDO;
import com.company.labor.module.file.mapper.FileRecordMapper;
import com.company.labor.module.project.service.ProjectAccessService;
import com.company.labor.security.AccessControlSupport;
import com.company.labor.security.AuditUserProvider;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
@RequiredArgsConstructor
public class FileBindingGuard {
    private static final Pattern PRIVATE_DOWNLOAD_URL =
            Pattern.compile("^/(?:api/)?files/(\\d+)/download$");

    private final FileRecordMapper fileRecordMapper;
    private final ProjectAccessService projectAccessService;
    private final AuditUserProvider auditUserProvider;

    public FileRecordDO requireProjectFileBindable(Long fileId, Long projectId) {
        FileRecordDO file = requireFile(fileId);
        if (isCurrentUploader(file) || hasProjectAccess(projectId)) {
            return file;
        }
        throw new BusinessException("无权绑定该附件");
    }

    public String normalizeSalaryProofUrl(String proofUrl) {
        if (!StringUtils.hasText(proofUrl)) {
            return null;
        }
        Long fileId = privateDownloadFileId(proofUrl.trim());
        if (fileId == null) {
            throw new BusinessException("工资凭证必须使用已上传文件");
        }
        FileRecordDO file = requireFile(fileId);
        if (isCurrentUploader(file) || AccessControlSupport.hasSalaryManageRole(authentication())) {
            return "/api/files/" + fileId + "/download";
        }
        throw new BusinessException("无权绑定工资凭证");
    }

    private FileRecordDO requireFile(Long fileId) {
        if (fileId == null) {
            throw new BusinessException("附件必填");
        }
        FileRecordDO file = fileRecordMapper.selectById(fileId);
        if (file == null) {
            throw new BusinessException("附件不存在");
        }
        return file;
    }

    private boolean isCurrentUploader(FileRecordDO file) {
        Long currentUserId = auditUserProvider.currentUserId();
        return currentUserId != null && currentUserId.equals(file.getCreatedBy());
    }

    private boolean hasProjectAccess(Long projectId) {
        Authentication authentication = authentication();
        if (projectAccessService.hasProjectWideAccess(authentication)) {
            return true;
        }
        if (projectId == null) {
            return false;
        }
        Set<Long> authorizedProjectIds = projectAccessService.authorizedProjectIds(authentication);
        return authorizedProjectIds != null && authorizedProjectIds.contains(projectId);
    }

    private Long privateDownloadFileId(String proofUrl) {
        Matcher matcher = PRIVATE_DOWNLOAD_URL.matcher(proofUrl);
        if (!matcher.matches()) {
            return null;
        }
        return Long.valueOf(matcher.group(1));
    }

    private Authentication authentication() {
        return SecurityContextHolder.getContext().getAuthentication();
    }
}

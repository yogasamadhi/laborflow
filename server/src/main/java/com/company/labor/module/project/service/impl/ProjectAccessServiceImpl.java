package com.company.labor.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.project.domain.entity.ProjectMemberDO;
import com.company.labor.module.project.mapper.ProjectMemberMapper;
import com.company.labor.module.project.service.ProjectAccessService;
import com.company.labor.security.AccessControlSupport;
import java.util.LinkedHashSet;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ProjectAccessServiceImpl implements ProjectAccessService {
    private final ProjectMemberMapper projectMemberMapper;

    @Override
    public boolean hasProjectWideAccess(Authentication authentication) {
        return AccessControlSupport.hasProjectFinanceRole(authentication);
    }

    @Override
    public Set<Long> authorizedProjectIds(Authentication authentication) {
        if (hasProjectWideAccess(authentication)) {
            return Set.of();
        }
        Long employeeId = AccessControlSupport.currentEmployeeId(authentication);
        if (employeeId == null) {
            return Set.of();
        }
        LambdaQueryWrapper<ProjectMemberDO> wrapper = new LambdaQueryWrapper<ProjectMemberDO>()
                .eq(ProjectMemberDO::getDeleted, 0)
                .eq(ProjectMemberDO::getStatus, "active")
                .eq(ProjectMemberDO::getEmployeeId, employeeId);
        Set<Long> projectIds = new LinkedHashSet<>();
        for (ProjectMemberDO member : projectMemberMapper.selectList(wrapper)) {
            if (member.getProjectId() != null) {
                projectIds.add(member.getProjectId());
            }
        }
        return projectIds;
    }

    @Override
    public void requireProjectAccess(Long projectId, Authentication authentication) {
        if (projectId == null || hasProjectWideAccess(authentication)) {
            return;
        }
        if (!authorizedProjectIds(authentication).contains(projectId)) {
            throw new BusinessException("无权访问该项目");
        }
    }
}

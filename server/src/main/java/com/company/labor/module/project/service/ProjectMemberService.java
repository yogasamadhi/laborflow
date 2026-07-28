package com.company.labor.module.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.module.project.domain.dto.ProjectMemberSaveDTO;
import com.company.labor.module.project.domain.entity.ProjectMemberDO;

public interface ProjectMemberService {
    Page<ProjectMemberDO> page(long page, long pageSize, Long projectId, Long employeeId, String status);
    ProjectMemberDO create(ProjectMemberSaveDTO dto);
    ProjectMemberDO update(Long id, ProjectMemberSaveDTO dto);
    void delete(Long id);
}

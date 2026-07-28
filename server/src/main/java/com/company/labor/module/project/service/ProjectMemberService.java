package com.company.labor.module.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.module.project.domain.dto.ProjectMemberSaveDTO;
import com.company.labor.module.project.domain.entity.ProjectMemberDO;
import com.company.labor.module.project.domain.vo.ProjectMemberVO;

public interface ProjectMemberService {
    Page<ProjectMemberVO> page(long page, long pageSize, Long projectId, Long employeeId,
                               Long positionId, String keyword, String status);
    ProjectMemberVO create(ProjectMemberSaveDTO dto);
    ProjectMemberVO update(Long id, ProjectMemberSaveDTO dto);
    void delete(Long id);
}

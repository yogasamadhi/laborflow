package com.company.labor.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.employee.domain.entity.EmployeeDO;
import com.company.labor.module.employee.mapper.EmployeeMapper;
import com.company.labor.module.project.domain.dto.ProjectMemberSaveDTO;
import com.company.labor.module.project.domain.entity.ProjectDO;
import com.company.labor.module.project.domain.entity.ProjectMemberDO;
import com.company.labor.module.project.mapper.ProjectMapper;
import com.company.labor.module.project.mapper.ProjectMemberMapper;
import com.company.labor.module.project.service.ProjectMemberService;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.mapper.SysUserMapper;
import com.company.labor.module.system.service.OperationLogService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProjectMemberServiceImpl implements ProjectMemberService {
    private final ProjectMemberMapper projectMemberMapper;
    private final ProjectMapper projectMapper;
    private final EmployeeMapper employeeMapper;
    private final SysUserMapper sysUserMapper;
    private final OperationLogService operationLogService;

    @Override
    public Page<ProjectMemberDO> page(long page, long pageSize, Long projectId, Long employeeId, String status) {
        return projectMemberMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<ProjectMemberDO>()
                .eq(ProjectMemberDO::getDeleted, 0)
                .eq(projectId != null, ProjectMemberDO::getProjectId, projectId)
                .eq(employeeId != null, ProjectMemberDO::getEmployeeId, employeeId)
                .eq(StringUtils.hasText(status), ProjectMemberDO::getStatus, status)
                .orderByDesc(ProjectMemberDO::getCreatedAt)
                .orderByDesc(ProjectMemberDO::getId));
    }

    @Override
    @Transactional
    public ProjectMemberDO create(ProjectMemberSaveDTO dto) {
        validate(dto, null);
        ProjectMemberDO member = toEntity(dto);
        member.setDeleted(0);
        projectMemberMapper.insert(member);
        operationLogService.record("project_member", member.getId(), "create_project_member",
                "新增项目成员，项目ID:" + member.getProjectId() + "，员工ID:" + member.getEmployeeId());
        return member;
    }

    @Override
    @Transactional
    public ProjectMemberDO update(Long id, ProjectMemberSaveDTO dto) {
        requireMember(id);
        validate(dto, id);
        ProjectMemberDO member = toEntity(dto);
        member.setId(id);
        projectMemberMapper.updateById(member);
        operationLogService.record("project_member", id, "update_project_member",
                "更新项目成员，项目ID:" + member.getProjectId() + "，状态:" + member.getStatus());
        return requireMember(id);
    }

    @Override
    @Transactional
    public void delete(Long id) {
        ProjectMemberDO member = requireMember(id);
        projectMemberMapper.deleteById(id);
        operationLogService.record("project_member", id, "delete_project_member",
                "移除项目成员，项目ID:" + member.getProjectId() + "，员工ID:" + member.getEmployeeId());
    }

    private void validate(ProjectMemberSaveDTO dto, Long currentId) {
        ProjectDO project = projectMapper.selectById(dto.getProjectId());
        if (project == null) throw new BusinessException("项目不存在");
        if (dto.getEmployeeId() == null && dto.getUserId() == null) {
            throw new BusinessException("员工或系统用户至少选择一项");
        }
        if (dto.getEmployeeId() != null) {
            EmployeeDO employee = employeeMapper.selectById(dto.getEmployeeId());
            if (employee == null) throw new BusinessException("员工不存在");
        }
        if (dto.getUserId() != null) {
            SysUserDO user = sysUserMapper.selectById(dto.getUserId());
            if (user == null) throw new BusinessException("系统用户不存在");
        }
        long duplicates = projectMemberMapper.selectCount(new LambdaQueryWrapper<ProjectMemberDO>()
                .eq(ProjectMemberDO::getDeleted, 0)
                .eq(ProjectMemberDO::getProjectId, dto.getProjectId())
                .eq(dto.getEmployeeId() != null, ProjectMemberDO::getEmployeeId, dto.getEmployeeId())
                .eq(dto.getEmployeeId() == null && dto.getUserId() != null, ProjectMemberDO::getUserId, dto.getUserId())
                .ne(currentId != null, ProjectMemberDO::getId, currentId));
        if (duplicates > 0) throw new BusinessException("该人员已是项目成员");
    }

    private ProjectMemberDO toEntity(ProjectMemberSaveDTO dto) {
        ProjectMemberDO member = new ProjectMemberDO();
        member.setProjectId(dto.getProjectId());
        member.setUserId(dto.getUserId());
        member.setEmployeeId(dto.getEmployeeId());
        member.setRoleName(StringUtils.hasText(dto.getRoleName()) ? dto.getRoleName().trim() : "项目成员");
        member.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "active");
        member.setRemark(dto.getRemark());
        return member;
    }

    private ProjectMemberDO requireMember(Long id) {
        ProjectMemberDO member = projectMemberMapper.selectById(id);
        if (member == null) throw new BusinessException("项目成员不存在");
        return member;
    }
}

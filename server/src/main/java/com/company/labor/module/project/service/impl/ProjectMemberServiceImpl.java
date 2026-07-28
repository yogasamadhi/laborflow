package com.company.labor.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.employee.domain.entity.EmployeeDO;
import com.company.labor.module.employee.domain.entity.PositionDO;
import com.company.labor.module.employee.mapper.EmployeeMapper;
import com.company.labor.module.employee.mapper.PositionMapper;
import com.company.labor.module.project.domain.dto.ProjectMemberSaveDTO;
import com.company.labor.module.project.domain.entity.ProjectDO;
import com.company.labor.module.project.domain.entity.ProjectMemberDO;
import com.company.labor.module.project.mapper.ProjectMapper;
import com.company.labor.module.project.mapper.ProjectMemberMapper;
import com.company.labor.module.project.domain.vo.ProjectMemberVO;
import com.company.labor.module.project.service.ProjectMemberService;
import com.company.labor.module.system.service.OperationLogService;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;
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
    private final PositionMapper positionMapper;
    private final OperationLogService operationLogService;

    @Override
    public Page<ProjectMemberVO> page(long page, long pageSize, Long projectId, Long employeeId,
                                      Long positionId, String keyword, String status) {
        List<Long> filteredEmployeeIds = null;
        if (positionId != null || StringUtils.hasText(keyword)) {
            filteredEmployeeIds = employeeMapper.selectList(new LambdaQueryWrapper<EmployeeDO>()
                    .eq(EmployeeDO::getDeleted, 0)
                    .eq(positionId != null, EmployeeDO::getPositionId, positionId)
                    .like(StringUtils.hasText(keyword), EmployeeDO::getName, keyword))
                    .stream().map(EmployeeDO::getId).toList();
            if (filteredEmployeeIds.isEmpty()) {
                return new Page<>(page, pageSize);
            }
        }
        Page<ProjectMemberDO> rows = projectMemberMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<ProjectMemberDO>()
                .eq(ProjectMemberDO::getDeleted, 0)
                .eq(projectId != null, ProjectMemberDO::getProjectId, projectId)
                .eq(employeeId != null, ProjectMemberDO::getEmployeeId, employeeId)
                .in(filteredEmployeeIds != null, ProjectMemberDO::getEmployeeId, filteredEmployeeIds)
                .eq(StringUtils.hasText(status), ProjectMemberDO::getStatus, status)
                .orderByDesc(ProjectMemberDO::getCreatedAt)
                .orderByDesc(ProjectMemberDO::getId));
        List<Long> employeeIds = rows.getRecords().stream().map(ProjectMemberDO::getEmployeeId).distinct().toList();
        Map<Long, EmployeeDO> employees = employeeIds.isEmpty() ? Map.of() : employeeMapper
                .selectBatchIds(employeeIds).stream().collect(Collectors.toMap(EmployeeDO::getId, Function.identity()));
        List<Long> positionIds = employees.values().stream().map(EmployeeDO::getPositionId)
                .filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, PositionDO> positions = positionIds.isEmpty() ? Map.of() : positionMapper
                .selectBatchIds(positionIds).stream().collect(Collectors.toMap(PositionDO::getId, Function.identity()));
        Page<ProjectMemberVO> result = new Page<>(rows.getCurrent(), rows.getSize(), rows.getTotal());
        result.setRecords(rows.getRecords().stream().map(row -> toVO(row, employees, positions)).toList());
        return result;
    }

    @Override
    @Transactional
    public ProjectMemberVO create(ProjectMemberSaveDTO dto) {
        validate(dto, null);
        ProjectMemberDO member = toEntity(dto);
        member.setDeleted(0);
        projectMemberMapper.insert(member);
        operationLogService.record("project_member", member.getId(), "create_project_member",
                "新增项目成员，项目ID:" + member.getProjectId() + "，员工ID:" + member.getEmployeeId());
        return toVO(member);
    }

    @Override
    @Transactional
    public ProjectMemberVO update(Long id, ProjectMemberSaveDTO dto) {
        requireMember(id);
        validate(dto, id);
        ProjectMemberDO member = toEntity(dto);
        member.setId(id);
        projectMemberMapper.updateById(member);
        operationLogService.record("project_member", id, "update_project_member",
                "更新项目成员，项目ID:" + member.getProjectId() + "，状态:" + member.getStatus());
        return toVO(requireMember(id));
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
        if (dto.getEmployeeId() == null) throw new BusinessException("员工必填");
        EmployeeDO employee = employeeMapper.selectById(dto.getEmployeeId());
        if (employee == null) throw new BusinessException("员工不存在");
        if (!"active".equals(employee.getStatus())) throw new BusinessException("只能添加在职员工");
        long duplicates = projectMemberMapper.selectCount(new LambdaQueryWrapper<ProjectMemberDO>()
                .eq(ProjectMemberDO::getDeleted, 0)
                .eq(ProjectMemberDO::getProjectId, dto.getProjectId())
                .eq(ProjectMemberDO::getEmployeeId, dto.getEmployeeId())
                .ne(currentId != null, ProjectMemberDO::getId, currentId));
        if (duplicates > 0) throw new BusinessException("该人员已是项目成员");
    }

    private ProjectMemberDO toEntity(ProjectMemberSaveDTO dto) {
        ProjectMemberDO member = new ProjectMemberDO();
        member.setProjectId(dto.getProjectId());
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

    private ProjectMemberVO toVO(ProjectMemberDO member) {
        EmployeeDO employee = employeeMapper.selectByIdIncludingDeleted(member.getEmployeeId());
        PositionDO position = employee == null || employee.getPositionId() == null
                ? null : positionMapper.selectById(employee.getPositionId());
        return toVO(member,
                employee == null ? Map.of() : Map.of(employee.getId(), employee),
                position == null ? Map.of() : Map.of(position.getId(), position));
    }

    private ProjectMemberVO toVO(ProjectMemberDO member,
                                 Map<Long, EmployeeDO> employees,
        Map<Long, PositionDO> positions) {
        EmployeeDO employee = employees.get(member.getEmployeeId());
        PositionDO position = employee == null || employee.getPositionId() == null
                ? null : positions.get(employee.getPositionId());
        ProjectMemberVO vo = new ProjectMemberVO();
        vo.setId(member.getId());
        vo.setProjectId(member.getProjectId());
        vo.setEmployeeId(member.getEmployeeId());
        vo.setEmployeeName(employee == null ? "已删除员工" : employee.getName());
        vo.setPositionId(employee == null ? null : employee.getPositionId());
        vo.setPositionName(position == null ? null : position.getName());
        vo.setEmployeeStatus(employee == null ? "deleted" : employee.getStatus());
        vo.setRoleName(member.getRoleName());
        vo.setStatus(member.getStatus());
        vo.setRemark(member.getRemark());
        vo.setCreatedAt(member.getCreatedAt());
        vo.setJoinedAt(member.getCreatedAt());
        return vo;
    }
}

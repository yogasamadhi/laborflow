package com.company.labor.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.module.project.domain.entity.DailyWorkReportDO;
import com.company.labor.module.project.domain.entity.ProjectDO;
import com.company.labor.module.project.domain.entity.TeamSettlementDO;
import com.company.labor.module.project.domain.entity.VariationOrderDO;
import com.company.labor.module.project.mapper.DailyWorkReportMapper;
import com.company.labor.module.project.mapper.ProjectMapper;
import com.company.labor.module.project.mapper.TeamSettlementMapper;
import com.company.labor.module.project.mapper.VariationOrderMapper;
import com.company.labor.module.project.service.ProjectAccessService;
import com.company.labor.module.employee.domain.entity.DepartmentDO;
import com.company.labor.module.employee.mapper.DepartmentMapper;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import com.company.labor.module.salary.mapper.SalaryMonthlyMapper;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.domain.vo.TodoVO;
import com.company.labor.module.system.mapper.SysUserMapper;
import com.company.labor.security.AccessControlSupport;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class TodoService {
    private final SalaryMonthlyMapper salaryMonthlyMapper;
    private final DailyWorkReportMapper dailyReportMapper;
    private final VariationOrderMapper variationOrderMapper;
    private final TeamSettlementMapper settlementMapper;
    private final ProjectMapper projectMapper;
    private final SysUserMapper userMapper;
    private final ProjectAccessService projectAccessService;
    private final DepartmentMapper departmentMapper;

    public Page<TodoVO> page(long page, long pageSize, String type, Authentication authentication) {
        List<TodoVO> rows = new ArrayList<>();
        if (AccessControlSupport.hasAnyRole(authentication, "ADMIN", "BOSS")
                && (type == null || "salary".equals(type))) {
            for (SalaryMonthlyDO monthly : salaryMonthlyMapper.selectList(new LambdaQueryWrapper<SalaryMonthlyDO>()
                    .eq(SalaryMonthlyDO::getDeleted, 0)
                    .eq(SalaryMonthlyDO::getStatus, "pending_approval"))) {
                DepartmentDO department = monthly.getDepartmentId() == null ? null : departmentMapper.selectById(monthly.getDepartmentId());
                String departmentName = department == null ? "历史全公司" : department.getName();
                TodoVO todo = base("salary", monthly.getId(),
                        monthly.getTitle(), departmentName + "待审核工资表", monthly.getStatus(),
                        monthly.getCreatedBy(), monthly.getCreatedAt(), List.of("approve", "reject"));
                todo.setDepartmentId(monthly.getDepartmentId());
                todo.setDepartmentName(departmentName);
                rows.add(todo);
            }
        }
        if (AccessControlSupport.hasProjectManageRole(authentication)) {
            Set<Long> projectIds = authorizedProjects(authentication);
            if (type == null || "report_pm".equals(type) || "report_final".equals(type)) {
                for (DailyWorkReportDO report : dailyReportMapper.selectList(new LambdaQueryWrapper<DailyWorkReportDO>()
                        .eq(DailyWorkReportDO::getDeleted, 0)
                        .in(DailyWorkReportDO::getStatus, List.of("submitted", "pm_confirmed"))
                        .in(projectIds != null, DailyWorkReportDO::getProjectId, projectIds))) {
                    String todoType = "submitted".equals(report.getStatus()) ? "report_pm" : "report_final";
                    if (type != null && !type.equals(todoType)) continue;
                    TodoVO todo = base(todoType, report.getId(),
                            "工程量日报 " + report.getWorkDate(),
                            "submitted".equals(report.getStatus()) ? "待项目经理确认" : "待商务终审",
                            report.getStatus(), report.getCreatedBy(), report.getCreatedAt(),
                            List.of("approve", "reject"));
                    todo.setProjectId(report.getProjectId());
                    rows.add(todo);
                }
            }
            if (type == null || "variation".equals(type)) {
                for (VariationOrderDO order : variationOrderMapper.selectList(new LambdaQueryWrapper<VariationOrderDO>()
                        .eq(VariationOrderDO::getDeleted, 0)
                        .eq(VariationOrderDO::getStatus, "submitted")
                        .in(projectIds != null, VariationOrderDO::getProjectId, projectIds))) {
                    TodoVO todo = base("variation", order.getId(),
                            "签证：" + order.getVariationType(), "待确认签证金额",
                            order.getStatus(), order.getCreatedBy(), order.getCreatedAt(),
                            List.of("approve", "reject"));
                    todo.setProjectId(order.getProjectId());
                    rows.add(todo);
                }
            }
        }
        if (AccessControlSupport.hasProjectFinanceRole(authentication)
                && (type == null || "settlement".equals(type))) {
            Set<Long> projectIds = authorizedProjects(authentication);
            for (TeamSettlementDO settlement : settlementMapper.selectList(new LambdaQueryWrapper<TeamSettlementDO>()
                    .eq(TeamSettlementDO::getDeleted, 0)
                    .eq(TeamSettlementDO::getSettlementStatus, "draft")
                    .in(projectIds != null, TeamSettlementDO::getProjectId, projectIds))) {
                TodoVO todo = base("settlement", settlement.getId(),
                        "班组结算：" + settlement.getTeamName(),
                        settlement.getSettlementMonth() + " 待审核结算",
                        settlement.getSettlementStatus(), settlement.getCreatedBy(), settlement.getCreatedAt(),
                        List.of("approve", "reject"));
                todo.setProjectId(settlement.getProjectId());
                rows.add(todo);
            }
        }
        enrichNames(rows);
        rows.sort(Comparator.comparing(TodoVO::getOccurredAt,
                Comparator.nullsLast(Comparator.reverseOrder())));
        long current = Math.max(page, 1);
        long size = Math.max(Math.min(pageSize, 100), 1);
        int from = (int) Math.min((current - 1) * size, rows.size());
        int to = (int) Math.min(from + size, rows.size());
        Page<TodoVO> result = new Page<>(current, size, rows.size());
        result.setRecords(rows.subList(from, to));
        return result;
    }

    private Set<Long> authorizedProjects(Authentication authentication) {
        return projectAccessService.hasProjectWideAccess(authentication)
                ? null : projectAccessService.authorizedProjectIds(authentication);
    }

    private TodoVO base(String type, Long businessId, String title, String description,
                        String status, Long applicantId, LocalDateTime occurredAt, List<String> actions) {
        TodoVO todo = new TodoVO();
        todo.setId(type + "-" + businessId);
        todo.setType(type);
        todo.setBusinessId(businessId);
        todo.setTitle(title);
        todo.setDescription(description);
        todo.setStatus(status);
        todo.setApplicantId(applicantId);
        todo.setOccurredAt(occurredAt);
        todo.setActions(actions);
        return todo;
    }

    private void enrichNames(List<TodoVO> rows) {
        List<Long> projectIds = rows.stream().map(TodoVO::getProjectId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, ProjectDO> projects = projectIds.isEmpty() ? Map.of() : projectMapper.selectBatchIds(projectIds)
                .stream().collect(Collectors.toMap(ProjectDO::getId, Function.identity()));
        List<Long> userIds = rows.stream().map(TodoVO::getApplicantId).filter(java.util.Objects::nonNull).distinct().toList();
        Map<Long, SysUserDO> users = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(SysUserDO::getId, Function.identity()));
        for (TodoVO row : rows) {
            ProjectDO project = projects.get(row.getProjectId());
            row.setProjectName(project == null ? null : project.getName());
            SysUserDO user = users.get(row.getApplicantId());
            row.setApplicantName(user == null ? null
                    : (user.getNickname() == null ? user.getUsername() : user.getNickname()));
        }
    }
}

package com.company.labor.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.labor.module.compensation.domain.entity.PerformancePlanDO;
import com.company.labor.module.compensation.domain.entity.PerformanceScoreDO;
import com.company.labor.module.compensation.domain.entity.ProjectBonusPoolDO;
import com.company.labor.module.compensation.mapper.PerformancePlanMapper;
import com.company.labor.module.compensation.mapper.PerformanceScoreMapper;
import com.company.labor.module.compensation.mapper.ProjectBonusPoolMapper;
import com.company.labor.module.compensation.service.CompensationService;
import com.company.labor.module.project.domain.entity.ProjectDO;
import com.company.labor.module.project.domain.vo.ProjectDashboardVO;
import com.company.labor.module.project.mapper.ProjectMapper;
import com.company.labor.module.project.service.ProjectAccessService;
import com.company.labor.module.project.service.ProjectManagementService;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDetailDO;
import com.company.labor.module.salary.domain.entity.SalaryProfileDO;
import com.company.labor.module.salary.domain.vo.SalaryOverviewVO;
import com.company.labor.module.salary.mapper.SalaryMonthlyDetailMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyMapper;
import com.company.labor.module.salary.mapper.SalaryProfileMapper;
import com.company.labor.module.system.domain.vo.DashboardOverviewVO;
import com.company.labor.security.AccessControlSupport;
import java.math.BigDecimal;
import java.time.YearMonth;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DashboardOverviewService {
    private final SalaryMonthlyMapper salaryMonthlyMapper;
    private final SalaryMonthlyDetailMapper salaryDetailMapper;
    private final ProjectMapper projectMapper;
    private final ProjectAccessService projectAccessService;
    private final ProjectManagementService projectManagementService;
    private final CompensationService compensationService;
    private final SalaryProfileMapper salaryProfileMapper;
    private final PerformancePlanMapper performancePlanMapper;
    private final PerformanceScoreMapper performanceScoreMapper;
    private final ProjectBonusPoolMapper bonusPoolMapper;

    public DashboardOverviewVO overview(String requestedMonth, Authentication authentication) {
        String month = StringUtils.hasText(requestedMonth) ? requestedMonth : YearMonth.now().toString();
        try {
            YearMonth.parse(month);
        } catch (Exception ex) {
            throw new com.company.labor.common.exception.BusinessException("月份格式必须为 yyyy-MM");
        }
        boolean salary = AccessControlSupport.hasSalaryManageRole(authentication);
        boolean projects = AccessControlSupport.hasProjectFinanceRole(authentication);
        boolean compensation = AccessControlSupport.hasAnyRole(authentication, "ADMIN", "BOSS", "MANAGER", "FINANCE");

        DashboardOverviewVO result = new DashboardOverviewVO();
        result.setMonth(month);
        DashboardOverviewVO.CapabilitiesVO capabilities = new DashboardOverviewVO.CapabilitiesVO();
        capabilities.setSalary(salary);
        capabilities.setProjects(projects);
        capabilities.setCompensation(compensation);
        capabilities.setEmployeeHandoff(!salary && !projects && !compensation);
        result.setCapabilities(capabilities);
        if (salary) result.setSalary(salaryOverview(month));
        if (projects) result.setProjects(projectPortfolio(authentication));
        if (compensation) result.setCompensation(compensationOverview(month));
        if (capabilities.isEmployeeHandoff()) {
            DashboardOverviewVO.EmployeeHandoffVO handoff = new DashboardOverviewVO.EmployeeHandoffVO();
            handoff.setEmployeeBound(AccessControlSupport.currentEmployeeId(authentication) != null);
            handoff.setMessage("请使用 LaborFlow 微信小程序办理工资条、日报和签证业务");
            result.setEmployee(handoff);
        }
        return result;
    }

    private SalaryOverviewVO salaryOverview(String month) {
        List<SalaryMonthlyDO> monthlies = salaryMonthlyMapper.selectList(new LambdaQueryWrapper<SalaryMonthlyDO>()
                .eq(SalaryMonthlyDO::getDeleted, 0)
                .eq(SalaryMonthlyDO::getSalaryMonth, month));
        if (monthlies.isEmpty()) {
            return new SalaryOverviewVO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO,
                    BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);
        }
        List<Long> monthlyIds = monthlies.stream().map(SalaryMonthlyDO::getId).toList();
        List<SalaryMonthlyDetailDO> details = salaryDetailMapper.selectList(new LambdaQueryWrapper<SalaryMonthlyDetailDO>()
                .eq(SalaryMonthlyDetailDO::getDeleted, 0)
                .in(SalaryMonthlyDetailDO::getSalaryMonthlyId, monthlyIds));
        BigDecimal payable = monthlies.stream().map(SalaryMonthlyDO::getTotalPayableAmount).map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal deduction = monthlies.stream().map(SalaryMonthlyDO::getTotalDeductionAmount).map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal actual = monthlies.stream().map(SalaryMonthlyDO::getTotalActualAmount).map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal paid = details.stream().filter(item -> "paid".equals(item.getPaymentStatus()))
                .map(SalaryMonthlyDetailDO::getActualAmount).map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        long paidCount = details.stream().filter(item -> "paid".equals(item.getPaymentStatus())).count();
        return new SalaryOverviewVO(
                payable,
                deduction,
                actual,
                paid,
                actual.subtract(paid),
                paidCount,
                details.size() - paidCount);
    }

    private DashboardOverviewVO.ProjectPortfolioVO projectPortfolio(Authentication authentication) {
        Set<Long> scopedIds = projectAccessService.hasProjectWideAccess(authentication)
                ? null : projectAccessService.authorizedProjectIds(authentication);
        List<ProjectDO> projects = scopedIds != null && scopedIds.isEmpty() ? List.of()
                : projectMapper.selectList(new LambdaQueryWrapper<ProjectDO>()
                        .eq(ProjectDO::getDeleted, 0)
                        .in(scopedIds != null, ProjectDO::getId, scopedIds));
        DashboardOverviewVO.ProjectPortfolioVO vo = new DashboardOverviewVO.ProjectPortfolioVO();
        vo.setProjectCount(projects.size());
        vo.setInProgressCount(projects.stream().filter(item -> "in_progress".equals(item.getStatus())).count());
        BigDecimal contract = BigDecimal.ZERO;
        BigDecimal revenue = BigDecimal.ZERO;
        BigDecimal cost = BigDecimal.ZERO;
        BigDecimal profit = BigDecimal.ZERO;
        BigDecimal receivable = BigDecimal.ZERO;
        BigDecimal received = BigDecimal.ZERO;
        long risks = 0;
        for (ProjectDO project : projects) {
            ProjectDashboardVO dashboard = projectManagementService.dashboard(project.getId());
            contract = contract.add(nvl(dashboard.getContractAmount()));
            revenue = revenue.add(nvl(dashboard.getRevenueAmount()));
            cost = cost.add(nvl(dashboard.getTotalCostAmount()));
            profit = profit.add(nvl(dashboard.getGrossProfit()));
            receivable = receivable.add(nvl(dashboard.getReceivableAmount()));
            received = received.add(nvl(dashboard.getReceivedAmount()));
            risks += dashboard.getRisks().size();
        }
        vo.setContractAmount(contract);
        vo.setRevenueAmount(revenue);
        vo.setTotalCostAmount(cost);
        vo.setGrossProfit(profit);
        vo.setReceivableAmount(receivable);
        vo.setReceivedAmount(received);
        vo.setRiskCount(risks);
        return vo;
    }

    private DashboardOverviewVO.CompensationOverviewVO compensationOverview(String month) {
        DashboardOverviewVO.CompensationOverviewVO vo = new DashboardOverviewVO.CompensationOverviewVO();
        vo.setTotals(compensationService.dashboard(month));
        vo.setActiveProfileCount(salaryProfileMapper.selectCount(new LambdaQueryWrapper<SalaryProfileDO>()
                .eq(SalaryProfileDO::getDeleted, 0).eq(SalaryProfileDO::getStatus, "active")));
        vo.setPendingPerformancePlanCount(performancePlanMapper.selectCount(new LambdaQueryWrapper<PerformancePlanDO>()
                .eq(PerformancePlanDO::getDeleted, 0)
                .eq(PerformancePlanDO::getPerformanceMonth, month)
                .eq(PerformancePlanDO::getStatus, "draft")));
        vo.setDraftPerformanceScoreCount(performanceScoreMapper.selectCount(new LambdaQueryWrapper<PerformanceScoreDO>()
                .eq(PerformanceScoreDO::getDeleted, 0)
                .eq(PerformanceScoreDO::getPerformanceMonth, month)
                .eq(PerformanceScoreDO::getStatus, "draft")));
        vo.setDraftBonusPoolCount(bonusPoolMapper.selectCount(new LambdaQueryWrapper<ProjectBonusPoolDO>()
                .eq(ProjectBonusPoolDO::getDeleted, 0)
                .eq(ProjectBonusPoolDO::getBonusMonth, month)
                .eq(ProjectBonusPoolDO::getReleaseStatus, "draft")));
        return vo;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

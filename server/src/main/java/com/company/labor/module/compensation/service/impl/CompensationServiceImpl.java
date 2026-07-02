package com.company.labor.module.compensation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.statemachine.BusinessStateMachine;
import com.company.labor.common.statemachine.BusinessStateMachine.Flow;
import com.company.labor.module.compensation.domain.entity.AttendanceRecordDO;
import com.company.labor.module.compensation.domain.entity.PerformanceIndicatorDO;
import com.company.labor.module.compensation.domain.entity.PerformancePlanDO;
import com.company.labor.module.compensation.domain.entity.PerformanceScoreDO;
import com.company.labor.module.compensation.domain.entity.ProjectBonusAllocationDO;
import com.company.labor.module.compensation.domain.entity.ProjectBonusPoolDO;
import com.company.labor.module.compensation.domain.entity.SalaryDeductionRecordDO;
import com.company.labor.module.compensation.domain.vo.CompensationDashboardVO;
import com.company.labor.module.compensation.mapper.AttendanceRecordMapper;
import com.company.labor.module.compensation.mapper.PerformanceIndicatorMapper;
import com.company.labor.module.compensation.mapper.PerformancePlanMapper;
import com.company.labor.module.compensation.mapper.PerformanceScoreMapper;
import com.company.labor.module.compensation.mapper.ProjectBonusAllocationMapper;
import com.company.labor.module.compensation.mapper.ProjectBonusPoolMapper;
import com.company.labor.module.compensation.mapper.SalaryDeductionRecordMapper;
import com.company.labor.module.compensation.service.CompensationService;
import com.company.labor.module.project.domain.vo.ProjectDashboardVO;
import com.company.labor.module.project.service.ProjectManagementService;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import com.company.labor.module.salary.mapper.SalaryMonthlyMapper;
import com.company.labor.module.system.service.OperationLogService;
import java.math.BigDecimal;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class CompensationServiceImpl implements CompensationService {
    private final AttendanceRecordMapper attendanceMapper;
    private final PerformanceIndicatorMapper indicatorMapper;
    private final PerformancePlanMapper planMapper;
    private final PerformanceScoreMapper scoreMapper;
    private final ProjectBonusPoolMapper bonusPoolMapper;
    private final ProjectBonusAllocationMapper bonusAllocationMapper;
    private final SalaryDeductionRecordMapper salaryDeductionMapper;
    private final ProjectManagementService projectManagementService;
    private final SalaryMonthlyMapper salaryMonthlyMapper;
    private final OperationLogService operationLogService;

    @Override
    public Page<AttendanceRecordDO> attendanceRecords(long page, long pageSize, Long employeeId, String month) {
        return attendanceMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<AttendanceRecordDO>()
                .eq(AttendanceRecordDO::getDeleted, 0)
                .eq(employeeId != null, AttendanceRecordDO::getEmployeeId, employeeId)
                .eq(StringUtils.hasText(month), AttendanceRecordDO::getAttendanceMonth, month)
                .orderByDesc(AttendanceRecordDO::getAttendanceMonth)
                .orderByDesc(AttendanceRecordDO::getCreatedAt));
    }

    @Override
    public AttendanceRecordDO saveAttendance(AttendanceRecordDO record) {
        normalizeAttendance(record);
        record.setDeleted(0);
        attendanceMapper.insert(record);
        return record;
    }

    @Override
    public AttendanceRecordDO updateAttendance(Long id, AttendanceRecordDO record) {
        AttendanceRecordDO old = requireAttendance(id);
        record.setId(id);
        record.setEmployeeId(old.getEmployeeId());
        normalizeAttendance(record);
        attendanceMapper.updateById(record);
        return attendanceMapper.selectById(id);
    }

    @Override
    public Page<SalaryDeductionRecordDO> salaryDeductions(long page, long pageSize, Long employeeId, String month, String type) {
        return salaryDeductionMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<SalaryDeductionRecordDO>()
                .eq(SalaryDeductionRecordDO::getDeleted, 0)
                .eq(employeeId != null, SalaryDeductionRecordDO::getEmployeeId, employeeId)
                .eq(StringUtils.hasText(month), SalaryDeductionRecordDO::getDeductionMonth, month)
                .eq(StringUtils.hasText(type), SalaryDeductionRecordDO::getDeductionType, type)
                .orderByDesc(SalaryDeductionRecordDO::getDeductionMonth)
                .orderByDesc(SalaryDeductionRecordDO::getCreatedAt));
    }

    @Override
    public SalaryDeductionRecordDO saveSalaryDeduction(SalaryDeductionRecordDO record) {
        normalizeSalaryDeduction(record);
        record.setDeleted(0);
        salaryDeductionMapper.insert(record);
        operationLogService.record("compensation", record.getId(), "save_salary_deduction", "保存工资扣款：" + record.getSalaryItemName());
        return record;
    }

    @Override
    public SalaryDeductionRecordDO updateSalaryDeduction(Long id, SalaryDeductionRecordDO record) {
        requireSalaryDeduction(id);
        record.setId(id);
        normalizeSalaryDeduction(record);
        salaryDeductionMapper.updateById(record);
        operationLogService.record("compensation", id, "update_salary_deduction", "更新工资扣款：" + record.getSalaryItemName());
        return salaryDeductionMapper.selectById(id);
    }

    @Override
    public void deleteSalaryDeduction(Long id) {
        SalaryDeductionRecordDO record = requireSalaryDeduction(id);
        record.setDeleted(1);
        salaryDeductionMapper.updateById(record);
        operationLogService.record("compensation", id, "delete_salary_deduction", "删除工资扣款：" + record.getSalaryItemName());
    }

    @Override
    public Page<PerformanceIndicatorDO> indicators(long page, long pageSize, Long positionId, String status) {
        return indicatorMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<PerformanceIndicatorDO>()
                .eq(PerformanceIndicatorDO::getDeleted, 0)
                .eq(positionId != null, PerformanceIndicatorDO::getPositionId, positionId)
                .eq(StringUtils.hasText(status), PerformanceIndicatorDO::getStatus, status)
                .orderByDesc(PerformanceIndicatorDO::getCreatedAt));
    }

    @Override
    public PerformanceIndicatorDO saveIndicator(PerformanceIndicatorDO indicator) {
        requireText(indicator.getName(), "指标名称必填");
        indicator.setScoreWeight(nvl(indicator.getScoreWeight()));
        indicator.setDataSource(defaultText(indicator.getDataSource(), "manual"));
        indicator.setStatus(defaultText(indicator.getStatus(), "active"));
        indicator.setDeleted(0);
        indicatorMapper.insert(indicator);
        return indicator;
    }

    @Override
    public PerformanceIndicatorDO updateIndicator(Long id, PerformanceIndicatorDO indicator) {
        requireIndicator(id);
        indicator.setId(id);
        indicatorMapper.updateById(indicator);
        return indicatorMapper.selectById(id);
    }

    @Override
    public Page<PerformancePlanDO> plans(long page, long pageSize, String month, Long projectId) {
        return planMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<PerformancePlanDO>()
                .eq(PerformancePlanDO::getDeleted, 0)
                .eq(StringUtils.hasText(month), PerformancePlanDO::getPerformanceMonth, month)
                .eq(projectId != null, PerformancePlanDO::getProjectId, projectId)
                .orderByDesc(PerformancePlanDO::getPerformanceMonth)
                .orderByDesc(PerformancePlanDO::getCreatedAt));
    }

    @Override
    public PerformancePlanDO savePlan(PerformancePlanDO plan) {
        requireText(plan.getName(), "绩效方案名称必填");
        requireText(plan.getPerformanceMonth(), "绩效月份必填");
        plan.setStatus(defaultText(plan.getStatus(), "draft"));
        plan.setDeleted(0);
        planMapper.insert(plan);
        return plan;
    }

    @Override
    public PerformancePlanDO updatePlan(Long id, PerformancePlanDO plan) {
        requirePlan(id);
        plan.setId(id);
        planMapper.updateById(plan);
        return planMapper.selectById(id);
    }

    @Override
    public Page<PerformanceScoreDO> scores(long page, long pageSize, String month, Long employeeId, Long projectId) {
        return scoreMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<PerformanceScoreDO>()
                .eq(PerformanceScoreDO::getDeleted, 0)
                .eq(StringUtils.hasText(month), PerformanceScoreDO::getPerformanceMonth, month)
                .eq(employeeId != null, PerformanceScoreDO::getEmployeeId, employeeId)
                .eq(projectId != null, PerformanceScoreDO::getProjectId, projectId)
                .orderByDesc(PerformanceScoreDO::getPerformanceMonth)
                .orderByDesc(PerformanceScoreDO::getCreatedAt));
    }

    @Override
    public PerformanceScoreDO saveScore(PerformanceScoreDO score) {
        normalizeScore(score);
        score.setStatus("draft");
        score.setDeleted(0);
        scoreMapper.insert(score);
        return score;
    }

    @Override
    public PerformanceScoreDO updateScore(Long id, PerformanceScoreDO score) {
        PerformanceScoreDO old = requireScore(id);
        BusinessStateMachine.requireCurrent(Flow.PERFORMANCE_SCORE, old.getStatus(), "draft");
        score.setId(id);
        score.setEmployeeId(old.getEmployeeId());
        score.setStatus(old.getStatus());
        normalizeScore(score);
        scoreMapper.updateById(score);
        return scoreMapper.selectById(id);
    }

    @Override
    public void confirmScore(Long id) {
        PerformanceScoreDO score = requireScore(id);
        String statusBefore = score.getStatus();
        BusinessStateMachine.requireTransition(Flow.PERFORMANCE_SCORE, statusBefore, "confirmed");
        score.setStatus("confirmed");
        scoreMapper.updateById(score);
        operationLogService.record("compensation", id, "confirm_score", "确认绩效评分：" + statusBefore + " -> confirmed");
    }

    @Override
    public Page<ProjectBonusPoolDO> bonusPools(long page, long pageSize, Long projectId, String status) {
        return bonusPoolMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<ProjectBonusPoolDO>()
                .eq(ProjectBonusPoolDO::getDeleted, 0)
                .eq(projectId != null, ProjectBonusPoolDO::getProjectId, projectId)
                .eq(StringUtils.hasText(status), ProjectBonusPoolDO::getReleaseStatus, status)
                .orderByDesc(ProjectBonusPoolDO::getCreatedAt));
    }

    @Override
    public ProjectBonusPoolDO saveBonusPool(ProjectBonusPoolDO pool) {
        normalizeBonusPool(pool);
        pool.setReleaseStatus("draft");
        pool.setDeleted(0);
        bonusPoolMapper.insert(pool);
        return pool;
    }

    @Override
    public ProjectBonusPoolDO updateBonusPool(Long id, ProjectBonusPoolDO pool) {
        ProjectBonusPoolDO old = requireBonusPool(id);
        BusinessStateMachine.requireCurrent(Flow.PROJECT_BONUS_POOL, old.getReleaseStatus(), "draft");
        pool.setId(id);
        pool.setReleaseStatus(old.getReleaseStatus());
        if (StringUtils.hasText(old.getDashboardSnapshot())) {
            pool.setDashboardSnapshot(old.getDashboardSnapshot());
        }
        normalizeBonusPool(pool);
        bonusPoolMapper.updateById(pool);
        return bonusPoolMapper.selectById(id);
    }

    @Override
    public void releaseBonusPool(Long id) {
        ProjectBonusPoolDO pool = requireBonusPool(id);
        String statusBefore = pool.getReleaseStatus();
        BusinessStateMachine.requireTransition(Flow.PROJECT_BONUS_POOL, statusBefore, "released");
        pool.setReleaseStatus("released");
        bonusPoolMapper.updateById(pool);
        operationLogService.record("compensation", id, "release_bonus_pool", "发布项目奖金池：" + statusBefore + " -> released");
    }

    @Override
    public Page<ProjectBonusAllocationDO> bonusAllocations(long page, long pageSize, Long poolId, String paymentMonth) {
        return bonusAllocationMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<ProjectBonusAllocationDO>()
                .eq(ProjectBonusAllocationDO::getDeleted, 0)
                .eq(poolId != null, ProjectBonusAllocationDO::getBonusPoolId, poolId)
                .eq(StringUtils.hasText(paymentMonth), ProjectBonusAllocationDO::getPaymentMonth, paymentMonth)
                .orderByDesc(ProjectBonusAllocationDO::getPaymentMonth)
                .orderByDesc(ProjectBonusAllocationDO::getCreatedAt));
    }

    @Override
    public ProjectBonusAllocationDO saveBonusAllocation(ProjectBonusAllocationDO allocation) {
        normalizeBonusAllocation(allocation);
        allocation.setStatus("allocated");
        allocation.setDeleted(0);
        bonusAllocationMapper.insert(allocation);
        return allocation;
    }

    @Override
    public ProjectBonusAllocationDO updateBonusAllocation(Long id, ProjectBonusAllocationDO allocation) {
        ProjectBonusAllocationDO old = requireBonusAllocation(id);
        BusinessStateMachine.requireCurrent(Flow.PROJECT_BONUS_ALLOCATION, old.getStatus(), "allocated");
        allocation.setId(id);
        allocation.setStatus(old.getStatus());
        normalizeBonusAllocation(allocation);
        bonusAllocationMapper.updateById(allocation);
        return bonusAllocationMapper.selectById(id);
    }

    @Override
    public void confirmBonusAllocation(Long id) {
        ProjectBonusAllocationDO allocation = requireBonusAllocation(id);
        String statusBefore = allocation.getStatus();
        BusinessStateMachine.requireTransition(Flow.PROJECT_BONUS_ALLOCATION, statusBefore, "confirmed");
        allocation.setStatus("confirmed");
        bonusAllocationMapper.updateById(allocation);
        operationLogService.record("compensation", id, "confirm_bonus_allocation", "确认奖金分配：" + statusBefore + " -> confirmed");
    }

    @Override
    public CompensationDashboardVO dashboard(String month) {
        CompensationDashboardVO vo = new CompensationDashboardVO();
        vo.setMonth(month);
        BigDecimal attendanceDeduction = BigDecimal.ZERO;
        for (AttendanceRecordDO row : attendanceMapper.selectList(new LambdaQueryWrapper<AttendanceRecordDO>()
                .eq(AttendanceRecordDO::getDeleted, 0)
                .eq(StringUtils.hasText(month), AttendanceRecordDO::getAttendanceMonth, month))) {
            attendanceDeduction = attendanceDeduction.add(nvl(row.getDeductionAmount()));
        }
        BigDecimal salaryDeduction = BigDecimal.ZERO;
        for (SalaryDeductionRecordDO row : salaryDeductionMapper.selectList(new LambdaQueryWrapper<SalaryDeductionRecordDO>()
                .eq(SalaryDeductionRecordDO::getDeleted, 0)
                .eq(SalaryDeductionRecordDO::getStatus, "active")
                .eq(StringUtils.hasText(month), SalaryDeductionRecordDO::getDeductionMonth, month))) {
            salaryDeduction = salaryDeduction.add(nvl(row.getAmount()));
        }
        BigDecimal performanceAmount = BigDecimal.ZERO;
        long confirmedScoreCount = 0;
        for (PerformanceScoreDO row : scoreMapper.selectList(new LambdaQueryWrapper<PerformanceScoreDO>()
                .eq(PerformanceScoreDO::getDeleted, 0)
                .eq(StringUtils.hasText(month), PerformanceScoreDO::getPerformanceMonth, month))) {
            performanceAmount = performanceAmount.add(nvl(row.getPerformanceAmount()));
            if ("confirmed".equals(row.getStatus())) confirmedScoreCount++;
        }
        BigDecimal projectBonus = BigDecimal.ZERO;
        for (ProjectBonusAllocationDO row : bonusAllocationMapper.selectList(new LambdaQueryWrapper<ProjectBonusAllocationDO>()
                .eq(ProjectBonusAllocationDO::getDeleted, 0)
                .eq(StringUtils.hasText(month), ProjectBonusAllocationDO::getPaymentMonth, month))) {
            projectBonus = projectBonus.add(nvl(row.getAllocationAmount()));
        }
        BigDecimal actualSalary = BigDecimal.ZERO;
        for (SalaryMonthlyDO row : salaryMonthlyMapper.selectList(new LambdaQueryWrapper<SalaryMonthlyDO>()
                .eq(SalaryMonthlyDO::getDeleted, 0)
                .eq(StringUtils.hasText(month), SalaryMonthlyDO::getSalaryMonth, month))) {
            actualSalary = actualSalary.add(nvl(row.getTotalActualAmount()));
        }
        vo.setAttendanceDeductionAmount(attendanceDeduction);
        vo.setSalaryDeductionAmount(salaryDeduction);
        vo.setPerformanceAmount(performanceAmount);
        vo.setProjectBonusAmount(projectBonus);
        vo.setSalaryActualAmount(actualSalary);
        vo.setScoredEmployeeCount(scoreMapper.selectCount(new LambdaQueryWrapper<PerformanceScoreDO>().eq(PerformanceScoreDO::getDeleted, 0).eq(StringUtils.hasText(month), PerformanceScoreDO::getPerformanceMonth, month)));
        vo.setConfirmedScoreCount(confirmedScoreCount);
        vo.setBonusAllocationCount(bonusAllocationMapper.selectCount(new LambdaQueryWrapper<ProjectBonusAllocationDO>().eq(ProjectBonusAllocationDO::getDeleted, 0).eq(StringUtils.hasText(month), ProjectBonusAllocationDO::getPaymentMonth, month)));
        return vo;
    }

    private void normalizeAttendance(AttendanceRecordDO record) {
        if (record.getEmployeeId() == null) throw new BusinessException("员工必填");
        requireText(record.getAttendanceMonth(), "考勤月份必填");
        record.setWorkDays(nvl(record.getWorkDays()));
        record.setLeaveDays(nvl(record.getLeaveDays()));
        record.setOvertimeDays(nvl(record.getOvertimeDays()));
        record.setAbsentDays(nvl(record.getAbsentDays()));
        record.setFieldDays(nvl(record.getFieldDays()));
        record.setProjectSiteDays(nvl(record.getProjectSiteDays()));
        if (record.getDeductionAmount() == null || record.getDeductionAmount().compareTo(BigDecimal.ZERO) == 0) {
            record.setDeductionAmount(record.getLeaveDays().multiply(BigDecimal.valueOf(100)).add(record.getAbsentDays().multiply(BigDecimal.valueOf(200))));
        }
    }

    private void normalizeSalaryDeduction(SalaryDeductionRecordDO record) {
        if (record.getEmployeeId() == null) throw new BusinessException("员工必填");
        requireText(record.getDeductionMonth(), "扣款月份必填");
        record.setDeductionType(defaultText(record.getDeductionType(), "other"));
        record.setAmount(nvl(record.getAmount()));
        record.setStatus(defaultText(record.getStatus(), "active"));
        record.setSalaryItemId(defaultSalaryItemId(record));
        record.setSalaryItemName(defaultText(record.getSalaryItemName(), defaultSalaryItemName(record.getDeductionType())));
    }

    private void normalizeScore(PerformanceScoreDO score) {
        if (score.getEmployeeId() == null) throw new BusinessException("员工必填");
        requireText(score.getPerformanceMonth(), "绩效月份必填");
        score.setScore(nvl(score.getScore()));
        score.setPerformanceAmount(nvl(score.getPerformanceAmount()));
        if (score.getProjectId() != null) {
            ProjectDashboardVO dashboard = projectManagementService.dashboard(score.getProjectId());
            score.setAutoMetricSnapshot("项目毛利:" + dashboard.getGrossProfit() + ";回款进度:" + dashboard.getPaymentProgress() + "%;风险数:" + dashboard.getRisks().size());
        }
    }

    private void normalizeBonusPool(ProjectBonusPoolDO pool) {
        if (pool.getProjectId() == null) throw new BusinessException("项目必填");
        pool.setBonusRatio(nvl(pool.getBonusRatio()));
        ProjectDashboardVO dashboard = projectManagementService.dashboard(pool.getProjectId());
        if (pool.getFinalGrossProfit() == null || pool.getFinalGrossProfit().compareTo(BigDecimal.ZERO) == 0) {
            pool.setFinalGrossProfit(dashboard.getGrossProfit());
        }
        if (!StringUtils.hasText(pool.getDashboardSnapshot())) {
            pool.setDashboardSnapshot(bonusPoolSnapshot(dashboard));
        }
        pool.setBonusPoolAmount(pool.getFinalGrossProfit().multiply(pool.getBonusRatio()));
    }

    private String bonusPoolSnapshot(ProjectDashboardVO dashboard) {
        return "项目:" + dashboard.getProjectName()
                + ";合同额:" + dashboard.getContractAmount()
                + ";已完成产值:" + dashboard.getCompletedOutputValue()
                + ";总成本:" + dashboard.getTotalCostAmount()
                + ";毛利:" + dashboard.getGrossProfit()
                + ";应收:" + dashboard.getReceivableAmount()
                + ";已收:" + dashboard.getReceivedAmount()
                + ";风险数:" + dashboard.getRisks().size();
    }

    private void normalizeBonusAllocation(ProjectBonusAllocationDO allocation) {
        ProjectBonusPoolDO pool = requireBonusPool(allocation.getBonusPoolId());
        allocation.setProjectId(pool.getProjectId());
        if (allocation.getEmployeeId() == null) throw new BusinessException("员工必填");
        requireText(allocation.getPaymentMonth(), "发放月份必填");
        allocation.setAllocationRatio(nvl(allocation.getAllocationRatio()));
        if (allocation.getAllocationAmount() == null || allocation.getAllocationAmount().compareTo(BigDecimal.ZERO) == 0) {
            allocation.setAllocationAmount(nvl(pool.getBonusPoolAmount()).multiply(allocation.getAllocationRatio()));
        }
    }

    private AttendanceRecordDO requireAttendance(Long id) {
        AttendanceRecordDO row = attendanceMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("考勤记录不存在");
        return row;
    }

    private SalaryDeductionRecordDO requireSalaryDeduction(Long id) {
        SalaryDeductionRecordDO row = salaryDeductionMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("工资扣款不存在");
        return row;
    }

    private PerformanceIndicatorDO requireIndicator(Long id) {
        PerformanceIndicatorDO row = indicatorMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("绩效指标不存在");
        return row;
    }

    private PerformancePlanDO requirePlan(Long id) {
        PerformancePlanDO row = planMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("绩效方案不存在");
        return row;
    }

    private PerformanceScoreDO requireScore(Long id) {
        PerformanceScoreDO row = scoreMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("绩效评分不存在");
        return row;
    }

    private ProjectBonusPoolDO requireBonusPool(Long id) {
        ProjectBonusPoolDO row = bonusPoolMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("项目奖金池不存在");
        return row;
    }

    private ProjectBonusAllocationDO requireBonusAllocation(Long id) {
        ProjectBonusAllocationDO row = bonusAllocationMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("奖金分配不存在");
        return row;
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) throw new BusinessException(message);
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private Long defaultSalaryItemId(SalaryDeductionRecordDO record) {
        if (record.getSalaryItemId() != null) return record.getSalaryItemId();
        return switch (record.getDeductionType()) {
            case "social_security" -> 2102L;
            case "housing_fund" -> 2301L;
            case "individual_tax" -> 2302L;
            case "advance" -> 2103L;
            default -> 2105L;
        };
    }

    private String defaultSalaryItemName(String type) {
        return switch (type) {
            case "social_security" -> "社保扣款";
            case "housing_fund" -> "公积金扣款";
            case "individual_tax" -> "个税扣款";
            case "advance" -> "借支扣款";
            default -> "其他扣款";
        };
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }
}

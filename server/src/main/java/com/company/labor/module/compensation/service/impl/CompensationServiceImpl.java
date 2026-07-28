package com.company.labor.module.compensation.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.statemachine.BusinessStateMachine;
import com.company.labor.common.statemachine.BusinessStateMachine.Flow;
import com.company.labor.module.compensation.domain.dto.CompensationWriteDTO;
import com.company.labor.module.compensation.domain.entity.AttendanceRecordDO;
import com.company.labor.module.compensation.domain.entity.PerformanceIndicatorDO;
import com.company.labor.module.compensation.domain.entity.PerformancePlanIndicatorDO;
import com.company.labor.module.compensation.domain.entity.PerformancePlanDO;
import com.company.labor.module.compensation.domain.entity.PerformanceScoreDO;
import com.company.labor.module.compensation.domain.entity.PerformanceScoreItemDO;
import com.company.labor.module.compensation.domain.entity.ProjectBonusAllocationDO;
import com.company.labor.module.compensation.domain.entity.ProjectBonusPoolDO;
import com.company.labor.module.compensation.domain.entity.SalaryDeductionRecordDO;
import com.company.labor.module.compensation.domain.vo.CompensationDashboardVO;
import com.company.labor.module.compensation.domain.vo.PerformancePlanDetailVO;
import com.company.labor.module.compensation.domain.vo.PerformanceScoreDetailVO;
import com.company.labor.module.compensation.mapper.AttendanceRecordMapper;
import com.company.labor.module.compensation.mapper.PerformanceIndicatorMapper;
import com.company.labor.module.compensation.mapper.PerformancePlanMapper;
import com.company.labor.module.compensation.mapper.PerformancePlanIndicatorMapper;
import com.company.labor.module.compensation.mapper.PerformanceScoreMapper;
import com.company.labor.module.compensation.mapper.PerformanceScoreItemMapper;
import com.company.labor.module.compensation.mapper.ProjectBonusAllocationMapper;
import com.company.labor.module.compensation.mapper.ProjectBonusPoolMapper;
import com.company.labor.module.compensation.mapper.SalaryDeductionRecordMapper;
import com.company.labor.module.compensation.service.CompensationService;
import com.company.labor.module.employee.domain.entity.EmployeeDO;
import com.company.labor.module.employee.mapper.EmployeeMapper;
import com.company.labor.module.project.domain.vo.ProjectDashboardVO;
import com.company.labor.module.project.service.ProjectManagementService;
import com.company.labor.module.salary.domain.entity.SalaryItemDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDetailDO;
import com.company.labor.module.salary.domain.entity.SalaryProfileDO;
import com.company.labor.module.salary.domain.entity.SalaryProfileItemDO;
import com.company.labor.module.salary.mapper.SalaryItemMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyDetailMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyMapper;
import com.company.labor.module.salary.mapper.SalaryProfileItemMapper;
import com.company.labor.module.salary.mapper.SalaryProfileMapper;
import com.company.labor.module.system.service.OperationLogService;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class CompensationServiceImpl implements CompensationService {
    private static final Map<String, String> METRIC_SOURCES = Map.ofEntries(
            Map.entry("project_profit_rate", "project"),
            Map.entry("project_payment_progress", "project"),
            Map.entry("project_cost_rate", "project"),
            Map.entry("project_risk_count", "project"),
            Map.entry("attendance_work_days", "attendance"),
            Map.entry("attendance_absent_days", "attendance"),
            Map.entry("attendance_leave_days", "attendance"),
            Map.entry("attendance_project_site_days", "attendance"),
            Map.entry("salary_payment_completed", "salary"),
            Map.entry("salary_payslip_no_exception", "salary")
    );
    private final AttendanceRecordMapper attendanceMapper;
    private final PerformanceIndicatorMapper indicatorMapper;
    private final PerformancePlanMapper planMapper;
    private final PerformancePlanIndicatorMapper planIndicatorMapper;
    private final PerformanceScoreMapper scoreMapper;
    private final PerformanceScoreItemMapper scoreItemMapper;
    private final ProjectBonusPoolMapper bonusPoolMapper;
    private final ProjectBonusAllocationMapper bonusAllocationMapper;
    private final SalaryDeductionRecordMapper salaryDeductionMapper;
    private final ProjectManagementService projectManagementService;
    private final SalaryMonthlyMapper salaryMonthlyMapper;
    private final SalaryMonthlyDetailMapper salaryMonthlyDetailMapper;
    private final SalaryProfileMapper salaryProfileMapper;
    private final SalaryProfileItemMapper salaryProfileItemMapper;
    private final SalaryItemMapper salaryItemMapper;
    private final EmployeeMapper employeeMapper;
    private final OperationLogService operationLogService;

    @org.springframework.beans.factory.annotation.Autowired
    public CompensationServiceImpl(
            AttendanceRecordMapper attendanceMapper,
            PerformanceIndicatorMapper indicatorMapper,
            PerformancePlanMapper planMapper,
            PerformancePlanIndicatorMapper planIndicatorMapper,
            PerformanceScoreMapper scoreMapper,
            PerformanceScoreItemMapper scoreItemMapper,
            ProjectBonusPoolMapper bonusPoolMapper,
            ProjectBonusAllocationMapper bonusAllocationMapper,
            SalaryDeductionRecordMapper salaryDeductionMapper,
            ProjectManagementService projectManagementService,
            SalaryMonthlyMapper salaryMonthlyMapper,
            SalaryMonthlyDetailMapper salaryMonthlyDetailMapper,
            SalaryProfileMapper salaryProfileMapper,
            SalaryProfileItemMapper salaryProfileItemMapper,
            SalaryItemMapper salaryItemMapper,
            EmployeeMapper employeeMapper,
            OperationLogService operationLogService) {
        this.attendanceMapper = attendanceMapper;
        this.indicatorMapper = indicatorMapper;
        this.planMapper = planMapper;
        this.planIndicatorMapper = planIndicatorMapper;
        this.scoreMapper = scoreMapper;
        this.scoreItemMapper = scoreItemMapper;
        this.bonusPoolMapper = bonusPoolMapper;
        this.bonusAllocationMapper = bonusAllocationMapper;
        this.salaryDeductionMapper = salaryDeductionMapper;
        this.projectManagementService = projectManagementService;
        this.salaryMonthlyMapper = salaryMonthlyMapper;
        this.salaryMonthlyDetailMapper = salaryMonthlyDetailMapper;
        this.salaryProfileMapper = salaryProfileMapper;
        this.salaryProfileItemMapper = salaryProfileItemMapper;
        this.salaryItemMapper = salaryItemMapper;
        this.employeeMapper = employeeMapper;
        this.operationLogService = operationLogService;
    }

    /**
     * Compatibility constructor for bonus, attendance and deduction unit tests.
     */
    public CompensationServiceImpl(
            AttendanceRecordMapper attendanceMapper,
            PerformanceIndicatorMapper indicatorMapper,
            PerformancePlanMapper planMapper,
            PerformanceScoreMapper scoreMapper,
            ProjectBonusPoolMapper bonusPoolMapper,
            ProjectBonusAllocationMapper bonusAllocationMapper,
            SalaryDeductionRecordMapper salaryDeductionMapper,
            ProjectManagementService projectManagementService,
            SalaryMonthlyMapper salaryMonthlyMapper,
            OperationLogService operationLogService) {
        this(attendanceMapper, indicatorMapper, planMapper, null, scoreMapper, null,
                bonusPoolMapper, bonusAllocationMapper, salaryDeductionMapper, projectManagementService,
                salaryMonthlyMapper, null, null, null, null, null, operationLogService);
    }

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
        normalizeIndicator(indicator);
        indicator.setDeleted(0);
        indicatorMapper.insert(indicator);
        return indicator;
    }

    @Override
    public PerformanceIndicatorDO updateIndicator(Long id, PerformanceIndicatorDO indicator) {
        requireIndicator(id);
        indicator.setId(id);
        normalizeIndicator(indicator);
        indicatorMapper.updateById(indicator);
        return indicatorMapper.selectById(id);
    }

    @Override
    public Page<PerformancePlanDO> plans(long page, long pageSize, String month, Long projectId) {
        Page<PerformancePlanDO> result = planMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<PerformancePlanDO>()
                .eq(PerformancePlanDO::getDeleted, 0)
                .eq(StringUtils.hasText(month), PerformancePlanDO::getPerformanceMonth, month)
                .eq(projectId != null, PerformancePlanDO::getProjectId, projectId)
                .orderByDesc(PerformancePlanDO::getPerformanceMonth)
                .orderByDesc(PerformancePlanDO::getCreatedAt));
        result.getRecords().forEach(plan -> {
            if (plan.getProjectId() != null) {
                plan.setProjectName(projectManagementService.project(plan.getProjectId()).getName());
            }
        });
        return result;
    }

    @Override
    @Transactional
    public PerformancePlanDetailVO savePlan(PerformancePlanDO plan, List<Long> indicatorIds) {
        requireText(plan.getName(), "绩效方案名称必填");
        requireText(plan.getPerformanceMonth(), "绩效月份必填");
        plan.setStatus("draft");
        plan.setDeleted(0);
        planMapper.insert(plan);
        replacePlanIndicators(plan, indicatorIds);
        operationLogService.record("compensation", plan.getId(), "create_performance_plan", "创建绩效方案：" + plan.getName());
        return planDetail(plan.getId());
    }

    @Override
    @Transactional
    public PerformancePlanDetailVO updatePlan(Long id, PerformancePlanDO plan, List<Long> indicatorIds) {
        PerformancePlanDO old = requirePlan(id);
        requirePlanStatus(old, "draft");
        plan.setId(id);
        plan.setStatus("draft");
        plan.setDeleted(old.getDeleted());
        planMapper.updateById(plan);
        replacePlanIndicators(plan, indicatorIds);
        operationLogService.record("compensation", id, "update_performance_plan", "更新绩效方案：" + plan.getName());
        return planDetail(id);
    }

    @Override
    public PerformancePlanDetailVO planDetail(Long id) {
        PerformancePlanDO plan = requirePlan(id);
        return new PerformancePlanDetailVO(plan, planIndicators(id));
    }

    @Override
    public void activatePlan(Long id) {
        PerformancePlanDO plan = requirePlan(id);
        requirePlanStatus(plan, "draft");
        List<PerformancePlanIndicatorDO> indicators = planIndicators(id);
        if (indicators.isEmpty()) throw new BusinessException("绩效方案至少配置一个指标");
        BigDecimal totalWeight = indicators.stream()
                .map(PerformancePlanIndicatorDO::getScoreWeight).map(this::nvl)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (totalWeight.compareTo(new BigDecimal("100.00")) != 0) {
            throw new BusinessException("绩效方案指标权重合计必须等于 100");
        }
        if (plan.getProjectId() == null && indicators.stream().anyMatch(item -> "project".equals(item.getDataSource()))) {
            throw new BusinessException("包含项目自动指标的方案必须选择项目");
        }
        plan.setStatus("active");
        planMapper.updateById(plan);
        operationLogService.record("compensation", id, "activate_performance_plan", "启用绩效方案");
    }

    @Override
    public void closePlan(Long id) {
        PerformancePlanDO plan = requirePlan(id);
        requirePlanStatus(plan, "active");
        plan.setStatus("closed");
        planMapper.updateById(plan);
        operationLogService.record("compensation", id, "close_performance_plan", "关闭绩效方案");
    }

    @Override
    public Page<PerformanceScoreDO> scores(long page, long pageSize, String month, Long employeeId, Long projectId) {
        Page<PerformanceScoreDO> result = scoreMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<PerformanceScoreDO>()
                .eq(PerformanceScoreDO::getDeleted, 0)
                .eq(StringUtils.hasText(month), PerformanceScoreDO::getPerformanceMonth, month)
                .eq(employeeId != null, PerformanceScoreDO::getEmployeeId, employeeId)
                .eq(projectId != null, PerformanceScoreDO::getProjectId, projectId)
                .orderByDesc(PerformanceScoreDO::getPerformanceMonth)
                .orderByDesc(PerformanceScoreDO::getCreatedAt));
        result.getRecords().forEach(score -> {
            EmployeeDO employee = employeeMapper.selectById(score.getEmployeeId());
            PerformancePlanDO plan = planMapper.selectById(score.getPerformancePlanId());
            score.setEmployeeName(employee == null ? "已删除员工" : employee.getName());
            score.setPlanName(plan == null ? "已删除方案" : plan.getName());
            if (score.getProjectId() != null) {
                score.setProjectName(projectManagementService.project(score.getProjectId()).getName());
            }
        });
        return result;
    }

    @Override
    @Transactional
    public List<PerformanceScoreDetailVO> generateScores(Long planId, List<Long> employeeIds) {
        PerformancePlanDO plan = requirePlan(planId);
        requirePlanStatus(plan, "active");
        List<PerformancePlanIndicatorDO> indicators = planIndicators(planId);
        List<PerformanceScoreDetailVO> generated = new ArrayList<>();
        for (Long employeeId : employeeIds.stream().distinct().toList()) {
            EmployeeDO employee = employeeMapper.selectById(employeeId);
            if (employee == null || !"active".equals(employee.getStatus())) {
                throw new BusinessException("只能为在职员工生成绩效成绩");
            }
            if (plan.getPositionId() != null && !plan.getPositionId().equals(employee.getPositionId())) {
                throw new BusinessException("员工“" + employee.getName() + "”不属于方案岗位");
            }
            if (scoreMapper.selectCount(new LambdaQueryWrapper<PerformanceScoreDO>()
                    .eq(PerformanceScoreDO::getDeleted, 0)
                    .eq(PerformanceScoreDO::getEmployeeId, employeeId)
                    .eq(PerformanceScoreDO::getPerformanceMonth, plan.getPerformanceMonth())) > 0) {
                throw new BusinessException("员工“" + employee.getName() + "”本月绩效成绩已存在");
            }
            BigDecimal performanceBase = performanceBase(employee, plan.getPerformanceMonth());
            PerformanceScoreDO score = new PerformanceScoreDO();
            score.setPerformancePlanId(planId);
            score.setPerformanceMonth(plan.getPerformanceMonth());
            score.setProjectId(plan.getProjectId());
            score.setEmployeeId(employeeId);
            score.setPositionId(employee.getPositionId());
            score.setScore(BigDecimal.ZERO);
            score.setPerformanceBaseSnapshot(performanceBase);
            score.setPerformanceAmount(BigDecimal.ZERO);
            score.setStatus("draft");
            score.setDeleted(0);
            scoreMapper.insert(score);
            for (PerformancePlanIndicatorDO indicator : indicators) {
                scoreItemMapper.insert(createScoreItem(score, indicator));
            }
            recalculateScore(score);
            generated.add(scoreDetail(score.getId()));
        }
        operationLogService.record("compensation", planId, "generate_performance_scores",
                "生成绩效成绩：" + generated.size() + " 人");
        return generated;
    }

    @Override
    public PerformanceScoreDetailVO scoreDetail(Long id) {
        PerformanceScoreDO score = requireScore(id);
        return new PerformanceScoreDetailVO(score, scoreItems(id));
    }

    @Override
    @Transactional
    public PerformanceScoreDetailVO updateScore(Long id, CompensationWriteDTO.PerformanceScoreUpdateRequest dto) {
        PerformanceScoreDO score = requireScore(id);
        BusinessStateMachine.requireCurrent(Flow.PERFORMANCE_SCORE, score.getStatus(), "draft");
        Map<Long, CompensationWriteDTO.PerformanceScoreItemRequest> requests = new LinkedHashMap<>();
        for (CompensationWriteDTO.PerformanceScoreItemRequest item : dto.getItems()) {
            if (requests.put(item.getPlanIndicatorId(), item) != null) {
                throw new BusinessException("绩效评分明细不能重复");
            }
        }
        List<PerformanceScoreItemDO> items = scoreItems(id);
        for (PerformanceScoreItemDO item : items) {
            CompensationWriteDTO.PerformanceScoreItemRequest request = requests.remove(item.getPlanIndicatorId());
            if (request == null) continue;
            BigDecimal finalScore = request.getFinalScore().setScale(2, RoundingMode.HALF_UP);
            if (item.getAutoScore() != null && finalScore.compareTo(item.getAutoScore()) != 0
                    && !StringUtils.hasText(request.getOverrideReason())) {
                throw new BusinessException("覆盖自动评分时必须填写原因");
            }
            item.setFinalScore(finalScore);
            item.setOverrideReason(item.getAutoScore() != null && finalScore.compareTo(item.getAutoScore()) != 0
                    ? request.getOverrideReason().trim() : null);
            scoreItemMapper.updateById(item);
        }
        if (!requests.isEmpty()) throw new BusinessException("评分明细不属于当前绩效成绩");
        score.setRemark(dto.getRemark());
        recalculateScore(score);
        operationLogService.record("compensation", id, "update_performance_score", "更新绩效评分");
        return scoreDetail(id);
    }

    @Override
    @Transactional
    public PerformanceScoreDetailVO refreshScore(Long id) {
        PerformanceScoreDO score = requireScore(id);
        BusinessStateMachine.requireCurrent(Flow.PERFORMANCE_SCORE, score.getStatus(), "draft");
        Map<Long, PerformancePlanIndicatorDO> indicatorMap = planIndicators(score.getPerformancePlanId()).stream()
                .collect(java.util.stream.Collectors.toMap(PerformancePlanIndicatorDO::getId, item -> item));
        for (PerformanceScoreItemDO item : scoreItems(id)) {
            PerformancePlanIndicatorDO indicator = indicatorMap.get(item.getPlanIndicatorId());
            if (indicator == null || "manual".equals(indicator.getDataSource())) continue;
            MetricValue metric = metricValue(score, indicator);
            BigDecimal autoScore = calculateAutoScore(metric.value(), indicator);
            item.setActualValue(metric.value());
            item.setAutoScore(autoScore);
            item.setEvidenceSnapshot(metric.evidence());
            if (!StringUtils.hasText(item.getOverrideReason())) item.setFinalScore(autoScore);
            scoreItemMapper.updateById(item);
        }
        recalculateScore(score);
        operationLogService.record("compensation", id, "refresh_performance_score", "刷新绩效自动指标");
        return scoreDetail(id);
    }

    @Override
    public void confirmScore(Long id) {
        PerformanceScoreDO score = requireScore(id);
        String statusBefore = score.getStatus();
        BusinessStateMachine.requireTransition(Flow.PERFORMANCE_SCORE, statusBefore, "confirmed");
        recalculateScore(score);
        score.setStatus("confirmed");
        score.setConfirmedAt(LocalDateTime.now());
        score.setAutoMetricSnapshot(scoreItems(id).stream()
                .map(item -> "指标#" + item.getPlanIndicatorId() + ":" + defaultText(item.getEvidenceSnapshot(), "manual")
                        + ";最终分:" + item.getFinalScore()
                        + (StringUtils.hasText(item.getOverrideReason()) ? ";覆盖原因:" + item.getOverrideReason() : ""))
                .reduce((left, right) -> left + " | " + right).orElse(""));
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
        requireReleasedBonusPool(allocation.getBonusPoolId());
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
        for (ProjectBonusAllocationDO row : bonusAllocationMapper.selectConfirmedReleased(null, month)) {
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

    private void normalizeIndicator(PerformanceIndicatorDO indicator) {
        requireText(indicator.getName(), "指标名称必填");
        indicator.setScoreWeight(nvl(indicator.getScoreWeight()));
        if (indicator.getScoreWeight().compareTo(BigDecimal.ZERO) < 0
                || indicator.getScoreWeight().compareTo(BigDecimal.valueOf(100)) > 0) {
            throw new BusinessException("指标权重必须在 0 到 100 之间");
        }
        indicator.setDataSource(defaultText(indicator.getDataSource(), "manual"));
        indicator.setScoringDirection(defaultText(indicator.getScoringDirection(), "higher_better"));
        indicator.setStatus(defaultText(indicator.getStatus(), "active"));
        if ("manual".equals(indicator.getDataSource())) {
            indicator.setMetricCode(null);
        } else {
            requireText(indicator.getMetricCode(), "自动指标编码必填");
            if (!indicator.getDataSource().equals(METRIC_SOURCES.get(indicator.getMetricCode()))) {
                throw new BusinessException("指标编码与数据来源不匹配");
            }
            if (indicator.getTargetValue() == null || indicator.getTargetValue().compareTo(BigDecimal.ZERO) < 0) {
                throw new BusinessException("自动指标目标值必填且不能小于 0");
            }
        }
    }

    private void replacePlanIndicators(PerformancePlanDO plan, List<Long> indicatorIds) {
        if (indicatorIds == null || indicatorIds.isEmpty()) throw new BusinessException("绩效方案至少选择一个指标");
        planIndicatorMapper.delete(new LambdaQueryWrapper<PerformancePlanIndicatorDO>()
                .eq(PerformancePlanIndicatorDO::getPerformancePlanId, plan.getId()));
        int sortOrder = 0;
        for (Long indicatorId : indicatorIds.stream().distinct().toList()) {
            PerformanceIndicatorDO source = requireIndicator(indicatorId);
            if (!"active".equals(source.getStatus())) throw new BusinessException("绩效方案不能使用已停用指标");
            PerformancePlanIndicatorDO snapshot = new PerformancePlanIndicatorDO();
            snapshot.setPerformancePlanId(plan.getId());
            snapshot.setIndicatorId(source.getId());
            snapshot.setName(source.getName());
            snapshot.setCategory(source.getCategory());
            snapshot.setDataSource(source.getDataSource());
            snapshot.setMetricCode(source.getMetricCode());
            snapshot.setScoreWeight(source.getScoreWeight());
            snapshot.setScoringDirection(source.getScoringDirection());
            snapshot.setTargetValue(source.getTargetValue());
            snapshot.setSortOrder(sortOrder++);
            planIndicatorMapper.insert(snapshot);
        }
    }

    private List<PerformancePlanIndicatorDO> planIndicators(Long planId) {
        return planIndicatorMapper.selectList(new LambdaQueryWrapper<PerformancePlanIndicatorDO>()
                .eq(PerformancePlanIndicatorDO::getPerformancePlanId, planId)
                .orderByAsc(PerformancePlanIndicatorDO::getSortOrder)
                .orderByAsc(PerformancePlanIndicatorDO::getId));
    }

    private List<PerformanceScoreItemDO> scoreItems(Long scoreId) {
        return scoreItemMapper.selectList(new LambdaQueryWrapper<PerformanceScoreItemDO>()
                .eq(PerformanceScoreItemDO::getPerformanceScoreId, scoreId)
                .orderByAsc(PerformanceScoreItemDO::getId));
    }

    private PerformanceScoreItemDO createScoreItem(PerformanceScoreDO score, PerformancePlanIndicatorDO indicator) {
        PerformanceScoreItemDO item = new PerformanceScoreItemDO();
        item.setPerformanceScoreId(score.getId());
        item.setPlanIndicatorId(indicator.getId());
        if ("manual".equals(indicator.getDataSource())) {
            item.setFinalScore(BigDecimal.ZERO.setScale(2));
            item.setEvidenceSnapshot("手工评分");
        } else {
            MetricValue metric = metricValue(score, indicator);
            BigDecimal autoScore = calculateAutoScore(metric.value(), indicator);
            item.setActualValue(metric.value());
            item.setAutoScore(autoScore);
            item.setFinalScore(autoScore);
            item.setEvidenceSnapshot(metric.evidence());
        }
        return item;
    }

    private void recalculateScore(PerformanceScoreDO score) {
        Map<Long, PerformancePlanIndicatorDO> indicatorMap = planIndicators(score.getPerformancePlanId()).stream()
                .collect(java.util.stream.Collectors.toMap(PerformancePlanIndicatorDO::getId, item -> item));
        List<PerformanceScoreItemDO> items = scoreItems(score.getId());
        if (items.size() != indicatorMap.size()) throw new BusinessException("绩效评分明细不完整");
        BigDecimal weightedScore = BigDecimal.ZERO;
        for (PerformanceScoreItemDO item : items) {
            PerformancePlanIndicatorDO indicator = indicatorMap.get(item.getPlanIndicatorId());
            if (indicator == null || item.getFinalScore() == null) throw new BusinessException("绩效评分明细不合法");
            weightedScore = weightedScore.add(item.getFinalScore()
                    .multiply(nvl(indicator.getScoreWeight()))
                    .divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP));
        }
        weightedScore = weightedScore.setScale(2, RoundingMode.HALF_UP);
        score.setScore(weightedScore);
        score.setPerformanceAmount(nvl(score.getPerformanceBaseSnapshot())
                .multiply(weightedScore)
                .divide(BigDecimal.valueOf(100), 2, RoundingMode.HALF_UP));
        scoreMapper.updateById(score);
    }

    private BigDecimal performanceBase(EmployeeDO employee, String month) {
        YearMonth yearMonth;
        try {
            yearMonth = YearMonth.parse(month);
        } catch (Exception ex) {
            throw new BusinessException("绩效月份格式不合法");
        }
        SalaryProfileDO profile = salaryProfileMapper.selectList(new LambdaQueryWrapper<SalaryProfileDO>()
                        .eq(SalaryProfileDO::getDeleted, 0)
                        .eq(SalaryProfileDO::getStatus, "active")
                        .eq(SalaryProfileDO::getEmployeeId, employee.getId())
                        .and(wrapper -> wrapper.isNull(SalaryProfileDO::getEffectiveDate)
                                .or().le(SalaryProfileDO::getEffectiveDate, yearMonth.atEndOfMonth())))
                .stream()
                .max(Comparator
                        .comparing(SalaryProfileDO::getEffectiveDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(SalaryProfileDO::getCreatedAt, Comparator.nullsFirst(Comparator.naturalOrder()))
                        .thenComparing(SalaryProfileDO::getId))
                .orElseThrow(() -> new BusinessException("员工“" + employee.getName() + "”缺少有效工资档案"));
        List<SalaryProfileItemDO> bases = salaryProfileItemMapper.selectList(
                        new LambdaQueryWrapper<SalaryProfileItemDO>()
                                .eq(SalaryProfileItemDO::getSalaryProfileId, profile.getId()))
                .stream()
                .filter(item -> {
                    if (item.getSalaryItemId() == null) return false;
                    SalaryItemDO salaryItem = salaryItemMapper.selectById(item.getSalaryItemId());
                    return salaryItem != null && "performance_base".equals(salaryItem.getCalculationRole());
                }).toList();
        if (bases.size() != 1) {
            throw new BusinessException("员工“" + employee.getName() + "”缺少唯一有效绩效基数");
        }
        return nvl(bases.get(0).getAmount()).setScale(2, RoundingMode.HALF_UP);
    }

    private MetricValue metricValue(PerformanceScoreDO score, PerformancePlanIndicatorDO indicator) {
        String code = indicator.getMetricCode();
        if ("project".equals(indicator.getDataSource())) {
            if (score.getProjectId() == null) throw new BusinessException("项目自动指标缺少项目");
            ProjectDashboardVO dashboard = projectManagementService.dashboard(score.getProjectId());
            BigDecimal value = switch (code) {
                case "project_profit_rate" -> dashboard.getProfitRate();
                case "project_payment_progress" -> dashboard.getPaymentProgress();
                case "project_cost_rate" -> dashboard.getCostRate();
                case "project_risk_count" -> BigDecimal.valueOf(dashboard.getRisks().size());
                default -> throw new BusinessException("自动指标不可计算：" + code);
            };
            return new MetricValue(nvl(value), "项目#" + score.getProjectId() + ";" + code + "=" + nvl(value));
        }
        if ("attendance".equals(indicator.getDataSource())) {
            AttendanceRecordDO attendance = attendanceMapper.selectOne(new LambdaQueryWrapper<AttendanceRecordDO>()
                    .eq(AttendanceRecordDO::getDeleted, 0)
                    .eq(AttendanceRecordDO::getEmployeeId, score.getEmployeeId())
                    .eq(AttendanceRecordDO::getAttendanceMonth, score.getPerformanceMonth())
                    .orderByDesc(AttendanceRecordDO::getCreatedAt)
                    .orderByDesc(AttendanceRecordDO::getId)
                    .last("LIMIT 1"));
            if (attendance == null) throw new BusinessException("自动指标不可计算：缺少员工考勤记录");
            BigDecimal value = switch (code) {
                case "attendance_work_days" -> attendance.getWorkDays();
                case "attendance_absent_days" -> attendance.getAbsentDays();
                case "attendance_leave_days" -> attendance.getLeaveDays();
                case "attendance_project_site_days" -> attendance.getProjectSiteDays();
                default -> throw new BusinessException("自动指标不可计算：" + code);
            };
            return new MetricValue(nvl(value), "考勤#" + attendance.getId() + ";" + code + "=" + nvl(value));
        }
        if ("salary".equals(indicator.getDataSource())) {
            String sourceMonth = YearMonth.parse(score.getPerformanceMonth()).minusMonths(1).toString();
            SalaryMonthlyDO monthly = salaryMonthlyMapper.selectOne(new LambdaQueryWrapper<SalaryMonthlyDO>()
                    .eq(SalaryMonthlyDO::getDeleted, 0)
                    .eq(SalaryMonthlyDO::getSalaryMonth, sourceMonth)
                    .orderByDesc(SalaryMonthlyDO::getCreatedAt)
                    .last("LIMIT 1"));
            if (monthly == null) throw new BusinessException("自动指标不可计算：缺少上月工资表");
            SalaryMonthlyDetailDO detail = salaryMonthlyDetailMapper.selectOne(
                    new LambdaQueryWrapper<SalaryMonthlyDetailDO>()
                            .eq(SalaryMonthlyDetailDO::getDeleted, 0)
                            .eq(SalaryMonthlyDetailDO::getSalaryMonthlyId, monthly.getId())
                            .eq(SalaryMonthlyDetailDO::getEmployeeId, score.getEmployeeId())
                            .last("LIMIT 1"));
            if (detail == null) throw new BusinessException("自动指标不可计算：缺少员工工资明细");
            BigDecimal value = switch (code) {
                case "salary_payment_completed" -> "paid".equals(detail.getPaymentStatus()) ? BigDecimal.ONE : BigDecimal.ZERO;
                case "salary_payslip_no_exception" -> "exception".equals(detail.getPayslipStatus()) ? BigDecimal.ZERO : BigDecimal.ONE;
                default -> throw new BusinessException("自动指标不可计算：" + code);
            };
            return new MetricValue(value, "上月工资(" + sourceMonth + ");" + code + "=" + value);
        }
        throw new BusinessException("自动指标不可计算：" + code);
    }

    private BigDecimal calculateAutoScore(BigDecimal actual, PerformancePlanIndicatorDO indicator) {
        return calculateAutoScore(actual, indicator.getTargetValue(), indicator.getScoringDirection());
    }

    static BigDecimal calculateAutoScore(BigDecimal actualValue, BigDecimal targetValue, String direction) {
        BigDecimal actual = actualValue == null ? BigDecimal.ZERO : actualValue;
        BigDecimal target = targetValue == null ? BigDecimal.ZERO : targetValue;
        BigDecimal score;
        if ("lower_better".equals(direction)) {
            if (target.compareTo(BigDecimal.ZERO) == 0) {
                score = actual.compareTo(BigDecimal.ZERO) == 0 ? BigDecimal.valueOf(100) : BigDecimal.ZERO;
            } else if (actual.compareTo(target) <= 0) {
                score = BigDecimal.valueOf(100);
            } else {
                score = target.divide(actual, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
            }
        } else {
            if (target.compareTo(BigDecimal.ZERO) <= 0) {
                throw new BusinessException("越高越好指标目标值必须大于 0");
            }
            score = actual.divide(target, 6, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));
        }
        return score.max(BigDecimal.ZERO).min(BigDecimal.valueOf(100)).setScale(2, RoundingMode.HALF_UP);
    }

    private void requirePlanStatus(PerformancePlanDO plan, String status) {
        if (!status.equals(plan.getStatus())) {
            throw new BusinessException("绩效方案当前状态不允许操作：" + plan.getStatus());
        }
    }

    private record MetricValue(BigDecimal value, String evidence) {
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
        ProjectBonusPoolDO pool = requireReleasedBonusPool(allocation.getBonusPoolId());
        allocation.setProjectId(pool.getProjectId());
        if (allocation.getEmployeeId() == null) throw new BusinessException("员工必填");
        requireText(allocation.getPaymentMonth(), "发放月份必填");
        allocation.setAllocationRatio(nvl(allocation.getAllocationRatio()));
        if (allocation.getAllocationAmount() == null || allocation.getAllocationAmount().compareTo(BigDecimal.ZERO) == 0) {
            allocation.setAllocationAmount(nvl(pool.getBonusPoolAmount()).multiply(allocation.getAllocationRatio()));
        }
    }

    private ProjectBonusPoolDO requireReleasedBonusPool(Long id) {
        ProjectBonusPoolDO pool = requireBonusPool(id);
        if (!"released".equals(pool.getReleaseStatus())) {
            throw new BusinessException("项目奖金池未发布");
        }
        return pool;
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

package com.company.labor.module.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.statemachine.BusinessStateMachine;
import com.company.labor.common.statemachine.BusinessStateMachine.Flow;
import com.company.labor.module.compensation.domain.entity.AttendanceRecordDO;
import com.company.labor.module.compensation.domain.entity.PerformanceScoreDO;
import com.company.labor.module.compensation.domain.entity.ProjectBonusAllocationDO;
import com.company.labor.module.compensation.domain.entity.SalarySourceRecordDO;
import com.company.labor.module.compensation.mapper.AttendanceRecordMapper;
import com.company.labor.module.compensation.mapper.PerformanceScoreMapper;
import com.company.labor.module.compensation.mapper.ProjectBonusAllocationMapper;
import com.company.labor.module.compensation.mapper.SalarySourceRecordMapper;
import com.company.labor.module.employee.domain.entity.EmployeeDO;
import com.company.labor.module.employee.mapper.EmployeeMapper;
import com.company.labor.module.salary.domain.dto.BatchSalaryPaymentDTO;
import com.company.labor.module.salary.domain.dto.SalaryPaymentDTO;
import com.company.labor.module.salary.domain.entity.SalaryAuditRecordDO;
import com.company.labor.module.salary.domain.entity.SalaryDetailItemDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDetailDO;
import com.company.labor.module.salary.domain.entity.SalaryProfileDO;
import com.company.labor.module.salary.domain.entity.SalaryProfileItemDO;
import com.company.labor.module.salary.mapper.SalaryAuditRecordMapper;
import com.company.labor.module.salary.mapper.SalaryDetailItemMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyDetailMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyMapper;
import com.company.labor.module.salary.mapper.SalaryProfileItemMapper;
import com.company.labor.module.salary.mapper.SalaryProfileMapper;
import com.company.labor.module.salary.service.SalaryMonthlyService;
import com.company.labor.module.system.service.OperationLogService;
import com.company.labor.security.AuditUserProvider;
import com.company.labor.security.LoginUser;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SalaryMonthlyServiceImpl extends ServiceImpl<SalaryMonthlyMapper, SalaryMonthlyDO> implements SalaryMonthlyService {
    private final EmployeeMapper employeeMapper;
    private final SalaryProfileMapper salaryProfileMapper;
    private final SalaryProfileItemMapper salaryProfileItemMapper;
    private final SalaryMonthlyDetailMapper detailMapper;
    private final SalaryDetailItemMapper detailItemMapper;
    private final SalaryAuditRecordMapper auditRecordMapper;
    private final OperationLogService operationLogService;
    private final AuditUserProvider auditUserProvider;
    private final AttendanceRecordMapper attendanceRecordMapper;
    private final PerformanceScoreMapper performanceScoreMapper;
    private final ProjectBonusAllocationMapper bonusAllocationMapper;
    private final SalarySourceRecordMapper salarySourceRecordMapper;

    @Override
    @Transactional
    public SalaryMonthlyDO createMonthly(SalaryMonthlyDO monthly) {
        if (!StringUtils.hasText(monthly.getSalaryMonth())) {
            throw new BusinessException("工资月份必填");
        }
        if (baseMapper.countActiveByMonth(monthly.getSalaryMonth()) > 0) {
            throw new BusinessException("该月份工资表已存在，请勿重复创建");
        }
        if (!StringUtils.hasText(monthly.getTitle())) {
            monthly.setTitle(monthly.getSalaryMonth() + " 工资表");
        }
        monthly.setStatus("draft");
        monthly.setTotalEmployeeCount(0);
        monthly.setTotalPayableAmount(BigDecimal.ZERO);
        monthly.setTotalDeductionAmount(BigDecimal.ZERO);
        monthly.setTotalActualAmount(BigDecimal.ZERO);
        monthly.setDeleted(0);
        save(monthly);
        log("create_monthly", monthly.getId(), "创建工资表：" + monthly.getSalaryMonth());
        audit(monthly.getId(), null, "create_monthly", null, monthly.getStatus(), "创建工资表：" + monthly.getSalaryMonth(), null);
        return monthly;
    }

    @Override
    @Transactional
    public SalaryMonthlyDO updateMonthly(Long monthlyId, SalaryMonthlyDO monthly) {
        SalaryMonthlyDO old = requireMonthly(monthlyId);
        monthly.setId(monthlyId);
        monthly.setStatus(old.getStatus());
        updateById(monthly);
        SalaryMonthlyDO updated = requireMonthly(monthlyId);
        String content = "修改工资表：" + valueOrDefault(old.getTitle(), old.getSalaryMonth()) + " -> " + valueOrDefault(updated.getTitle(), updated.getSalaryMonth());
        log("update_monthly", monthlyId, content);
        audit(monthlyId, null, "update_monthly", old.getStatus(), updated.getStatus(), content, null);
        return updated;
    }

    @Override
    @Transactional
    public void generate(Long monthlyId) {
        SalaryMonthlyDO monthly = requireMonthly(monthlyId);
        BusinessStateMachine.requireCurrent(Flow.SALARY_MONTHLY, monthly.getStatus(), "draft");
        List<SalaryProfileDO> activeProfiles = latestActiveProfiles();
        if (activeProfiles.isEmpty()) {
            throw new BusinessException("没有启用的工资档案，无法生成工资明细");
        }
        List<SalaryMonthlyDetailDO> oldDetails = detailMapper.selectList(new LambdaQueryWrapper<SalaryMonthlyDetailDO>().eq(SalaryMonthlyDetailDO::getSalaryMonthlyId, monthlyId));
        for (SalaryMonthlyDetailDO old : oldDetails) {
            detailItemMapper.delete(new LambdaUpdateWrapper<SalaryDetailItemDO>().eq(SalaryDetailItemDO::getSalaryDetailId, old.getId()));
        }
        salarySourceRecordMapper.delete(new LambdaUpdateWrapper<SalarySourceRecordDO>().eq(SalarySourceRecordDO::getSalaryMonthlyId, monthlyId));
        detailMapper.delete(new LambdaUpdateWrapper<SalaryMonthlyDetailDO>().eq(SalaryMonthlyDetailDO::getSalaryMonthlyId, monthlyId));

        int generatedCount = 0;
        List<String> skippedReasons = new ArrayList<>();
        for (SalaryProfileDO profile : activeProfiles) {
            EmployeeDO employee = employeeMapper.selectByIdIncludingDeleted(profile.getEmployeeId());
            if (employee == null) {
                skippedReasons.add("档案 " + profile.getId() + " 绑定的员工 " + profile.getEmployeeId() + " 不存在");
                continue;
            }
            if (employee.getDeleted() != null && employee.getDeleted() == 1) {
                skippedReasons.add("员工 " + employee.getName() + " 已删除");
                continue;
            }
            if ("left".equals(employee.getStatus())) {
                skippedReasons.add("员工 " + employee.getName() + " 已离职");
                continue;
            }
            List<SalaryProfileItemDO> profileItems = salaryProfileItemMapper.selectList(new LambdaQueryWrapper<SalaryProfileItemDO>().eq(SalaryProfileItemDO::getSalaryProfileId, profile.getId()));
            BigDecimal payable = BigDecimal.ZERO;
            BigDecimal deduction = BigDecimal.ZERO;
            for (SalaryProfileItemDO item : profileItems) {
                BigDecimal amount = item.getAmount() == null ? BigDecimal.ZERO : item.getAmount();
                if ("deduction".equals(item.getSalaryItemType())) {
                    deduction = deduction.add(amount);
                } else {
                    payable = payable.add(amount);
                }
            }
            AttendanceRecordDO attendance = attendanceRecordMapper.selectOne(new LambdaQueryWrapper<AttendanceRecordDO>()
                    .eq(AttendanceRecordDO::getDeleted, 0)
                    .eq(AttendanceRecordDO::getEmployeeId, employee.getId())
                    .eq(AttendanceRecordDO::getAttendanceMonth, monthly.getSalaryMonth())
                    .orderByDesc(AttendanceRecordDO::getCreatedAt)
                    .last("LIMIT 1"));
            BigDecimal attendanceDeduction = attendance == null || attendance.getDeductionAmount() == null ? BigDecimal.ZERO : attendance.getDeductionAmount();
            deduction = deduction.add(attendanceDeduction);

            List<PerformanceScoreDO> scores = performanceScoreMapper.selectList(new LambdaQueryWrapper<PerformanceScoreDO>()
                    .eq(PerformanceScoreDO::getDeleted, 0)
                    .eq(PerformanceScoreDO::getEmployeeId, employee.getId())
                    .eq(PerformanceScoreDO::getPerformanceMonth, monthly.getSalaryMonth())
                    .eq(PerformanceScoreDO::getStatus, "confirmed"));
            BigDecimal performanceAmount = BigDecimal.ZERO;
            for (PerformanceScoreDO score : scores) {
                performanceAmount = performanceAmount.add(score.getPerformanceAmount() == null ? BigDecimal.ZERO : score.getPerformanceAmount());
            }
            payable = payable.add(performanceAmount);

            List<ProjectBonusAllocationDO> allocations = bonusAllocationMapper.selectList(new LambdaQueryWrapper<ProjectBonusAllocationDO>()
                    .eq(ProjectBonusAllocationDO::getDeleted, 0)
                    .eq(ProjectBonusAllocationDO::getEmployeeId, employee.getId())
                    .eq(ProjectBonusAllocationDO::getPaymentMonth, monthly.getSalaryMonth())
                    .in(ProjectBonusAllocationDO::getStatus, List.of("allocated", "confirmed")));
            BigDecimal projectBonus = BigDecimal.ZERO;
            for (ProjectBonusAllocationDO allocation : allocations) {
                projectBonus = projectBonus.add(allocation.getAllocationAmount() == null ? BigDecimal.ZERO : allocation.getAllocationAmount());
            }
            payable = payable.add(projectBonus);

            SalaryMonthlyDetailDO detail = new SalaryMonthlyDetailDO();
            detail.setSalaryMonthlyId(monthlyId);
            detail.setEmployeeId(employee.getId());
            detail.setDepartmentId(employee.getDepartmentId());
            detail.setPositionId(employee.getPositionId());
            detail.setPayableAmount(payable);
            detail.setDeductionAmount(deduction);
            detail.setActualAmount(payable.subtract(deduction));
            detail.setPaymentStatus("unpaid");
            detail.setDeleted(0);
            detailMapper.insert(detail);
            generatedCount++;
            for (SalaryProfileItemDO profileItem : profileItems) {
                SalaryDetailItemDO detailItem = new SalaryDetailItemDO();
                detailItem.setSalaryDetailId(detail.getId());
                detailItem.setSalaryItemId(profileItem.getSalaryItemId());
                detailItem.setSalaryItemName(profileItem.getSalaryItemName());
                detailItem.setSalaryItemType(profileItem.getSalaryItemType());
                detailItem.setAmount(profileItem.getAmount());
                detailItem.setRemark(profileItem.getRemark());
                detailItemMapper.insert(detailItem);
                source(monthlyId, detail.getId(), employee.getId(), "fixed_salary", profileItem.getSalaryItemId(), profileItem.getSalaryItemName(), profileItem.getAmount(), profileItem.getRemark());
            }
            if (attendanceDeduction.compareTo(BigDecimal.ZERO) != 0) {
                detailItemMapper.insert(detailItem(detail.getId(), 2204L, "考勤扣款", "deduction", attendanceDeduction, "来自考勤记录"));
                source(monthlyId, detail.getId(), employee.getId(), "attendance_deduction", attendance == null ? null : attendance.getId(), "考勤扣款", attendanceDeduction.negate(), "请假/缺勤扣款");
            }
            if (performanceAmount.compareTo(BigDecimal.ZERO) != 0) {
                detailItemMapper.insert(detailItem(detail.getId(), 2201L, "绩效奖金", "earning", performanceAmount, "来自绩效评分"));
                for (PerformanceScoreDO score : scores) {
                    source(monthlyId, detail.getId(), employee.getId(), "performance_salary", score.getId(), "绩效工资", score.getPerformanceAmount(), score.getAutoMetricSnapshot());
                }
            }
            if (projectBonus.compareTo(BigDecimal.ZERO) != 0) {
                detailItemMapper.insert(detailItem(detail.getId(), 2202L, "项目奖金", "earning", projectBonus, "来自项目奖金分配"));
                for (ProjectBonusAllocationDO allocation : allocations) {
                    source(monthlyId, detail.getId(), employee.getId(), "project_bonus", allocation.getId(), "项目奖金", allocation.getAllocationAmount(), allocation.getRoleName());
                }
            }
        }
        if (generatedCount == 0) {
            String reason = skippedReasons.isEmpty() ? "未找到可生成的员工" : String.join("；", skippedReasons.subList(0, Math.min(skippedReasons.size(), 3)));
            throw new BusinessException("启用的工资档案未生成任何明细：" + reason);
        }
        summarize(monthlyId);
        log("generate_monthly", monthlyId, "生成工资明细");
        audit(monthlyId, null, "generate_monthly", monthly.getStatus(), "draft", "生成工资明细：" + generatedCount + " 人", null);
    }

    @Override
    @Transactional
    public void submit(Long monthlyId) {
        SalaryMonthlyDO monthly = requireMonthly(monthlyId);
        String statusBefore = monthly.getStatus();
        BusinessStateMachine.requireTransition(Flow.SALARY_MONTHLY, statusBefore, "pending_approval");
        monthly.setStatus("pending_approval");
        updateById(monthly);
        log("submit_approval", monthlyId, "提交审核");
        audit(monthlyId, null, "submit_approval", statusBefore, "pending_approval", "提交审核", null);
    }

    @Override
    @Transactional
    public void approve(Long monthlyId, Long userId, String remark) {
        SalaryMonthlyDO monthly = requireMonthly(monthlyId);
        String statusBefore = monthly.getStatus();
        BusinessStateMachine.requireTransition(Flow.SALARY_MONTHLY, statusBefore, "approved");
        monthly.setStatus("approved");
        monthly.setApprovedBy(userId);
        monthly.setApprovedAt(LocalDateTime.now());
        updateById(monthly);
        log("approve", monthlyId, "审核通过");
        audit(monthlyId, null, "approve", statusBefore, "approved", textOrDefault(remark, "审核通过"), null);
    }

    @Override
    @Transactional
    public void reject(Long monthlyId, String remark) {
        SalaryMonthlyDO monthly = requireMonthly(monthlyId);
        String statusBefore = monthly.getStatus();
        BusinessStateMachine.requireTransition(Flow.SALARY_MONTHLY, statusBefore, "draft");
        monthly.setStatus("draft");
        updateById(monthly);
        log("reject", monthlyId, "审核驳回");
        audit(monthlyId, null, "reject", statusBefore, "draft", textOrDefault(remark, "审核驳回"), null);
    }

    @Override
    @Transactional
    public void markMonthlyPaid(Long monthlyId, SalaryPaymentDTO dto) {
        SalaryMonthlyDO monthly = requireMonthly(monthlyId);
        BusinessStateMachine.requireCurrent(Flow.SALARY_MONTHLY, monthly.getStatus(), "approved");
        BatchSalaryPaymentDTO batch = new BatchSalaryPaymentDTO();
        batch.setMonthlyId(monthlyId);
        batch.setPaymentDate(dto.getPaymentDate());
        batch.setPaymentMethod(dto.getPaymentMethod());
        batch.setPaymentProofUrl(dto.getPaymentProofUrl());
        batchMarkPaid(batch);
    }

    @Override
    public Page<SalaryMonthlyDetailDO> details(Long monthlyId, long page, long pageSize) {
        return detailMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<SalaryMonthlyDetailDO>()
                .eq(SalaryMonthlyDetailDO::getSalaryMonthlyId, monthlyId)
                .eq(SalaryMonthlyDetailDO::getDeleted, 0)
                .orderByAsc(SalaryMonthlyDetailDO::getEmployeeId));
    }

    @Override
    @Transactional
    public SalaryMonthlyDetailDO updateDetail(Long detailId, SalaryMonthlyDetailDO detail) {
        SalaryMonthlyDetailDO old = detailMapper.selectById(detailId);
        if (old == null) throw new BusinessException("工资明细不存在");
        SalaryMonthlyDO monthly = requireMonthly(old.getSalaryMonthlyId());
        BusinessStateMachine.requireCurrent(Flow.SALARY_MONTHLY, monthly.getStatus(), "draft");
        detail.setId(detailId);
        detail.setSalaryMonthlyId(old.getSalaryMonthlyId());
        detail.setPaymentStatus(old.getPaymentStatus());
        detailMapper.updateById(detail);
        summarize(old.getSalaryMonthlyId());
        SalaryMonthlyDetailDO updated = detailMapper.selectById(detailId);
        String content = "修改工资明细：" + detailId + "，实发 " + amount(old.getActualAmount()) + " -> " + amount(updated.getActualAmount());
        log("update_detail", old.getSalaryMonthlyId(), content);
        audit(old.getSalaryMonthlyId(), detailId, "update_detail", monthly.getStatus(), monthly.getStatus(), content, null);
        return detailMapper.selectById(detailId);
    }

    @Override
    @Transactional
    public void markDetailPaid(Long detailId, SalaryPaymentDTO dto) {
        SalaryMonthlyDetailDO detail = detailMapper.selectById(detailId);
        if (detail == null) throw new BusinessException("工资明细不存在");
        SalaryMonthlyDO monthly = requireMonthly(detail.getSalaryMonthlyId());
        BusinessStateMachine.requireCurrent(Flow.SALARY_MONTHLY, monthly.getStatus(), "approved");
        String beforePaymentStatus = detail.getPaymentStatus();
        applyPayment(detail, dto);
        detailMapper.updateById(detail);
        refreshPaidStatus(monthly.getId());
        log("single_payment", detail.getSalaryMonthlyId(), "单人发放：" + detailId);
        audit(detail.getSalaryMonthlyId(), detailId, "single_payment", beforePaymentStatus, detail.getPaymentStatus(), "单人发放：" + detailId, dto.getPaymentProofUrl());
        if (StringUtils.hasText(dto.getPaymentProofUrl())) {
            log("upload_proof", detail.getSalaryMonthlyId(), "上传凭证：" + detailId);
            audit(detail.getSalaryMonthlyId(), detailId, "upload_proof", null, null, "上传凭证：" + detailId, dto.getPaymentProofUrl());
        }
    }

    @Override
    @Transactional
    public void batchMarkPaid(BatchSalaryPaymentDTO dto) {
        if (dto.getMonthlyId() == null && CollectionUtils.isEmpty(dto.getDetailIds())) {
            throw new BusinessException("请选择工资表或工资明细");
        }
        List<SalaryMonthlyDetailDO> details = new ArrayList<>();
        if (!CollectionUtils.isEmpty(dto.getDetailIds())) {
            details.addAll(detailMapper.selectBatchIds(dto.getDetailIds()));
        } else {
            details.addAll(detailMapper.selectList(new LambdaQueryWrapper<SalaryMonthlyDetailDO>()
                    .eq(SalaryMonthlyDetailDO::getSalaryMonthlyId, dto.getMonthlyId())
                    .eq(SalaryMonthlyDetailDO::getDeleted, 0)));
        }
        Set<Long> affectedMonthlyIds = new HashSet<>();
        for (SalaryMonthlyDetailDO detail : details) {
            SalaryMonthlyDO monthly = requireMonthly(detail.getSalaryMonthlyId());
            BusinessStateMachine.requireCurrent(Flow.SALARY_MONTHLY, monthly.getStatus(), "approved");
            String beforePaymentStatus = detail.getPaymentStatus();
            applyPayment(detail, dto);
            detailMapper.updateById(detail);
            refreshPaidStatus(detail.getSalaryMonthlyId());
            affectedMonthlyIds.add(detail.getSalaryMonthlyId());
            audit(detail.getSalaryMonthlyId(), detail.getId(), "batch_payment", beforePaymentStatus, detail.getPaymentStatus(), "批量发放", dto.getPaymentProofUrl());
            if (StringUtils.hasText(dto.getPaymentProofUrl())) {
                audit(detail.getSalaryMonthlyId(), detail.getId(), "upload_proof", null, null, "批量上传凭证", dto.getPaymentProofUrl());
            }
        }
        Long businessId = dto.getMonthlyId() == null && !details.isEmpty() ? details.get(0).getSalaryMonthlyId() : dto.getMonthlyId();
        log("batch_payment", businessId, "批量发放：" + details.size() + " 人");
        for (Long monthlyId : affectedMonthlyIds) {
            audit(monthlyId, null, "batch_payment", null, null, "批量发放：" + countByMonthly(details, monthlyId) + " 人", dto.getPaymentProofUrl());
        }
        if (StringUtils.hasText(dto.getPaymentProofUrl())) {
            log("upload_proof", businessId, "批量上传凭证：" + details.size() + " 人");
            for (Long monthlyId : affectedMonthlyIds) {
                audit(monthlyId, null, "upload_proof", null, null, "批量上传凭证：" + countByMonthly(details, monthlyId) + " 人", dto.getPaymentProofUrl());
            }
        }
    }

    @Override
    public List<SalaryAuditRecordDO> auditRecords(Long monthlyId) {
        requireMonthly(monthlyId);
        return auditRecordMapper.selectByMonthlyId(monthlyId);
    }

    private SalaryProfileDO latestProfile(Long employeeId) {
        return salaryProfileMapper.selectOne(new LambdaQueryWrapper<SalaryProfileDO>()
                .eq(SalaryProfileDO::getEmployeeId, employeeId)
                .eq(SalaryProfileDO::getStatus, "active")
                .eq(SalaryProfileDO::getDeleted, 0)
                .orderByDesc(SalaryProfileDO::getEffectiveDate)
                .orderByDesc(SalaryProfileDO::getCreatedAt)
                .last("LIMIT 1"));
    }

    private List<SalaryProfileDO> latestActiveProfiles() {
        List<SalaryProfileDO> profiles = salaryProfileMapper.selectList(new LambdaQueryWrapper<SalaryProfileDO>()
                .eq(SalaryProfileDO::getStatus, "active")
                .eq(SalaryProfileDO::getDeleted, 0)
                .orderByAsc(SalaryProfileDO::getEmployeeId)
                .orderByDesc(SalaryProfileDO::getEffectiveDate)
                .orderByDesc(SalaryProfileDO::getCreatedAt));
        List<SalaryProfileDO> result = new ArrayList<>();
        Set<Long> employeeIds = new HashSet<>();
        for (SalaryProfileDO profile : profiles) {
            if (profile.getEmployeeId() != null && employeeIds.add(profile.getEmployeeId())) {
                result.add(profile);
            }
        }
        return result;
    }

    private SalaryMonthlyDO requireMonthly(Long monthlyId) {
        SalaryMonthlyDO monthly = getById(monthlyId);
        if (monthly == null || monthly.getDeleted() != null && monthly.getDeleted() == 1) {
            throw new BusinessException("工资表不存在");
        }
        return monthly;
    }

    private void applyPayment(SalaryMonthlyDetailDO detail, SalaryPaymentDTO dto) {
        BusinessStateMachine.requireTransition(Flow.SALARY_DETAIL_PAYMENT, detail.getPaymentStatus(), "paid");
        detail.setPaymentStatus("paid");
        detail.setPaymentDate(dto.getPaymentDate() == null ? LocalDate.now() : dto.getPaymentDate());
        detail.setPaymentMethod(StringUtils.hasText(dto.getPaymentMethod()) ? dto.getPaymentMethod() : "bank_transfer");
        detail.setPaymentProofUrl(dto.getPaymentProofUrl());
    }

    private SalaryDetailItemDO detailItem(Long detailId, Long itemId, String name, String type, BigDecimal amount, String remark) {
        SalaryDetailItemDO item = new SalaryDetailItemDO();
        item.setSalaryDetailId(detailId);
        item.setSalaryItemId(itemId);
        item.setSalaryItemName(name);
        item.setSalaryItemType(type);
        item.setAmount(amount);
        item.setRemark(remark);
        return item;
    }

    private void source(Long monthlyId, Long detailId, Long employeeId, String type, Long sourceId, String sourceName, BigDecimal amount, String remark) {
        SalarySourceRecordDO row = new SalarySourceRecordDO();
        row.setSalaryMonthlyId(monthlyId);
        row.setSalaryDetailId(detailId);
        row.setEmployeeId(employeeId);
        row.setSourceType(type);
        row.setSourceId(sourceId);
        row.setSourceName(sourceName);
        row.setAmount(amount == null ? BigDecimal.ZERO : amount);
        row.setRemark(remark);
        row.setCreatedAt(LocalDateTime.now());
        salarySourceRecordMapper.insert(row);
    }

    private void summarize(Long monthlyId) {
        List<SalaryMonthlyDetailDO> details = detailMapper.selectList(new LambdaQueryWrapper<SalaryMonthlyDetailDO>().eq(SalaryMonthlyDetailDO::getSalaryMonthlyId, monthlyId).eq(SalaryMonthlyDetailDO::getDeleted, 0));
        BigDecimal payable = BigDecimal.ZERO;
        BigDecimal deduction = BigDecimal.ZERO;
        BigDecimal actual = BigDecimal.ZERO;
        for (SalaryMonthlyDetailDO detail : details) {
            payable = payable.add(detail.getPayableAmount() == null ? BigDecimal.ZERO : detail.getPayableAmount());
            deduction = deduction.add(detail.getDeductionAmount() == null ? BigDecimal.ZERO : detail.getDeductionAmount());
            actual = actual.add(detail.getActualAmount() == null ? BigDecimal.ZERO : detail.getActualAmount());
        }
        SalaryMonthlyDO monthly = requireMonthly(monthlyId);
        monthly.setTotalEmployeeCount(details.size());
        monthly.setTotalPayableAmount(payable);
        monthly.setTotalDeductionAmount(deduction);
        monthly.setTotalActualAmount(actual);
        updateById(monthly);
    }

    private void refreshPaidStatus(Long monthlyId) {
        List<SalaryMonthlyDetailDO> details = detailMapper.selectList(new LambdaQueryWrapper<SalaryMonthlyDetailDO>().eq(SalaryMonthlyDetailDO::getSalaryMonthlyId, monthlyId).eq(SalaryMonthlyDetailDO::getDeleted, 0));
        boolean allPaid = !details.isEmpty() && details.stream().allMatch(item -> "paid".equals(item.getPaymentStatus()));
        if (allPaid) {
            SalaryMonthlyDO monthly = requireMonthly(monthlyId);
            if ("paid".equals(monthly.getStatus())) {
                return;
            }
            String statusBefore = monthly.getStatus();
            BusinessStateMachine.requireTransition(Flow.SALARY_MONTHLY, statusBefore, "paid");
            monthly.setStatus("paid");
            monthly.setPaidAt(LocalDateTime.now());
            updateById(monthly);
            log("monthly_paid", monthlyId, "工资表全部发放完成");
            audit(monthlyId, null, "monthly_paid", statusBefore, "paid", "工资表全部发放完成", null);
        }
    }

    private void log(String action, Long businessId, String content) {
        operationLogService.record("salary", businessId, action, content);
    }

    private void audit(Long monthlyId, Long detailId, String action, String statusBefore, String statusAfter, String remark, String proofUrl) {
        SalaryAuditRecordDO record = new SalaryAuditRecordDO();
        record.setSalaryMonthlyId(monthlyId);
        record.setSalaryDetailId(detailId);
        record.setAction(action);
        LoginUser operator = auditUserProvider.currentLoginUser();
        if (operator != null) {
            record.setOperatorId(operator.getUser().getId());
            record.setOperatorName(operator.getUsername());
        }
        record.setStatusBefore(statusBefore);
        record.setStatusAfter(statusAfter);
        record.setRemark(limit(remark, 255));
        record.setProofUrl(limit(proofUrl, 255));
        record.setCreatedAt(LocalDateTime.now());
        auditRecordMapper.insert(record);
    }

    private String textOrDefault(String text, String fallback) {
        return StringUtils.hasText(text) ? text : fallback;
    }

    private String valueOrDefault(String value, String fallback) {
        return StringUtils.hasText(value) ? value : fallback;
    }

    private String amount(BigDecimal value) {
        return value == null ? "0" : value.toPlainString();
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private long countByMonthly(List<SalaryMonthlyDetailDO> details, Long monthlyId) {
        return details.stream().filter(detail -> monthlyId.equals(detail.getSalaryMonthlyId())).count();
    }
}

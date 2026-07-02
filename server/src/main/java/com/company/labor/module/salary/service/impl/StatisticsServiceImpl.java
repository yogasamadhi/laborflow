package com.company.labor.module.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.employee.domain.entity.EmployeeDO;
import com.company.labor.module.employee.mapper.EmployeeMapper;
import com.company.labor.module.salary.domain.entity.SalaryAnomalyRecordDO;
import com.company.labor.module.salary.domain.entity.SalaryDetailItemDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDetailDO;
import com.company.labor.module.salary.domain.vo.SalaryAnomalyVO;
import com.company.labor.module.salary.domain.vo.SalaryDetailItemVO;
import com.company.labor.module.salary.domain.vo.SalaryEmployeeHistoryVO;
import com.company.labor.module.salary.domain.vo.SalaryHistoryRecordVO;
import com.company.labor.module.salary.domain.vo.SalaryOverviewVO;
import com.company.labor.module.salary.domain.vo.SalaryTrendVO;
import com.company.labor.module.salary.domain.vo.SalaryYearSummaryVO;
import com.company.labor.module.salary.mapper.SalaryAnomalyRecordMapper;
import com.company.labor.module.salary.mapper.SalaryDetailItemMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyDetailMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyMapper;
import com.company.labor.module.salary.service.StatisticsService;
import com.company.labor.module.system.service.OperationLogService;
import com.company.labor.security.AuditUserProvider;
import com.company.labor.security.LoginUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SalaryMonthlyMapper monthlyMapper;
    private final SalaryMonthlyDetailMapper detailMapper;
    private final SalaryDetailItemMapper detailItemMapper;
    private final EmployeeMapper employeeMapper;
    private final SalaryAnomalyRecordMapper anomalyRecordMapper;
    private final OperationLogService operationLogService;
    private final AuditUserProvider auditUserProvider;

    @Override
    public SalaryOverviewVO overview() {
        SalaryMonthlyDO monthly = latestMonthly();
        if (monthly == null) {
            return new SalaryOverviewVO(BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, BigDecimal.ZERO, 0, 0);
        }
        List<SalaryMonthlyDetailDO> details = details(monthly.getId());
        BigDecimal paidAmount = BigDecimal.ZERO;
        long paidCount = 0;
        for (SalaryMonthlyDetailDO detail : details) {
            if ("paid".equals(detail.getPaymentStatus())) {
                paidAmount = paidAmount.add(nvl(detail.getActualAmount()));
                paidCount++;
            }
        }
        BigDecimal actual = nvl(monthly.getTotalActualAmount());
        return new SalaryOverviewVO(
                nvl(monthly.getTotalPayableAmount()),
                nvl(monthly.getTotalDeductionAmount()),
                actual,
                paidAmount,
                actual.subtract(paidAmount),
                paidCount,
                details.size() - paidCount);
    }

    @Override
    public List<SalaryTrendVO> monthlyTrend() {
        List<SalaryMonthlyDO> rows = monthlyMapper.selectList(new LambdaQueryWrapper<SalaryMonthlyDO>()
                .eq(SalaryMonthlyDO::getDeleted, 0)
                .orderByDesc(SalaryMonthlyDO::getSalaryMonth)
                .last("LIMIT 12"));
        List<SalaryTrendVO> result = new ArrayList<>();
        for (int i = rows.size() - 1; i >= 0; i--) {
            SalaryMonthlyDO row = rows.get(i);
            result.add(new SalaryTrendVO(row.getSalaryMonth(), nvl(row.getTotalActualAmount())));
        }
        return result;
    }

    @Override
    public List<Map<String, Object>> departmentSummary() {
        SalaryMonthlyDO monthly = latestMonthly();
        if (monthly == null) return List.of();
        Map<Long, Map<String, Object>> grouped = new LinkedHashMap<>();
        for (SalaryMonthlyDetailDO detail : details(monthly.getId())) {
            Long key = detail.getDepartmentId() == null ? 0L : detail.getDepartmentId();
            Map<String, Object> row = grouped.computeIfAbsent(key, id -> {
                Map<String, Object> item = new LinkedHashMap<>();
                item.put("departmentId", id);
                item.put("employeeCount", 0);
                item.put("actualAmount", BigDecimal.ZERO);
                return item;
            });
            row.put("employeeCount", (Integer) row.get("employeeCount") + 1);
            row.put("actualAmount", ((BigDecimal) row.get("actualAmount")).add(nvl(detail.getActualAmount())));
        }
        return new ArrayList<>(grouped.values());
    }

    @Override
    public SalaryEmployeeHistoryVO employeeHistory(Long employeeId) {
        EmployeeDO employee = employeeMapper.selectById(employeeId);
        if (employee == null || employee.getDeleted() != null && employee.getDeleted() == 1) {
            throw new BusinessException("员工不存在");
        }
        List<SalaryMonthlyDetailDO> rows = detailMapper.selectList(new LambdaQueryWrapper<SalaryMonthlyDetailDO>()
                .eq(SalaryMonthlyDetailDO::getEmployeeId, employeeId)
                .eq(SalaryMonthlyDetailDO::getDeleted, 0)
                .orderByDesc(SalaryMonthlyDetailDO::getCreatedAt));
        List<SalaryHistoryRecordVO> records = new ArrayList<>();
        for (SalaryMonthlyDetailDO detail : rows) {
            SalaryMonthlyDO monthly = monthlyMapper.selectById(detail.getSalaryMonthlyId());
            SalaryHistoryRecordVO record = toHistoryRecord(monthly, detail);
            records.add(record);
        }
        records.sort(Comparator.comparing(SalaryHistoryRecordVO::getSalaryMonth, Comparator.nullsLast(String::compareTo)).reversed());

        Map<String, SalaryYearAccumulator> yearMap = new LinkedHashMap<>();
        BigDecimal totalActual = BigDecimal.ZERO;
        SalaryHistoryRecordVO latestPaid = null;
        List<SalaryTrendVO> trend = new ArrayList<>();
        for (int i = records.size() - 1; i >= 0; i--) {
            SalaryHistoryRecordVO record = records.get(i);
            totalActual = totalActual.add(nvl(record.getActualAmount()));
            trend.add(new SalaryTrendVO(record.getSalaryMonth(), nvl(record.getActualAmount())));
            if (record.getSalaryMonth() != null && record.getSalaryMonth().length() >= 4) {
                String year = record.getSalaryMonth().substring(0, 4);
                SalaryYearAccumulator acc = yearMap.computeIfAbsent(year, SalaryYearAccumulator::new);
                acc.add(record);
            }
        }
        for (SalaryHistoryRecordVO record : records) {
            if ("paid".equals(record.getPaymentStatus())) {
                latestPaid = record;
                break;
            }
        }

        SalaryEmployeeHistoryVO result = new SalaryEmployeeHistoryVO();
        result.setEmployeeId(employeeId);
        result.setEmployeeName(employee.getName());
        result.setRecords(records);
        result.setMonthlyTrend(trend);
        result.setTotalActualAmount(totalActual);
        result.setLatestPaymentRecord(latestPaid);
        result.setYearlySummaries(yearMap.values().stream().map(SalaryYearAccumulator::toVO).toList());
        return result;
    }

    @Override
    public List<SalaryAnomalyVO> anomalies(double fluctuationRatio) {
        BigDecimal ratioLimit = BigDecimal.valueOf(fluctuationRatio <= 0 ? 0.3 : fluctuationRatio);
        List<SalaryAnomalyVO> result = new ArrayList<>();
        Map<Long, EmployeeDO> employeeMap = new HashMap<>();
        for (EmployeeDO employee : employeeMapper.selectList(new LambdaQueryWrapper<EmployeeDO>().eq(EmployeeDO::getDeleted, 0))) {
            employeeMap.put(employee.getId(), employee);
        }

        List<SalaryMonthlyDO> monthlies = monthlyMapper.selectList(new LambdaQueryWrapper<SalaryMonthlyDO>()
                .eq(SalaryMonthlyDO::getDeleted, 0)
                .orderByAsc(SalaryMonthlyDO::getSalaryMonth));
        Map<String, Integer> monthCounts = new LinkedHashMap<>();
        Map<Long, BigDecimal> previousActualByEmployee = new HashMap<>();
        for (SalaryMonthlyDO monthly : monthlies) {
            monthCounts.merge(monthly.getSalaryMonth(), 1, Integer::sum);
            List<SalaryMonthlyDetailDO> rows = details(monthly.getId());
            for (SalaryMonthlyDetailDO detail : rows) {
                EmployeeDO employee = employeeMap.get(detail.getEmployeeId());
                if (employee != null && !StringUtils.hasText(employee.getBankAccount())) {
                    result.add(anomaly("bank_account_empty", "warning", monthly, detail, employee, "银行卡号为空", null));
                }
                if (nvl(detail.getActualAmount()).compareTo(BigDecimal.ZERO) == 0) {
                    result.add(anomaly("actual_amount_zero", "warning", monthly, detail, employee, "实发工资为 0", null));
                }
                if (nvl(detail.getActualAmount()).compareTo(BigDecimal.ZERO) < 0) {
                    result.add(anomaly("actual_amount_negative", "danger", monthly, detail, employee, "实发工资小于 0", null));
                }
                BigDecimal previous = previousActualByEmployee.get(detail.getEmployeeId());
                if (previous != null && previous.compareTo(BigDecimal.ZERO) != 0) {
                    BigDecimal deltaRatio = nvl(detail.getActualAmount()).subtract(previous).abs()
                            .divide(previous.abs(), 4, RoundingMode.HALF_UP);
                    if (deltaRatio.compareTo(ratioLimit) > 0) {
                        String message = "工资较上月波动超过 " + ratioLimit.multiply(BigDecimal.valueOf(100)).stripTrailingZeros().toPlainString() + "%";
                        result.add(anomaly("monthly_fluctuation", "warning", monthly, detail, employee, message, previous));
                    }
                }
                if ("approved".equals(monthly.getStatus()) && !"paid".equals(detail.getPaymentStatus())) {
                    result.add(anomaly("approved_unpaid", "warning", monthly, detail, employee, "已审核但未发放", null));
                }
                if ("paid".equals(detail.getPaymentStatus()) && !StringUtils.hasText(detail.getPaymentProofUrl())) {
                    result.add(anomaly("paid_without_proof", "warning", monthly, detail, employee, "已发放但无凭证", null));
                }
                previousActualByEmployee.put(detail.getEmployeeId(), nvl(detail.getActualAmount()));
            }
        }
        for (SalaryMonthlyDO monthly : monthlies) {
            if (monthCounts.getOrDefault(monthly.getSalaryMonth(), 0) > 1) {
                result.add(anomaly("duplicate_monthly", "danger", monthly, null, null, "重复创建同月份工资表", null));
            }
        }
        applyAnomalyRecords(result);
        result.sort(Comparator.comparing(SalaryAnomalyVO::getSalaryMonth, Comparator.nullsLast(String::compareTo)).reversed());
        return result;
    }

    @Override
    public void handleAnomaly(String anomalyKey, String status, String remark) {
        if (!StringUtils.hasText(anomalyKey)) {
            throw new BusinessException("异常标识必填");
        }
        if (!List.of("handled", "ignored", "adjustment").contains(status)) {
            throw new BusinessException("处理状态不合法");
        }
        SalaryAnomalyRecordDO record = anomalyRecordMapper.selectOne(new LambdaQueryWrapper<SalaryAnomalyRecordDO>()
                .eq(SalaryAnomalyRecordDO::getDeleted, 0)
                .eq(SalaryAnomalyRecordDO::getAnomalyKey, anomalyKey)
                .last("LIMIT 1"));
        if (record == null) {
            record = new SalaryAnomalyRecordDO();
            record.setAnomalyKey(anomalyKey);
            record.setDeleted(0);
        }
        LoginUser operator = auditUserProvider.currentLoginUser();
        if (operator != null) {
            record.setHandlerId(operator.getUser().getId());
            record.setHandlerName(operator.getUsername());
        }
        record.setStatus(status);
        record.setRemark(limit(remark, 500));
        record.setHandledAt(LocalDateTime.now());
        if (record.getId() == null) {
            anomalyRecordMapper.insert(record);
        } else {
            anomalyRecordMapper.updateById(record);
        }
        operationLogService.record("salary", null, "handle_anomaly", "处理工资异常：" + anomalyKey + " -> " + status);
    }

    private SalaryMonthlyDO latestMonthly() {
        return monthlyMapper.selectOne(new LambdaQueryWrapper<SalaryMonthlyDO>()
                .eq(SalaryMonthlyDO::getDeleted, 0)
                .orderByDesc(SalaryMonthlyDO::getSalaryMonth)
                .last("LIMIT 1"));
    }

    private List<SalaryMonthlyDetailDO> details(Long monthlyId) {
        return detailMapper.selectList(new LambdaQueryWrapper<SalaryMonthlyDetailDO>()
                .eq(SalaryMonthlyDetailDO::getSalaryMonthlyId, monthlyId)
                .eq(SalaryMonthlyDetailDO::getDeleted, 0));
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private SalaryHistoryRecordVO toHistoryRecord(SalaryMonthlyDO monthly, SalaryMonthlyDetailDO detail) {
        SalaryHistoryRecordVO record = new SalaryHistoryRecordVO();
        record.setSalaryMonth(monthly == null ? null : monthly.getSalaryMonth());
        record.setMonthlyId(detail.getSalaryMonthlyId());
        record.setDetailId(detail.getId());
        record.setPayableAmount(detail.getPayableAmount());
        record.setDeductionAmount(detail.getDeductionAmount());
        record.setActualAmount(detail.getActualAmount());
        record.setPaymentStatus(detail.getPaymentStatus());
        record.setPaymentDate(detail.getPaymentDate());
        record.setPaymentMethod(detail.getPaymentMethod());
        record.setPayslipStatus(detail.getPayslipStatus());
        record.setPayslipConfirmedAt(detail.getPayslipConfirmedAt());
        record.setPayslipFeedback(detail.getPayslipFeedback());
        record.setPayslipFeedbackAt(detail.getPayslipFeedbackAt());
        record.setRemark(detail.getRemark());
        List<SalaryDetailItemVO> earnings = new ArrayList<>();
        List<SalaryDetailItemVO> deductions = new ArrayList<>();
        for (SalaryDetailItemDO item : detailItemMapper.selectList(new LambdaQueryWrapper<SalaryDetailItemDO>().eq(SalaryDetailItemDO::getSalaryDetailId, detail.getId()))) {
            SalaryDetailItemVO vo = new SalaryDetailItemVO(item.getSalaryItemName(), item.getSalaryItemType(), item.getAmount(), item.getRemark());
            if ("deduction".equals(item.getSalaryItemType())) {
                deductions.add(vo);
            } else {
                earnings.add(vo);
            }
        }
        record.setEarningItems(earnings);
        record.setDeductionItems(deductions);
        return record;
    }

    private SalaryAnomalyVO anomaly(String type, String level, SalaryMonthlyDO monthly, SalaryMonthlyDetailDO detail, EmployeeDO employee, String message, BigDecimal compareAmount) {
        SalaryAnomalyVO vo = new SalaryAnomalyVO();
        vo.setAnomalyKey(anomalyKey(type, monthly, detail, employee));
        vo.setType(type);
        vo.setLevel(level);
        vo.setSalaryMonth(monthly == null ? null : monthly.getSalaryMonth());
        vo.setMonthlyId(monthly == null ? null : monthly.getId());
        vo.setDetailId(detail == null ? null : detail.getId());
        vo.setEmployeeId(detail == null ? null : detail.getEmployeeId());
        vo.setEmployeeName(employee == null ? null : employee.getName());
        vo.setActualAmount(detail == null ? null : detail.getActualAmount());
        vo.setCompareAmount(compareAmount);
        vo.setMessage(message);
        vo.setProcessStatus("open");
        return vo;
    }

    private void applyAnomalyRecords(List<SalaryAnomalyVO> anomalies) {
        if (anomalies.isEmpty()) {
            return;
        }
        Map<String, SalaryAnomalyRecordDO> records = new HashMap<>();
        for (SalaryAnomalyRecordDO record : anomalyRecordMapper.selectList(new LambdaQueryWrapper<SalaryAnomalyRecordDO>()
                .eq(SalaryAnomalyRecordDO::getDeleted, 0))) {
            records.put(record.getAnomalyKey(), record);
        }
        for (SalaryAnomalyVO anomaly : anomalies) {
            SalaryAnomalyRecordDO record = records.get(anomaly.getAnomalyKey());
            if (record == null) {
                continue;
            }
            anomaly.setProcessStatus(record.getStatus());
            anomaly.setProcessRemark(record.getRemark());
            anomaly.setProcessedBy(record.getHandlerName());
            anomaly.setProcessedAt(record.getHandledAt());
        }
    }

    private String anomalyKey(String type, SalaryMonthlyDO monthly, SalaryMonthlyDetailDO detail, EmployeeDO employee) {
        return type
                + "|" + (monthly == null ? 0 : monthly.getId())
                + "|" + (detail == null ? 0 : detail.getId())
                + "|" + (employee == null ? 0 : employee.getId());
    }

    private String limit(String value, int maxLength) {
        if (!StringUtils.hasText(value) || value.length() <= maxLength) {
            return value;
        }
        return value.substring(0, maxLength);
    }

    private static class SalaryYearAccumulator {
        private final String year;
        private BigDecimal payableAmount = BigDecimal.ZERO;
        private BigDecimal deductionAmount = BigDecimal.ZERO;
        private BigDecimal actualAmount = BigDecimal.ZERO;
        private long paidCount;
        private long unpaidCount;

        private SalaryYearAccumulator(String year) {
            this.year = year;
        }

        private void add(SalaryHistoryRecordVO record) {
            payableAmount = payableAmount.add(record.getPayableAmount() == null ? BigDecimal.ZERO : record.getPayableAmount());
            deductionAmount = deductionAmount.add(record.getDeductionAmount() == null ? BigDecimal.ZERO : record.getDeductionAmount());
            actualAmount = actualAmount.add(record.getActualAmount() == null ? BigDecimal.ZERO : record.getActualAmount());
            if ("paid".equals(record.getPaymentStatus())) {
                paidCount++;
            } else {
                unpaidCount++;
            }
        }

        private SalaryYearSummaryVO toVO() {
            return new SalaryYearSummaryVO(year, payableAmount, deductionAmount, actualAmount, paidCount, unpaidCount);
        }
    }
}

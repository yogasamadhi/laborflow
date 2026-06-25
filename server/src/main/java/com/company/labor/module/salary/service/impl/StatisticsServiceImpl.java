package com.company.labor.module.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDetailDO;
import com.company.labor.module.salary.domain.vo.SalaryOverviewVO;
import com.company.labor.module.salary.domain.vo.SalaryTrendVO;
import com.company.labor.module.salary.mapper.SalaryMonthlyDetailMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyMapper;
import com.company.labor.module.salary.service.StatisticsService;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class StatisticsServiceImpl implements StatisticsService {
    private final SalaryMonthlyMapper monthlyMapper;
    private final SalaryMonthlyDetailMapper detailMapper;

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
    public List<Map<String, Object>> employeeHistory(Long employeeId) {
        List<SalaryMonthlyDetailDO> rows = detailMapper.selectList(new LambdaQueryWrapper<SalaryMonthlyDetailDO>()
                .eq(SalaryMonthlyDetailDO::getEmployeeId, employeeId)
                .eq(SalaryMonthlyDetailDO::getDeleted, 0)
                .orderByDesc(SalaryMonthlyDetailDO::getCreatedAt));
        List<Map<String, Object>> result = new ArrayList<>();
        for (SalaryMonthlyDetailDO detail : rows) {
            SalaryMonthlyDO monthly = monthlyMapper.selectById(detail.getSalaryMonthlyId());
            Map<String, Object> row = new LinkedHashMap<>();
            row.put("salaryMonth", monthly == null ? null : monthly.getSalaryMonth());
            row.put("monthlyId", detail.getSalaryMonthlyId());
            row.put("detailId", detail.getId());
            row.put("actualAmount", detail.getActualAmount());
            row.put("paymentStatus", detail.getPaymentStatus());
            row.put("paymentDate", detail.getPaymentDate());
            result.add(row);
        }
        return result;
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
}


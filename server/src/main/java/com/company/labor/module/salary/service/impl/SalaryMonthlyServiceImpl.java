package com.company.labor.module.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.employee.domain.entity.EmployeeDO;
import com.company.labor.module.employee.mapper.EmployeeMapper;
import com.company.labor.module.salary.domain.dto.BatchSalaryPaymentDTO;
import com.company.labor.module.salary.domain.dto.SalaryPaymentDTO;
import com.company.labor.module.salary.domain.entity.SalaryDetailItemDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDetailDO;
import com.company.labor.module.salary.domain.entity.SalaryProfileDO;
import com.company.labor.module.salary.domain.entity.SalaryProfileItemDO;
import com.company.labor.module.salary.mapper.SalaryDetailItemMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyDetailMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyMapper;
import com.company.labor.module.salary.mapper.SalaryProfileItemMapper;
import com.company.labor.module.salary.mapper.SalaryProfileMapper;
import com.company.labor.module.salary.service.SalaryMonthlyService;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
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

    @Override
    @Transactional
    public SalaryMonthlyDO createMonthly(SalaryMonthlyDO monthly) {
        if (!StringUtils.hasText(monthly.getSalaryMonth())) {
            throw new BusinessException("工资月份必填");
        }
        if (!StringUtils.hasText(monthly.getTitle())) {
            monthly.setTitle(monthly.getSalaryMonth() + " 工资表");
        }
        monthly.setStatus("draft");
        monthly.setTotalEmployeeCount(0);
        monthly.setTotalPayableAmount(BigDecimal.ZERO);
        monthly.setTotalDeductionAmount(BigDecimal.ZERO);
        monthly.setTotalActualAmount(BigDecimal.ZERO);
        save(monthly);
        return monthly;
    }

    @Override
    @Transactional
    public void generate(Long monthlyId) {
        SalaryMonthlyDO monthly = requireMonthly(monthlyId);
        requireStatus(monthly, "draft");
        List<SalaryMonthlyDetailDO> oldDetails = detailMapper.selectList(new LambdaQueryWrapper<SalaryMonthlyDetailDO>().eq(SalaryMonthlyDetailDO::getSalaryMonthlyId, monthlyId));
        for (SalaryMonthlyDetailDO old : oldDetails) {
            detailItemMapper.delete(new LambdaUpdateWrapper<SalaryDetailItemDO>().eq(SalaryDetailItemDO::getSalaryDetailId, old.getId()));
        }
        detailMapper.delete(new LambdaUpdateWrapper<SalaryMonthlyDetailDO>().eq(SalaryMonthlyDetailDO::getSalaryMonthlyId, monthlyId));

        List<EmployeeDO> employees = employeeMapper.selectList(new LambdaQueryWrapper<EmployeeDO>().eq(EmployeeDO::getStatus, "active").eq(EmployeeDO::getDeleted, 0));
        for (EmployeeDO employee : employees) {
            SalaryProfileDO profile = latestProfile(employee.getId());
            if (profile == null) {
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
            for (SalaryProfileItemDO profileItem : profileItems) {
                SalaryDetailItemDO detailItem = new SalaryDetailItemDO();
                detailItem.setSalaryDetailId(detail.getId());
                detailItem.setSalaryItemId(profileItem.getSalaryItemId());
                detailItem.setSalaryItemName(profileItem.getSalaryItemName());
                detailItem.setSalaryItemType(profileItem.getSalaryItemType());
                detailItem.setAmount(profileItem.getAmount());
                detailItem.setRemark(profileItem.getRemark());
                detailItemMapper.insert(detailItem);
            }
        }
        summarize(monthlyId);
    }

    @Override
    @Transactional
    public void submit(Long monthlyId) {
        SalaryMonthlyDO monthly = requireMonthly(monthlyId);
        requireStatus(monthly, "draft");
        monthly.setStatus("pending_approval");
        updateById(monthly);
    }

    @Override
    @Transactional
    public void approve(Long monthlyId, Long userId) {
        SalaryMonthlyDO monthly = requireMonthly(monthlyId);
        requireStatus(monthly, "pending_approval");
        monthly.setStatus("approved");
        monthly.setApprovedBy(userId);
        monthly.setApprovedAt(LocalDateTime.now());
        updateById(monthly);
    }

    @Override
    @Transactional
    public void reject(Long monthlyId) {
        SalaryMonthlyDO monthly = requireMonthly(monthlyId);
        requireStatus(monthly, "pending_approval");
        monthly.setStatus("draft");
        updateById(monthly);
    }

    @Override
    @Transactional
    public void markMonthlyPaid(Long monthlyId, SalaryPaymentDTO dto) {
        SalaryMonthlyDO monthly = requireMonthly(monthlyId);
        requireStatus(monthly, "approved");
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
        requireStatus(monthly, "draft");
        detail.setId(detailId);
        detail.setSalaryMonthlyId(old.getSalaryMonthlyId());
        detailMapper.updateById(detail);
        summarize(old.getSalaryMonthlyId());
        return detailMapper.selectById(detailId);
    }

    @Override
    @Transactional
    public void markDetailPaid(Long detailId, SalaryPaymentDTO dto) {
        SalaryMonthlyDetailDO detail = detailMapper.selectById(detailId);
        if (detail == null) throw new BusinessException("工资明细不存在");
        SalaryMonthlyDO monthly = requireMonthly(detail.getSalaryMonthlyId());
        requireStatus(monthly, "approved");
        applyPayment(detail, dto);
        detailMapper.updateById(detail);
        refreshPaidStatus(monthly.getId());
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
        for (SalaryMonthlyDetailDO detail : details) {
            SalaryMonthlyDO monthly = requireMonthly(detail.getSalaryMonthlyId());
            requireStatus(monthly, "approved");
            applyPayment(detail, dto);
            detailMapper.updateById(detail);
            refreshPaidStatus(detail.getSalaryMonthlyId());
        }
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

    private SalaryMonthlyDO requireMonthly(Long monthlyId) {
        SalaryMonthlyDO monthly = getById(monthlyId);
        if (monthly == null || monthly.getDeleted() != null && monthly.getDeleted() == 1) {
            throw new BusinessException("工资表不存在");
        }
        return monthly;
    }

    private void requireStatus(SalaryMonthlyDO monthly, String status) {
        if (!status.equals(monthly.getStatus())) {
            throw new BusinessException("当前状态不允许操作");
        }
    }

    private void applyPayment(SalaryMonthlyDetailDO detail, SalaryPaymentDTO dto) {
        detail.setPaymentStatus("paid");
        detail.setPaymentDate(dto.getPaymentDate() == null ? LocalDate.now() : dto.getPaymentDate());
        detail.setPaymentMethod(StringUtils.hasText(dto.getPaymentMethod()) ? dto.getPaymentMethod() : "bank_transfer");
        detail.setPaymentProofUrl(dto.getPaymentProofUrl());
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
            monthly.setStatus("paid");
            monthly.setPaidAt(LocalDateTime.now());
            updateById(monthly);
        }
    }
}


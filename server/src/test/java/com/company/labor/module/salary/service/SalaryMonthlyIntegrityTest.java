package com.company.labor.module.salary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.compensation.mapper.AttendanceRecordMapper;
import com.company.labor.module.compensation.mapper.PerformanceScoreMapper;
import com.company.labor.module.compensation.mapper.ProjectBonusAllocationMapper;
import com.company.labor.module.compensation.mapper.SalaryDeductionRecordMapper;
import com.company.labor.module.compensation.mapper.SalarySourceRecordMapper;
import com.company.labor.module.employee.mapper.EmployeeMapper;
import com.company.labor.module.file.service.FileBindingGuard;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDetailDO;
import com.company.labor.module.salary.domain.entity.SalaryProfileDO;
import com.company.labor.module.salary.mapper.SalaryAuditRecordMapper;
import com.company.labor.module.salary.mapper.SalaryDetailItemMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyDetailMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyMapper;
import com.company.labor.module.salary.mapper.SalaryProfileItemMapper;
import com.company.labor.module.salary.mapper.SalaryProfileMapper;
import com.company.labor.module.salary.service.impl.SalaryMonthlyServiceImpl;
import com.company.labor.module.system.service.OperationLogService;
import com.company.labor.security.AuditUserProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

class SalaryMonthlyIntegrityTest {
    private final SalaryMonthlyMapper monthlyMapper = mock(SalaryMonthlyMapper.class);
    private final SalaryProfileMapper profileMapper = mock(SalaryProfileMapper.class);
    private final SalaryMonthlyDetailMapper detailMapper = mock(SalaryMonthlyDetailMapper.class);
    private final SalaryMonthlyServiceImpl service = service();

    @Test
    void recalculatesActualAmountFromPayableAndDeduction() {
        SalaryMonthlyDO monthly = monthly("draft");
        SalaryMonthlyDetailDO old = detail("1000.00", "0.00", "1000.00");
        SalaryMonthlyDetailDO update = detail("1200.00", "200.00", "1.00");
        when(monthlyMapper.selectById(1L)).thenReturn(monthly);
        when(detailMapper.selectById(2L)).thenReturn(old, update, update);
        when(detailMapper.selectList(any())).thenReturn(List.of(update));

        SalaryMonthlyDetailDO result = service.updateDetail(2L, update);

        assertThat(result.getActualAmount()).isEqualByComparingTo("1000.00");
        assertThat(monthly.getTotalActualAmount()).isEqualByComparingTo("1000.00");
        verify(detailMapper).updateById(update);
    }

    @Test
    void rejectsDeductionGreaterThanPayable() {
        when(monthlyMapper.selectById(1L)).thenReturn(monthly("draft"));
        when(detailMapper.selectById(2L)).thenReturn(detail("1000.00", "0.00", "1000.00"));
        SalaryMonthlyDetailDO update = detail("100.00", "200.00", "0.00");

        assertThatThrownBy(() -> service.updateDetail(2L, update))
                .isInstanceOf(BusinessException.class)
                .hasMessage("扣款金额不能大于应发金额");

        verify(detailMapper, never()).updateById(any(SalaryMonthlyDetailDO.class));
    }

    @Test
    void rejectsSubmittingOrApprovingEmptyMonthly() {
        SalaryMonthlyDO draft = monthly("draft");
        when(monthlyMapper.selectById(1L)).thenReturn(draft);
        when(detailMapper.selectList(any())).thenReturn(List.of());

        assertThatThrownBy(() -> service.submit(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("工资表没有有效明细，不能提交或审核");
        assertThat(draft.getStatus()).isEqualTo("draft");

        SalaryMonthlyDO pending = monthly("pending_approval");
        when(monthlyMapper.selectById(2L)).thenReturn(pending);
        assertThatThrownBy(() -> service.approve(2L, 10L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("工资表没有有效明细，不能提交或审核");
        assertThat(pending.getStatus()).isEqualTo("pending_approval");
    }

    @Test
    void rejectsSubmittingStoredFormulaMismatch() {
        SalaryMonthlyDO draft = monthly("draft");
        draft.setTotalEmployeeCount(1);
        draft.setTotalPayableAmount(new BigDecimal("1000.00"));
        draft.setTotalDeductionAmount(new BigDecimal("100.00"));
        draft.setTotalActualAmount(new BigDecimal("1000.00"));
        when(monthlyMapper.selectById(1L)).thenReturn(draft);
        when(detailMapper.selectList(any())).thenReturn(List.of(detail("1000.00", "100.00", "1000.00")));

        assertThatThrownBy(() -> service.submit(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("实发金额不符合应发减扣款公式");
        assertThat(draft.getStatus()).isEqualTo("draft");
    }

    @Test
    void rejectsSubmittingHeaderAndDetailTotalMismatch() {
        SalaryMonthlyDO draft = monthly("draft");
        draft.setTotalEmployeeCount(1);
        draft.setTotalPayableAmount(new BigDecimal("999.00"));
        draft.setTotalDeductionAmount(new BigDecimal("100.00"));
        draft.setTotalActualAmount(new BigDecimal("899.00"));
        when(monthlyMapper.selectById(1L)).thenReturn(draft);
        when(detailMapper.selectList(any())).thenReturn(List.of(detail("1000.00", "100.00", "900.00")));

        assertThatThrownBy(() -> service.submit(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessage("工资表汇总与明细不一致，请重新生成或修正后再提交");
        assertThat(draft.getStatus()).isEqualTo("draft");
    }

    @Test
    @SuppressWarnings("unchecked")
    void selectsLatestProfileEffectiveByPayrollMonthAndTreatsNullAsBaseline() {
        SalaryProfileDO baseline = profile(11L, 100L, null);
        SalaryProfileDO effective = profile(12L, 100L, LocalDate.of(2031, 12, 1));
        SalaryProfileDO future = profile(13L, 100L, LocalDate.of(2032, 1, 1));
        SalaryProfileDO otherBaseline = profile(14L, 200L, null);
        when(profileMapper.selectList(any())).thenReturn(List.of(future, baseline, effective, otherBaseline));

        List<SalaryProfileDO> selected = ReflectionTestUtils.invokeMethod(service, "latestActiveProfiles", "2031-12");

        assertThat(selected).extracting(SalaryProfileDO::getId).containsExactly(12L, 14L);
    }

    private SalaryMonthlyServiceImpl service() {
        SalaryMonthlyServiceImpl service = new SalaryMonthlyServiceImpl(
                mock(EmployeeMapper.class),
                profileMapper,
                mock(SalaryProfileItemMapper.class),
                detailMapper,
                mock(SalaryDetailItemMapper.class),
                mock(SalaryAuditRecordMapper.class),
                mock(OperationLogService.class),
                mock(AuditUserProvider.class),
                mock(AttendanceRecordMapper.class),
                mock(PerformanceScoreMapper.class),
                mock(ProjectBonusAllocationMapper.class),
                mock(SalaryDeductionRecordMapper.class),
                mock(SalarySourceRecordMapper.class),
                mock(FileBindingGuard.class));
        ReflectionTestUtils.setField(service, "baseMapper", monthlyMapper);
        return service;
    }

    private SalaryMonthlyDO monthly(String status) {
        SalaryMonthlyDO monthly = new SalaryMonthlyDO();
        monthly.setId("pending_approval".equals(status) ? 2L : 1L);
        monthly.setStatus(status);
        monthly.setDeleted(0);
        monthly.setTotalEmployeeCount(0);
        monthly.setTotalPayableAmount(BigDecimal.ZERO);
        monthly.setTotalDeductionAmount(BigDecimal.ZERO);
        monthly.setTotalActualAmount(BigDecimal.ZERO);
        return monthly;
    }

    private SalaryMonthlyDetailDO detail(String payable, String deduction, String actual) {
        SalaryMonthlyDetailDO detail = new SalaryMonthlyDetailDO();
        detail.setId(2L);
        detail.setSalaryMonthlyId(1L);
        detail.setPayableAmount(new BigDecimal(payable));
        detail.setDeductionAmount(new BigDecimal(deduction));
        detail.setActualAmount(new BigDecimal(actual));
        detail.setPaymentStatus("unpaid");
        detail.setDeleted(0);
        return detail;
    }

    private SalaryProfileDO profile(Long id, Long employeeId, LocalDate effectiveDate) {
        SalaryProfileDO profile = new SalaryProfileDO();
        profile.setId(id);
        profile.setEmployeeId(employeeId);
        profile.setEffectiveDate(effectiveDate);
        profile.setStatus("active");
        profile.setDeleted(0);
        return profile;
    }
}

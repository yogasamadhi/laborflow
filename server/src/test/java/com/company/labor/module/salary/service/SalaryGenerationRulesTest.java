package com.company.labor.module.salary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verifyNoInteractions;
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
import com.company.labor.module.salary.mapper.SalaryAuditRecordMapper;
import com.company.labor.module.salary.mapper.SalaryDetailItemMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyDetailMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyMapper;
import com.company.labor.module.salary.mapper.SalaryProfileItemMapper;
import com.company.labor.module.salary.mapper.SalaryProfileMapper;
import com.company.labor.module.salary.service.impl.SalaryMonthlyServiceImpl;
import com.company.labor.module.system.service.OperationLogService;
import com.company.labor.security.AuditUserProvider;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class SalaryGenerationRulesTest {
    @Test
    void generateRejectsWhenAllRulesDisabled() {
        SalaryMonthlyMapper monthlyMapper = mock(SalaryMonthlyMapper.class);
        SalaryProfileMapper salaryProfileMapper = mock(SalaryProfileMapper.class);
        SalaryMonthlyDetailMapper detailMapper = mock(SalaryMonthlyDetailMapper.class);
        SalaryDetailItemMapper detailItemMapper = mock(SalaryDetailItemMapper.class);
        SalarySourceRecordMapper sourceRecordMapper = mock(SalarySourceRecordMapper.class);
        SalaryMonthlyServiceImpl service = new SalaryMonthlyServiceImpl(
                mock(EmployeeMapper.class),
                salaryProfileMapper,
                mock(SalaryProfileItemMapper.class),
                detailMapper,
                detailItemMapper,
                mock(SalaryAuditRecordMapper.class),
                mock(OperationLogService.class),
                mock(AuditUserProvider.class),
                mock(AttendanceRecordMapper.class),
                mock(PerformanceScoreMapper.class),
                mock(ProjectBonusAllocationMapper.class),
                mock(SalaryDeductionRecordMapper.class),
                sourceRecordMapper,
                mock(FileBindingGuard.class));
        ReflectionTestUtils.setField(service, "baseMapper", monthlyMapper);
        when(monthlyMapper.selectById(1L)).thenReturn(disabledMonthly());

        assertThatThrownBy(() -> service.generate(1L))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("至少启用一项工资生成规则");

        verifyNoInteractions(salaryProfileMapper, detailMapper, detailItemMapper, sourceRecordMapper);
    }

    @Test
    void updateMonthlyRejectsSubmittedMonthly() {
        SalaryMonthlyMapper monthlyMapper = mock(SalaryMonthlyMapper.class);
        SalaryMonthlyServiceImpl service = newService(
                monthlyMapper,
                mock(SalaryProfileMapper.class),
                mock(SalaryMonthlyDetailMapper.class),
                mock(SalaryDetailItemMapper.class),
                mock(SalarySourceRecordMapper.class));
        when(monthlyMapper.selectById(1L)).thenReturn(monthlyWithStatus("pending_approval"));

        assertThatThrownBy(() -> service.updateMonthly(1L, new SalaryMonthlyDO()))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("工资表已提交或锁定")
                .hasMessageContaining("冲正");

        verify(monthlyMapper, never()).updateById(any(SalaryMonthlyDO.class));
    }

    @Test
    void confirmPayslipUpdatesApprovedDetail() {
        SalaryMonthlyMapper monthlyMapper = mock(SalaryMonthlyMapper.class);
        SalaryMonthlyDetailMapper detailMapper = mock(SalaryMonthlyDetailMapper.class);
        SalaryMonthlyServiceImpl service = newService(
                monthlyMapper,
                mock(SalaryProfileMapper.class),
                detailMapper,
                mock(SalaryDetailItemMapper.class),
                mock(SalarySourceRecordMapper.class));
        when(monthlyMapper.selectById(1L)).thenReturn(monthlyWithStatus("approved"));
        SalaryMonthlyDetailDO detail = new SalaryMonthlyDetailDO();
        detail.setId(2L);
        detail.setSalaryMonthlyId(1L);
        detail.setDeleted(0);
        when(detailMapper.selectById(2L)).thenReturn(detail);

        service.confirmPayslip(2L);

        ArgumentCaptor<SalaryMonthlyDetailDO> captor = ArgumentCaptor.forClass(SalaryMonthlyDetailDO.class);
        verify(detailMapper).updateById(captor.capture());
        assertThat(captor.getValue().getPayslipStatus()).isEqualTo("confirmed");
        assertThat(captor.getValue().getPayslipConfirmedAt()).isNotNull();
    }

    private SalaryMonthlyServiceImpl newService(
            SalaryMonthlyMapper monthlyMapper,
            SalaryProfileMapper salaryProfileMapper,
            SalaryMonthlyDetailMapper detailMapper,
            SalaryDetailItemMapper detailItemMapper,
            SalarySourceRecordMapper sourceRecordMapper) {
        SalaryMonthlyServiceImpl service = new SalaryMonthlyServiceImpl(
                mock(EmployeeMapper.class),
                salaryProfileMapper,
                mock(SalaryProfileItemMapper.class),
                detailMapper,
                detailItemMapper,
                mock(SalaryAuditRecordMapper.class),
                mock(OperationLogService.class),
                mock(AuditUserProvider.class),
                mock(AttendanceRecordMapper.class),
                mock(PerformanceScoreMapper.class),
                mock(ProjectBonusAllocationMapper.class),
                mock(SalaryDeductionRecordMapper.class),
                sourceRecordMapper,
                mock(FileBindingGuard.class));
        ReflectionTestUtils.setField(service, "baseMapper", monthlyMapper);
        return service;
    }

    private SalaryMonthlyDO disabledMonthly() {
        SalaryMonthlyDO monthly = monthlyWithStatus("draft");
        monthly.setIncludeFixedSalary(0);
        monthly.setIncludeAttendanceDeduction(0);
        monthly.setIncludePerformanceSalary(0);
        monthly.setIncludeProjectBonus(0);
        return monthly;
    }

    private SalaryMonthlyDO monthlyWithStatus(String status) {
        SalaryMonthlyDO monthly = new SalaryMonthlyDO();
        monthly.setId(1L);
        monthly.setStatus(status);
        monthly.setDeleted(0);
        return monthly;
    }
}

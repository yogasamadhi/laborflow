package com.company.labor.module.compensation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

import com.company.labor.module.compensation.domain.entity.SalaryDeductionRecordDO;
import com.company.labor.module.compensation.mapper.AttendanceRecordMapper;
import com.company.labor.module.compensation.mapper.PerformanceIndicatorMapper;
import com.company.labor.module.compensation.mapper.PerformancePlanMapper;
import com.company.labor.module.compensation.mapper.PerformanceScoreMapper;
import com.company.labor.module.compensation.mapper.ProjectBonusAllocationMapper;
import com.company.labor.module.compensation.mapper.ProjectBonusPoolMapper;
import com.company.labor.module.compensation.mapper.SalaryDeductionRecordMapper;
import com.company.labor.module.compensation.service.impl.CompensationServiceImpl;
import com.company.labor.module.project.service.ProjectManagementService;
import com.company.labor.module.salary.mapper.SalaryMonthlyMapper;
import com.company.labor.module.system.service.OperationLogService;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SalaryDeductionRecordServiceTest {
    @Test
    void salaryDeductionDefaultsItemByDeductionType() {
        SalaryDeductionRecordMapper mapper = mock(SalaryDeductionRecordMapper.class);
        CompensationServiceImpl service = new CompensationServiceImpl(
                mock(AttendanceRecordMapper.class),
                mock(PerformanceIndicatorMapper.class),
                mock(PerformancePlanMapper.class),
                mock(PerformanceScoreMapper.class),
                mock(ProjectBonusPoolMapper.class),
                mock(ProjectBonusAllocationMapper.class),
                mapper,
                mock(ProjectManagementService.class),
                mock(SalaryMonthlyMapper.class),
                mock(OperationLogService.class));
        SalaryDeductionRecordDO record = new SalaryDeductionRecordDO();
        record.setEmployeeId(100L);
        record.setDeductionMonth("2026-06");
        record.setDeductionType("housing_fund");
        record.setAmount(new BigDecimal("300.00"));

        service.saveSalaryDeduction(record);

        ArgumentCaptor<SalaryDeductionRecordDO> captor = ArgumentCaptor.forClass(SalaryDeductionRecordDO.class);
        verify(mapper).insert(captor.capture());
        SalaryDeductionRecordDO saved = captor.getValue();
        assertThat(saved.getSalaryItemId()).isEqualTo(2301L);
        assertThat(saved.getSalaryItemName()).isEqualTo("公积金扣款");
        assertThat(saved.getStatus()).isEqualTo("active");
        assertThat(saved.getDeleted()).isZero();
        assertThat(saved.getAmount()).isEqualByComparingTo("300.00");
    }
}

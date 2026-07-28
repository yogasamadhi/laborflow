package com.company.labor.module.compensation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.compensation.domain.entity.ProjectBonusAllocationDO;
import com.company.labor.module.compensation.domain.entity.ProjectBonusPoolDO;
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

class BonusAllocationLifecycleTest {
    private final ProjectBonusPoolMapper poolMapper = mock(ProjectBonusPoolMapper.class);
    private final ProjectBonusAllocationMapper allocationMapper = mock(ProjectBonusAllocationMapper.class);
    private final CompensationServiceImpl service = new CompensationServiceImpl(
            mock(AttendanceRecordMapper.class),
            mock(PerformanceIndicatorMapper.class),
            mock(PerformancePlanMapper.class),
            mock(PerformanceScoreMapper.class),
            poolMapper,
            allocationMapper,
            mock(SalaryDeductionRecordMapper.class),
            mock(ProjectManagementService.class),
            mock(SalaryMonthlyMapper.class),
            mock(OperationLogService.class));

    @Test
    void rejectsAllocationWhenPoolIsNotReleased() {
        when(poolMapper.selectById(1L)).thenReturn(pool("draft"));

        assertThatThrownBy(() -> service.saveBonusAllocation(allocation()))
                .isInstanceOf(BusinessException.class)
                .hasMessage("项目奖金池未发布");
    }

    @Test
    void savesAndConfirmsAllocationOnlyForReleasedPool() {
        when(poolMapper.selectById(1L)).thenReturn(pool("released"));
        ProjectBonusAllocationDO allocation = allocation();

        ProjectBonusAllocationDO saved = service.saveBonusAllocation(allocation);

        assertThat(saved.getStatus()).isEqualTo("allocated");
        assertThat(saved.getProjectId()).isEqualTo(9L);
        verify(allocationMapper).insert(saved);

        saved.setId(2L);
        when(allocationMapper.selectById(2L)).thenReturn(saved);
        service.confirmBonusAllocation(2L);
        assertThat(saved.getStatus()).isEqualTo("confirmed");
        verify(allocationMapper).updateById(saved);
    }

    private ProjectBonusPoolDO pool(String status) {
        ProjectBonusPoolDO pool = new ProjectBonusPoolDO();
        pool.setId(1L);
        pool.setProjectId(9L);
        pool.setBonusPoolAmount(new BigDecimal("1000.00"));
        pool.setReleaseStatus(status);
        pool.setDeleted(0);
        return pool;
    }

    private ProjectBonusAllocationDO allocation() {
        ProjectBonusAllocationDO allocation = new ProjectBonusAllocationDO();
        allocation.setBonusPoolId(1L);
        allocation.setEmployeeId(20L);
        allocation.setPaymentMonth("2031-12");
        allocation.setAllocationRatio(new BigDecimal("0.10"));
        return allocation;
    }
}

package com.company.labor.module.compensation.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.labor.module.compensation.domain.entity.ProjectBonusPoolDO;
import com.company.labor.module.compensation.mapper.AttendanceRecordMapper;
import com.company.labor.module.compensation.mapper.PerformanceIndicatorMapper;
import com.company.labor.module.compensation.mapper.PerformancePlanMapper;
import com.company.labor.module.compensation.mapper.PerformanceScoreMapper;
import com.company.labor.module.compensation.mapper.ProjectBonusAllocationMapper;
import com.company.labor.module.compensation.mapper.ProjectBonusPoolMapper;
import com.company.labor.module.compensation.mapper.SalaryDeductionRecordMapper;
import com.company.labor.module.compensation.service.impl.CompensationServiceImpl;
import com.company.labor.module.project.domain.vo.ProjectDashboardVO;
import com.company.labor.module.project.service.ProjectManagementService;
import com.company.labor.module.salary.mapper.SalaryMonthlyMapper;
import com.company.labor.module.system.service.OperationLogService;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class CompensationSnapshotTest {
    @Test
    void bonusPoolStoresDashboardSnapshotWhenSaved() {
        ProjectBonusPoolMapper bonusPoolMapper = mock(ProjectBonusPoolMapper.class);
        ProjectManagementService projectManagementService = mock(ProjectManagementService.class);
        when(projectManagementService.dashboard(1L)).thenReturn(dashboard());
        CompensationServiceImpl service = new CompensationServiceImpl(
                mock(AttendanceRecordMapper.class),
                mock(PerformanceIndicatorMapper.class),
                mock(PerformancePlanMapper.class),
                mock(PerformanceScoreMapper.class),
                bonusPoolMapper,
                mock(ProjectBonusAllocationMapper.class),
                mock(SalaryDeductionRecordMapper.class),
                projectManagementService,
                mock(SalaryMonthlyMapper.class),
                mock(OperationLogService.class));
        ProjectBonusPoolDO pool = new ProjectBonusPoolDO();
        pool.setProjectId(1L);
        pool.setBonusRatio(new BigDecimal("0.10"));

        service.saveBonusPool(pool);

        ArgumentCaptor<ProjectBonusPoolDO> captor = ArgumentCaptor.forClass(ProjectBonusPoolDO.class);
        verify(bonusPoolMapper).insert(captor.capture());
        ProjectBonusPoolDO saved = captor.getValue();
        assertThat(saved.getFinalGrossProfit()).isEqualByComparingTo("1000.00");
        assertThat(saved.getBonusPoolAmount()).isEqualByComparingTo("100.0000");
        assertThat(saved.getDashboardSnapshot()).contains("项目:项目A", "毛利:1000.00", "风险数:1");
    }

    private ProjectDashboardVO dashboard() {
        ProjectDashboardVO vo = new ProjectDashboardVO();
        vo.setProjectName("项目A");
        vo.setContractAmount(new BigDecimal("5000.00"));
        vo.setCompletedOutputValue(new BigDecimal("3000.00"));
        vo.setTotalCostAmount(new BigDecimal("2000.00"));
        vo.setGrossProfit(new BigDecimal("1000.00"));
        vo.setReceivableAmount(new BigDecimal("4000.00"));
        vo.setReceivedAmount(new BigDecimal("2500.00"));
        vo.setRisks(List.of(new com.company.labor.module.project.domain.vo.ProjectRiskVO("risk", "warning", "测试风险")));
        return vo;
    }
}

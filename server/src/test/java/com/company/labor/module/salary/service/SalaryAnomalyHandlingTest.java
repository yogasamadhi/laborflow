package com.company.labor.module.salary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.labor.module.employee.mapper.EmployeeMapper;
import com.company.labor.module.salary.domain.entity.SalaryAnomalyRecordDO;
import com.company.labor.module.salary.mapper.SalaryAnomalyRecordMapper;
import com.company.labor.module.salary.mapper.SalaryDetailItemMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyDetailMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyMapper;
import com.company.labor.module.salary.service.impl.StatisticsServiceImpl;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.service.OperationLogService;
import com.company.labor.security.AuditUserProvider;
import com.company.labor.security.LoginUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class SalaryAnomalyHandlingTest {
    @Test
    void handleAnomalyCreatesAuditableRecord() {
        SalaryAnomalyRecordMapper anomalyRecordMapper = mock(SalaryAnomalyRecordMapper.class);
        AuditUserProvider auditUserProvider = mock(AuditUserProvider.class);
        when(auditUserProvider.currentLoginUser()).thenReturn(loginUser());
        StatisticsServiceImpl service = new StatisticsServiceImpl(
                mock(SalaryMonthlyMapper.class),
                mock(SalaryMonthlyDetailMapper.class),
                mock(SalaryDetailItemMapper.class),
                mock(EmployeeMapper.class),
                anomalyRecordMapper,
                mock(OperationLogService.class),
                auditUserProvider);

        service.handleAnomaly("monthly_fluctuation|1|2|3", "adjustment", "转入调整");

        ArgumentCaptor<SalaryAnomalyRecordDO> captor = ArgumentCaptor.forClass(SalaryAnomalyRecordDO.class);
        verify(anomalyRecordMapper).insert(captor.capture());
        SalaryAnomalyRecordDO record = captor.getValue();
        assertThat(record.getAnomalyKey()).isEqualTo("monthly_fluctuation|1|2|3");
        assertThat(record.getStatus()).isEqualTo("adjustment");
        assertThat(record.getRemark()).isEqualTo("转入调整");
        assertThat(record.getHandlerId()).isEqualTo(100L);
        assertThat(record.getHandlerName()).isEqualTo("finance");
        assertThat(record.getHandledAt()).isNotNull();
    }

    private LoginUser loginUser() {
        SysUserDO user = new SysUserDO();
        user.setId(100L);
        user.setUsername("finance");
        user.setStatus("active");
        return new LoginUser(user, List.of("FINANCE"));
    }
}

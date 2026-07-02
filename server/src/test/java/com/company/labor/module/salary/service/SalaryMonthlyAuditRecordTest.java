package com.company.labor.module.salary.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.labor.module.compensation.mapper.AttendanceRecordMapper;
import com.company.labor.module.compensation.mapper.PerformanceScoreMapper;
import com.company.labor.module.compensation.mapper.ProjectBonusAllocationMapper;
import com.company.labor.module.compensation.mapper.SalaryDeductionRecordMapper;
import com.company.labor.module.compensation.mapper.SalarySourceRecordMapper;
import com.company.labor.module.employee.mapper.EmployeeMapper;
import com.company.labor.module.file.service.FileBindingGuard;
import com.company.labor.module.salary.domain.entity.SalaryAuditRecordDO;
import com.company.labor.module.salary.mapper.SalaryAuditRecordMapper;
import com.company.labor.module.salary.mapper.SalaryDetailItemMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyDetailMapper;
import com.company.labor.module.salary.mapper.SalaryProfileItemMapper;
import com.company.labor.module.salary.mapper.SalaryProfileMapper;
import com.company.labor.module.salary.service.impl.SalaryMonthlyServiceImpl;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.service.OperationLogService;
import com.company.labor.security.AuditUserProvider;
import com.company.labor.security.LoginUser;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class SalaryMonthlyAuditRecordTest {
    @Test
    void auditRecordCapturesCurrentOperator() {
        SalaryAuditRecordMapper auditRecordMapper = mock(SalaryAuditRecordMapper.class);
        AuditUserProvider auditUserProvider = mock(AuditUserProvider.class);
        when(auditUserProvider.currentLoginUser()).thenReturn(loginUser());
        SalaryMonthlyServiceImpl service = new SalaryMonthlyServiceImpl(
                mock(EmployeeMapper.class),
                mock(SalaryProfileMapper.class),
                mock(SalaryProfileItemMapper.class),
                mock(SalaryMonthlyDetailMapper.class),
                mock(SalaryDetailItemMapper.class),
                auditRecordMapper,
                mock(OperationLogService.class),
                auditUserProvider,
                mock(AttendanceRecordMapper.class),
                mock(PerformanceScoreMapper.class),
                mock(ProjectBonusAllocationMapper.class),
                mock(SalaryDeductionRecordMapper.class),
                mock(SalarySourceRecordMapper.class),
                mock(FileBindingGuard.class));

        ReflectionTestUtils.invokeMethod(service, "audit", 1L, 2L, "approve", "pending_approval", "approved", "审核通过", "proof.xlsx");

        ArgumentCaptor<SalaryAuditRecordDO> captor = ArgumentCaptor.forClass(SalaryAuditRecordDO.class);
        verify(auditRecordMapper).insert(captor.capture());
        SalaryAuditRecordDO record = captor.getValue();
        assertThat(record.getSalaryMonthlyId()).isEqualTo(1L);
        assertThat(record.getSalaryDetailId()).isEqualTo(2L);
        assertThat(record.getOperatorId()).isEqualTo(100L);
        assertThat(record.getOperatorName()).isEqualTo("admin");
        assertThat(record.getAction()).isEqualTo("approve");
        assertThat(record.getStatusBefore()).isEqualTo("pending_approval");
        assertThat(record.getStatusAfter()).isEqualTo("approved");
        assertThat(record.getProofUrl()).isEqualTo("proof.xlsx");
    }

    private LoginUser loginUser() {
        SysUserDO user = new SysUserDO();
        user.setId(100L);
        user.setUsername("admin");
        user.setStatus("active");
        return new LoginUser(user, List.of("ADMIN"));
    }
}

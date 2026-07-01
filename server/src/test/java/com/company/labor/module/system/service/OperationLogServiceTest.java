package com.company.labor.module.system.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.labor.module.system.domain.entity.OperationLogDO;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.mapper.OperationLogMapper;
import com.company.labor.security.AuditUserProvider;
import com.company.labor.security.LoginUser;
import java.util.List;
import org.junit.jupiter.api.Test;

class OperationLogServiceTest {
    @Test
    void recordsCurrentOperatorWhenAvailable() {
        OperationLogMapper mapper = mock(OperationLogMapper.class);
        AuditUserProvider auditUserProvider = mock(AuditUserProvider.class);
        when(auditUserProvider.currentLoginUser()).thenReturn(loginUser());
        OperationLogService service = new OperationLogService(mapper, auditUserProvider);

        service.record("salary", 1L, "approve", "审核通过");

        verify(mapper).insert(any(OperationLogDO.class));
    }

    private LoginUser loginUser() {
        SysUserDO user = new SysUserDO();
        user.setId(100L);
        user.setUsername("admin");
        user.setStatus("active");
        return new LoginUser(user, List.of("ADMIN"));
    }
}

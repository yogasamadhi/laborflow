package com.company.labor.module.system.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.labor.module.employee.domain.entity.EmployeeDO;
import com.company.labor.module.employee.mapper.EmployeeMapper;
import com.company.labor.module.system.domain.entity.SysUserDO;
import org.junit.jupiter.api.Test;

class AccountEligibilityServiceTest {
    private final EmployeeMapper employeeMapper = mock(EmployeeMapper.class);
    private final AccountEligibilityService service = new AccountEligibilityService(employeeMapper);

    @Test
    void allowsActiveUnlinkedSystemAccount() {
        assertThat(service.isUserEligible(user(null, "active"))).isTrue();
    }

    @Test
    void rejectsInactiveOrLeftLinkedEmployeeAndAllowsReactivation() {
        SysUserDO user = user(20L, "active");
        when(employeeMapper.selectByIdIncludingDeleted(20L))
                .thenReturn(employee("inactive", 0), employee("left", 0), employee("active", 0));

        assertThat(service.isUserEligible(user)).isFalse();
        assertThat(service.isUserEligible(user)).isFalse();
        assertThat(service.isUserEligible(user)).isTrue();
    }

    @Test
    void rejectsMissingOrDeletedLinkedEmployee() {
        SysUserDO user = user(20L, "active");
        when(employeeMapper.selectByIdIncludingDeleted(20L))
                .thenReturn(null, employee("active", 1));

        assertThat(service.isUserEligible(user)).isFalse();
        assertThat(service.isUserEligible(user)).isFalse();
    }

    private SysUserDO user(Long employeeId, String status) {
        SysUserDO user = new SysUserDO();
        user.setId(10L);
        user.setEmployeeId(employeeId);
        user.setStatus(status);
        user.setDeleted(0);
        return user;
    }

    private EmployeeDO employee(String status, int deleted) {
        EmployeeDO employee = new EmployeeDO();
        employee.setId(20L);
        employee.setStatus(status);
        employee.setDeleted(deleted);
        return employee;
    }
}

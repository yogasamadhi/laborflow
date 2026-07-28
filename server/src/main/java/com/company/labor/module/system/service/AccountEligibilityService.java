package com.company.labor.module.system.service;

import com.company.labor.module.employee.domain.entity.EmployeeDO;
import com.company.labor.module.employee.mapper.EmployeeMapper;
import com.company.labor.module.system.domain.entity.SysUserDO;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class AccountEligibilityService {
    private final EmployeeMapper employeeMapper;

    public boolean isUserEligible(SysUserDO user) {
        if (user == null || deleted(user.getDeleted()) || !"active".equals(user.getStatus())) {
            return false;
        }
        return user.getEmployeeId() == null || isEmployeeActive(user.getEmployeeId());
    }

    public boolean isEmployeeActive(Long employeeId) {
        if (employeeId == null) {
            return true;
        }
        EmployeeDO employee = employeeMapper.selectByIdIncludingDeleted(employeeId);
        return employee != null && !deleted(employee.getDeleted()) && "active".equals(employee.getStatus());
    }

    private boolean deleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}

package com.company.labor.module.employee.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.company.labor.module.employee.domain.entity.EmployeeDO;

public interface EmployeeService extends IService<EmployeeDO> {
    Page<EmployeeDO> search(long page, long pageSize, String keyword, String status, Long departmentId, Long positionId);
    EmployeeDO create(EmployeeDO employee);
    EmployeeDO updateEmployee(Long id, EmployeeDO employee);
    void deleteEmployee(Long id);
    void restoreEmployee(Long id);
    void updateStatus(Long id, String status);
}

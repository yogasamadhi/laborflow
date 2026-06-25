package com.company.labor.module.employee.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.module.employee.domain.entity.DepartmentDO;

public interface DepartmentService extends IService<DepartmentDO> {
    Page<DepartmentDO> search(long page, long pageSize, String keyword, String status);
    DepartmentDO createDepartment(DepartmentDO department);
    DepartmentDO updateDepartment(Long id, DepartmentDO department);
    void deactivate(Long id);
}

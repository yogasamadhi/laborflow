package com.company.labor.module.employee.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.labor.module.employee.domain.entity.DepartmentDO;
import com.company.labor.module.employee.mapper.DepartmentMapper;
import com.company.labor.module.employee.service.DepartmentService;
import org.springframework.stereotype.Service;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, DepartmentDO> implements DepartmentService {
}


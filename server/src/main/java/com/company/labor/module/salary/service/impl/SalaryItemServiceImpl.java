package com.company.labor.module.salary.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.labor.module.salary.domain.entity.SalaryItemDO;
import com.company.labor.module.salary.mapper.SalaryItemMapper;
import com.company.labor.module.salary.service.SalaryItemService;
import org.springframework.stereotype.Service;

@Service
public class SalaryItemServiceImpl extends ServiceImpl<SalaryItemMapper, SalaryItemDO> implements SalaryItemService {
}


package com.company.labor.module.employee.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.labor.module.employee.domain.entity.PositionDO;
import com.company.labor.module.employee.mapper.PositionMapper;
import com.company.labor.module.employee.service.PositionService;
import org.springframework.stereotype.Service;

@Service
public class PositionServiceImpl extends ServiceImpl<PositionMapper, PositionDO> implements PositionService {
}


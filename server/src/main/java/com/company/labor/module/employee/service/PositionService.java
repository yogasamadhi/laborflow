package com.company.labor.module.employee.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.module.employee.domain.entity.PositionDO;

public interface PositionService extends IService<PositionDO> {
    Page<PositionDO> search(long page, long pageSize, String keyword, String status, Long departmentId);
    PositionDO createPosition(PositionDO position);
    PositionDO updatePosition(Long id, PositionDO position);
    void deletePosition(Long id);
    void restorePosition(Long id);
}

package com.company.labor.module.salary.service;

import com.baomidou.mybatisplus.extension.service.IService;
import com.company.labor.module.salary.domain.entity.SalaryItemDO;

public interface SalaryItemService extends IService<SalaryItemDO> {
    SalaryItemDO createItem(SalaryItemDO item);
    SalaryItemDO updateItem(Long id, SalaryItemDO item);
    void deleteItem(Long id);
    void restoreItem(Long id);
}

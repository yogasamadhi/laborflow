package com.company.labor.module.salary.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.softdelete.SoftDeleteGuard;
import com.company.labor.common.softdelete.SoftDeleteGuard.Reference;
import com.company.labor.module.salary.domain.entity.SalaryItemDO;
import com.company.labor.module.salary.mapper.SalaryItemMapper;
import com.company.labor.module.salary.service.SalaryItemService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SalaryItemServiceImpl extends ServiceImpl<SalaryItemMapper, SalaryItemDO> implements SalaryItemService {
    private final SoftDeleteGuard softDeleteGuard;

    @Override
    @Transactional
    public SalaryItemDO createItem(SalaryItemDO item) {
        validate(item, null);
        if (item.getStatus() == null) item.setStatus("active");
        if (item.getIsFixed() == null) item.setIsFixed(1);
        if (item.getSortOrder() == null) item.setSortOrder(0);
        save(item);
        return item;
    }

    @Override
    @Transactional
    public SalaryItemDO updateItem(Long id, SalaryItemDO item) {
        SalaryItemDO old = getById(id);
        if (old == null) throw new BusinessException("工资项不存在");
        item.setId(id);
        validate(item, id);
        if (item.getStatus() == null) item.setStatus(old.getStatus());
        if (item.getIsFixed() == null) item.setIsFixed(old.getIsFixed());
        if (item.getSortOrder() == null) item.setSortOrder(old.getSortOrder());
        updateById(item);
        return getById(id);
    }

    @Override
    @Transactional
    public void deleteItem(Long id) {
        SalaryItemDO item = getById(id);
        if (item == null) throw new BusinessException("工资项不存在");
        softDeleteGuard.requireNotReferenced("工资项", id,
                Reference.all("salary_profile_item", "salary_item_id", "工资档案项"),
                Reference.all("salary_detail_item", "salary_item_id", "工资明细项"));
        item.setDeleted(1);
        if (!updateById(item)) {
            throw new BusinessException("工资项删除失败");
        }
    }

    @Override
    @Transactional
    public void restoreItem(Long id) {
        SalaryItemDO item = baseMapper.selectByIdIncludingDeleted(id);
        if (item == null) throw new BusinessException("工资项不存在");
        if (!deleted(item.getDeleted())) {
            return;
        }
        validate(item, id);
        if (baseMapper.restoreById(id) <= 0) {
            throw new BusinessException("工资项恢复失败");
        }
    }

    private void validate(SalaryItemDO item, Long selfId) {
        if (!StringUtils.hasText(item.getName())) {
            throw new BusinessException("工资项名称必填");
        }
        if (!StringUtils.hasText(item.getType())) {
            throw new BusinessException("工资项类型必填");
        }
        Long id = selfId == null ? -1L : selfId;
        if (baseMapper.countActiveByNameAndTypeExceptId(item.getName(), item.getType(), id) > 0) {
            throw new BusinessException("同类型工资项名称已存在");
        }
    }

    private boolean deleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}

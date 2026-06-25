package com.company.labor.module.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.employee.domain.entity.DepartmentDO;
import com.company.labor.module.employee.mapper.DepartmentMapper;
import com.company.labor.module.employee.service.DepartmentService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, DepartmentDO> implements DepartmentService {
    @Override
    public Page<DepartmentDO> search(long page, long pageSize, String keyword, String status) {
        LambdaQueryWrapper<DepartmentDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(DepartmentDO::getName, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(DepartmentDO::getStatus, status);
        }
        wrapper.orderByAsc(DepartmentDO::getSortOrder).orderByDesc(DepartmentDO::getCreatedAt);
        return page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    @Transactional
    public DepartmentDO createDepartment(DepartmentDO department) {
        validate(department);
        if (department.getSortOrder() == null) department.setSortOrder(0);
        if (!StringUtils.hasText(department.getStatus())) department.setStatus("active");
        save(department);
        return department;
    }

    @Override
    @Transactional
    public DepartmentDO updateDepartment(Long id, DepartmentDO department) {
        DepartmentDO old = getById(id);
        if (old == null) throw new BusinessException("部门不存在");
        department.setId(id);
        validate(department);
        if (department.getSortOrder() == null) department.setSortOrder(0);
        if (!StringUtils.hasText(department.getStatus())) department.setStatus(old.getStatus());
        updateById(department);
        return getById(id);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        DepartmentDO department = getById(id);
        if (department == null) throw new BusinessException("部门不存在");
        department.setStatus("inactive");
        updateById(department);
    }

    private void validate(DepartmentDO department) {
        if (!StringUtils.hasText(department.getName())) {
            throw new BusinessException("部门名称必填");
        }
        if (department.getParentId() != null && department.getParentId().equals(department.getId())) {
            throw new BusinessException("上级部门不能选择自身");
        }
    }
}

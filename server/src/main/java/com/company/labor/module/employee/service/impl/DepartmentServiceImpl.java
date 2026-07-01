package com.company.labor.module.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.softdelete.SoftDeleteGuard;
import com.company.labor.common.softdelete.SoftDeleteGuard.Reference;
import com.company.labor.module.employee.domain.entity.DepartmentDO;
import com.company.labor.module.employee.mapper.DepartmentMapper;
import com.company.labor.module.employee.service.DepartmentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class DepartmentServiceImpl extends ServiceImpl<DepartmentMapper, DepartmentDO> implements DepartmentService {
    private final SoftDeleteGuard softDeleteGuard;

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
        validate(department, null);
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
        validate(department, id);
        if (department.getSortOrder() == null) department.setSortOrder(0);
        if (!StringUtils.hasText(department.getStatus())) department.setStatus(old.getStatus());
        updateById(department);
        return getById(id);
    }

    @Override
    @Transactional
    public void deleteDepartment(Long id) {
        DepartmentDO department = getById(id);
        if (department == null) throw new BusinessException("部门不存在");
        softDeleteGuard.requireNotReferenced("部门", id,
                Reference.active("department", "parent_id", "下级部门"),
                Reference.active("position", "department_id", "岗位"),
                Reference.active("employee", "department_id", "员工"),
                Reference.active("salary_monthly_detail", "department_id", "工资明细"));
        department.setDeleted(1);
        if (!updateById(department)) {
            throw new BusinessException("部门删除失败");
        }
    }

    @Override
    @Transactional
    public void restoreDepartment(Long id) {
        DepartmentDO department = baseMapper.selectByIdIncludingDeleted(id);
        if (department == null) throw new BusinessException("部门不存在");
        if (!deleted(department.getDeleted())) {
            return;
        }
        validate(department, id);
        if (baseMapper.restoreById(id) <= 0) {
            throw new BusinessException("部门恢复失败");
        }
    }

    private void validate(DepartmentDO department, Long selfId) {
        if (!StringUtils.hasText(department.getName())) {
            throw new BusinessException("部门名称必填");
        }
        if (department.getParentId() != null && department.getParentId().equals(department.getId())) {
            throw new BusinessException("上级部门不能选择自身");
        }
        if (department.getParentId() != null && getById(department.getParentId()) == null) {
            throw new BusinessException("上级部门不存在");
        }
        Long id = selfId == null ? -1L : selfId;
        if (baseMapper.countActiveByNameAndParentExceptId(department.getName(), department.getParentId(), id) > 0) {
            throw new BusinessException("同级部门名称已存在");
        }
    }

    private boolean deleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}

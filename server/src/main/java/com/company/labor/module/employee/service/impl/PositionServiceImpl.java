package com.company.labor.module.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.softdelete.SoftDeleteGuard;
import com.company.labor.common.softdelete.SoftDeleteGuard.Reference;
import com.company.labor.module.employee.domain.entity.DepartmentDO;
import com.company.labor.module.employee.domain.entity.PositionDO;
import com.company.labor.module.employee.mapper.DepartmentMapper;
import com.company.labor.module.employee.mapper.PositionMapper;
import com.company.labor.module.employee.service.PositionService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class PositionServiceImpl extends ServiceImpl<PositionMapper, PositionDO> implements PositionService {
    private final DepartmentMapper departmentMapper;
    private final SoftDeleteGuard softDeleteGuard;

    @Override
    public Page<PositionDO> search(long page, long pageSize, String keyword, String status, Long departmentId) {
        LambdaQueryWrapper<PositionDO> wrapper = new LambdaQueryWrapper<>();
        if (StringUtils.hasText(keyword)) {
            wrapper.like(PositionDO::getName, keyword);
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(PositionDO::getStatus, status);
        }
        if (departmentId != null) {
            wrapper.eq(PositionDO::getDepartmentId, departmentId);
        }
        wrapper.orderByAsc(PositionDO::getSortOrder).orderByDesc(PositionDO::getCreatedAt);
        return page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    @Transactional
    public PositionDO createPosition(PositionDO position) {
        validate(position, null);
        if (position.getSortOrder() == null) position.setSortOrder(0);
        if (!StringUtils.hasText(position.getStatus())) position.setStatus("active");
        if (!save(position)) {
            throw new BusinessException("岗位保存失败");
        }
        return position;
    }

    @Override
    @Transactional
    public PositionDO updatePosition(Long id, PositionDO position) {
        PositionDO old = getById(id);
        if (old == null) throw new BusinessException("岗位不存在");
        position.setId(id);
        validate(position, id);
        if (position.getSortOrder() == null) position.setSortOrder(0);
        if (!StringUtils.hasText(position.getStatus())) position.setStatus(old.getStatus());
        if (!updateById(position)) {
            throw new BusinessException("岗位保存失败");
        }
        return getById(id);
    }

    @Override
    @Transactional
    public void deletePosition(Long id) {
        PositionDO position = getById(id);
        if (position == null) throw new BusinessException("岗位不存在");
        softDeleteGuard.requireNotReferenced("岗位", id,
                Reference.active("employee", "position_id", "员工"),
                Reference.active("salary_monthly_detail", "position_id", "工资明细"),
                Reference.active("performance_indicator", "position_id", "绩效指标"),
                Reference.active("performance_plan", "position_id", "绩效方案"),
                Reference.active("performance_score", "position_id", "绩效评分"));
        position.setDeleted(1);
        if (!updateById(position)) {
            throw new BusinessException("岗位删除失败");
        }
    }

    @Override
    @Transactional
    public void restorePosition(Long id) {
        PositionDO position = baseMapper.selectByIdIncludingDeleted(id);
        if (position == null) throw new BusinessException("岗位不存在");
        if (!deleted(position.getDeleted())) {
            return;
        }
        validate(position, id);
        if (baseMapper.restoreById(id) <= 0) {
            throw new BusinessException("岗位恢复失败");
        }
    }

    private void validate(PositionDO position, Long selfId) {
        if (!StringUtils.hasText(position.getName())) {
            throw new BusinessException("岗位名称必填");
        }
        if (position.getDepartmentId() != null) {
            DepartmentDO department = departmentMapper.selectById(position.getDepartmentId());
            if (department == null) {
                throw new BusinessException("所属部门不存在");
            }
        }
        Long id = selfId == null ? -1L : selfId;
        if (baseMapper.countActiveByNameAndDepartmentExceptId(position.getName(), position.getDepartmentId(), id) > 0) {
            throw new BusinessException("同部门岗位名称已存在");
        }
    }

    private boolean deleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}

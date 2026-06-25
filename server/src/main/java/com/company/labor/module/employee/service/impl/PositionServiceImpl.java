package com.company.labor.module.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.labor.common.exception.BusinessException;
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
        validate(position);
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
        validate(position);
        if (position.getSortOrder() == null) position.setSortOrder(0);
        if (!StringUtils.hasText(position.getStatus())) position.setStatus(old.getStatus());
        if (!updateById(position)) {
            throw new BusinessException("岗位保存失败");
        }
        return getById(id);
    }

    @Override
    @Transactional
    public void deactivate(Long id) {
        PositionDO position = getById(id);
        if (position == null) throw new BusinessException("岗位不存在");
        position.setStatus("inactive");
        updateById(position);
    }

    private void validate(PositionDO position) {
        if (!StringUtils.hasText(position.getName())) {
            throw new BusinessException("岗位名称必填");
        }
        if (position.getDepartmentId() != null) {
            DepartmentDO department = departmentMapper.selectById(position.getDepartmentId());
            if (department == null) {
                throw new BusinessException("所属部门不存在");
            }
        }
    }
}

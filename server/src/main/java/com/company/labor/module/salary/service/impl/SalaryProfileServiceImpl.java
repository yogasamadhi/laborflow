package com.company.labor.module.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.salary.domain.dto.SalaryProfileItemDTO;
import com.company.labor.module.salary.domain.dto.SalaryProfileSaveDTO;
import com.company.labor.module.salary.domain.entity.SalaryItemDO;
import com.company.labor.module.salary.domain.entity.SalaryProfileDO;
import com.company.labor.module.salary.domain.entity.SalaryProfileItemDO;
import com.company.labor.module.salary.mapper.SalaryItemMapper;
import com.company.labor.module.salary.mapper.SalaryProfileItemMapper;
import com.company.labor.module.salary.mapper.SalaryProfileMapper;
import com.company.labor.module.salary.service.SalaryProfileService;
import java.math.BigDecimal;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class SalaryProfileServiceImpl extends ServiceImpl<SalaryProfileMapper, SalaryProfileDO> implements SalaryProfileService {
    private final SalaryProfileItemMapper profileItemMapper;
    private final SalaryItemMapper salaryItemMapper;

    @Override
    public Page<SalaryProfileDO> search(long page, long pageSize, Long employeeId) {
        LambdaQueryWrapper<SalaryProfileDO> wrapper = new LambdaQueryWrapper<SalaryProfileDO>().eq(SalaryProfileDO::getDeleted, 0);
        if (employeeId != null) wrapper.eq(SalaryProfileDO::getEmployeeId, employeeId);
        wrapper.orderByDesc(SalaryProfileDO::getCreatedAt);
        return page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    @Transactional
    public SalaryProfileDO saveProfile(Long id, SalaryProfileSaveDTO dto) {
        if (dto.getEmployeeId() == null) {
            throw new BusinessException("员工 ID 必填");
        }
        SalaryProfileDO profile = id == null ? new SalaryProfileDO() : getById(id);
        if (profile == null) {
            throw new BusinessException("工资档案不存在");
        }
        profile.setEmployeeId(dto.getEmployeeId());
        profile.setEffectiveDate(dto.getEffectiveDate());
        profile.setStatus(StringUtils.hasText(dto.getStatus()) ? dto.getStatus() : "active");
        profile.setRemark(dto.getRemark());
        saveOrUpdate(profile);

        profileItemMapper.delete(new LambdaUpdateWrapper<SalaryProfileItemDO>().eq(SalaryProfileItemDO::getSalaryProfileId, profile.getId()));
        if (dto.getItems() != null) {
            for (SalaryProfileItemDTO itemDTO : dto.getItems()) {
                SalaryProfileItemDO item = new SalaryProfileItemDO();
                item.setSalaryProfileId(profile.getId());
                item.setSalaryItemId(itemDTO.getSalaryItemId());
                item.setSalaryItemName(itemDTO.getSalaryItemName());
                item.setSalaryItemType(itemDTO.getSalaryItemType());
                if (itemDTO.getSalaryItemId() != null) {
                    SalaryItemDO salaryItem = salaryItemMapper.selectById(itemDTO.getSalaryItemId());
                    if (salaryItem != null) {
                        item.setSalaryItemName(salaryItem.getName());
                        item.setSalaryItemType(salaryItem.getType());
                    }
                }
                item.setAmount(itemDTO.getAmount() == null ? BigDecimal.ZERO : itemDTO.getAmount());
                item.setRemark(itemDTO.getRemark());
                if (!StringUtils.hasText(item.getSalaryItemName()) || !StringUtils.hasText(item.getSalaryItemType())) {
                    throw new BusinessException("工资项名称和类型必填");
                }
                profileItemMapper.insert(item);
            }
        }
        return profile;
    }

    @Override
    public List<SalaryProfileItemDO> items(Long profileId) {
        return profileItemMapper.selectList(new LambdaQueryWrapper<SalaryProfileItemDO>().eq(SalaryProfileItemDO::getSalaryProfileId, profileId));
    }
}


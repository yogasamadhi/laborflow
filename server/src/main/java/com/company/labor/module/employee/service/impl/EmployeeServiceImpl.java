package com.company.labor.module.employee.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.softdelete.SoftDeleteGuard;
import com.company.labor.common.softdelete.SoftDeleteGuard.Reference;
import com.company.labor.module.employee.domain.entity.EmployeeDO;
import com.company.labor.module.employee.mapper.EmployeeMapper;
import com.company.labor.module.employee.service.EmployeeService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class EmployeeServiceImpl extends ServiceImpl<EmployeeMapper, EmployeeDO> implements EmployeeService {
    private final SoftDeleteGuard softDeleteGuard;

    @Override
    public Page<EmployeeDO> search(long page, long pageSize, String keyword, String status, Long departmentId, Long positionId) {
        LambdaQueryWrapper<EmployeeDO> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(EmployeeDO::getDeleted, 0);
        if (StringUtils.hasText(keyword)) {
            wrapper.and(w -> w.like(EmployeeDO::getName, keyword).or().like(EmployeeDO::getPhone, keyword));
        }
        if (StringUtils.hasText(status)) {
            wrapper.eq(EmployeeDO::getStatus, status);
        }
        if (departmentId != null) {
            wrapper.eq(EmployeeDO::getDepartmentId, departmentId);
        }
        if (positionId != null) {
            wrapper.eq(EmployeeDO::getPositionId, positionId);
        }
        wrapper.orderByDesc(EmployeeDO::getCreatedAt);
        return page(new Page<>(page, pageSize), wrapper);
    }

    @Override
    @Transactional
    public EmployeeDO create(EmployeeDO employee) {
        if (!StringUtils.hasText(employee.getName())) {
            throw new BusinessException("员工姓名必填");
        }
        checkPhone(employee.getPhone(), null);
        if (!StringUtils.hasText(employee.getEmployeeType())) {
            employee.setEmployeeType("company_employee");
        }
        if (!StringUtils.hasText(employee.getStatus())) {
            employee.setStatus("active");
        }
        save(employee);
        return employee;
    }

    @Override
    @Transactional
    public EmployeeDO updateEmployee(Long id, EmployeeDO employee) {
        EmployeeDO old = getById(id);
        if (old == null) {
            throw new BusinessException("员工不存在");
        }
        checkPhone(employee.getPhone(), id);
        employee.setId(id);
        updateById(employee);
        return getById(id);
    }

    @Override
    @Transactional
    public void updateStatus(Long id, String status) {
        EmployeeDO employee = getById(id);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        employee.setStatus(status);
        updateById(employee);
    }

    @Override
    @Transactional
    public void deleteEmployee(Long id) {
        EmployeeDO employee = getById(id);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        softDeleteGuard.requireNotReferenced("员工", id,
                Reference.active("sys_user", "employee_id", "平台账号"),
                Reference.active("wechat_user_binding", "employee_id", "微信绑定"),
                Reference.active("salary_profile", "employee_id", "工资档案"),
                Reference.active("salary_monthly_detail", "employee_id", "工资明细"),
                Reference.active("attendance_record", "employee_id", "考勤记录"),
                Reference.active("performance_score", "employee_id", "绩效评分"),
                Reference.active("project_bonus_allocation", "employee_id", "奖金分配"),
                Reference.active("project_member", "employee_id", "项目成员"),
                Reference.all("salary_source_record", "employee_id", "工资来源"));
        employee.setDeleted(1);
        if (!updateById(employee)) {
            throw new BusinessException("员工删除失败");
        }
    }

    @Override
    @Transactional
    public void restoreEmployee(Long id) {
        EmployeeDO employee = baseMapper.selectByIdIncludingDeleted(id);
        if (employee == null) {
            throw new BusinessException("员工不存在");
        }
        if (!deleted(employee.getDeleted())) {
            return;
        }
        if (StringUtils.hasText(employee.getPhone()) && baseMapper.countActiveByPhoneExceptId(employee.getPhone(), id) > 0) {
            throw new BusinessException("手机号已存在，不能恢复员工");
        }
        if (baseMapper.restoreById(id) <= 0) {
            throw new BusinessException("员工恢复失败");
        }
    }

    private void checkPhone(String phone, Long selfId) {
        if (!StringUtils.hasText(phone)) {
            return;
        }
        LambdaQueryWrapper<EmployeeDO> wrapper = new LambdaQueryWrapper<EmployeeDO>()
                .eq(EmployeeDO::getPhone, phone)
                .eq(EmployeeDO::getDeleted, 0);
        if (selfId != null) {
            wrapper.ne(EmployeeDO::getId, selfId);
        }
        if (count(wrapper) > 0) {
            throw new BusinessException("手机号已存在");
        }
    }

    private boolean deleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }
}

package com.company.labor.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.employee.domain.entity.EmployeeDO;
import com.company.labor.module.employee.mapper.EmployeeMapper;
import com.company.labor.module.system.domain.dto.WechatBindingUpdateDTO;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.domain.entity.WechatUserBindingDO;
import com.company.labor.module.system.domain.vo.WechatBindingVO;
import com.company.labor.module.system.mapper.SysUserMapper;
import com.company.labor.module.system.mapper.WechatUserBindingMapper;
import com.company.labor.security.LoginUser;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class WechatBindingService {
    private static final String COL_ID = "id";
    private static final String COL_OPENID = "openid";
    private static final String COL_DELETED = "deleted";
    private static final String COL_STATUS = "status";
    private static final String COL_USER_ID = "user_id";
    private static final String COL_EMPLOYEE_ID = "employee_id";
    private static final String COL_NICKNAME = "nickname";
    private static final String COL_LAST_LOGIN_AT = "last_login_at";
    private static final String COL_UPDATED_AT = "updated_at";

    private final WechatUserBindingMapper bindingMapper;
    private final SysUserMapper userMapper;
    private final EmployeeMapper employeeMapper;
    private final AccountEligibilityService accountEligibilityService;
    private final OperationLogService operationLogService;

    public Page<WechatUserBindingDO> page(long page, long pageSize, String status, String keyword) {
        QueryWrapper<WechatUserBindingDO> query = new QueryWrapper<WechatUserBindingDO>()
                .eq(COL_DELETED, 0)
                .eq(StringUtils.hasText(status), COL_STATUS, status)
                .like(StringUtils.hasText(keyword), COL_OPENID, keyword)
                .orderByDesc("updated_at");
        return bindingMapper.selectPage(new Page<>(page, pageSize), query);
    }

    public Page<WechatBindingVO> pageView(long page, long pageSize, String status, String keyword) {
        Page<WechatUserBindingDO> rows = page(page, pageSize, status, keyword);
        List<Long> userIds = rows.getRecords().stream().map(WechatUserBindingDO::getUserId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, SysUserDO> users = userIds.isEmpty() ? Map.of() : userMapper.selectBatchIds(userIds)
                .stream().collect(Collectors.toMap(SysUserDO::getId, Function.identity()));
        List<Long> employeeIds = rows.getRecords().stream().map(WechatUserBindingDO::getEmployeeId)
                .filter(Objects::nonNull).distinct().toList();
        Map<Long, EmployeeDO> employees = employeeIds.isEmpty() ? Map.of() : employeeMapper.selectBatchIds(employeeIds)
                .stream().collect(Collectors.toMap(EmployeeDO::getId, Function.identity()));
        Page<WechatBindingVO> result = new Page<>(rows.getCurrent(), rows.getSize(), rows.getTotal());
        result.setRecords(rows.getRecords().stream().map(binding -> {
            SysUserDO user = users.get(binding.getUserId());
            EmployeeDO employee = employees.get(binding.getEmployeeId());
            String userName = user == null ? null
                    : (StringUtils.hasText(user.getNickname()) ? user.getNickname() : user.getUsername());
            return WechatBindingVO.from(binding, userName, employee == null ? null : employee.getName());
        }).toList());
        return result;
    }

    public void bindLoginUser(String openid, String nickname, LoginUser loginUser) {
        if (!StringUtils.hasText(openid)) {
            return;
        }
        WechatUserBindingDO binding = findByOpenid(openid);
        if (binding == null) {
            throw new BusinessException("微信会话不存在，请重新授权");
        }
        if ("inactive".equals(binding.getStatus())) {
            throw new BusinessException("微信绑定已停用，请联系管理员");
        }
        bind(binding, loginUser.getUser(), loginUser.getUser().getEmployeeId(), nickname, loginUser, "miniapp_bind");
    }

    public void bind(Long id, WechatBindingUpdateDTO dto, LoginUser operator) {
        WechatUserBindingDO binding = requireBinding(id);
        SysUserDO user = requireUser(dto.getUserId());
        Long employeeId = dto.getEmployeeId() != null ? dto.getEmployeeId() : user.getEmployeeId();
        requireEmployee(employeeId);
        bind(binding, user, employeeId, dto.getNickname(), operator, "admin_bind");
    }

    public void unbind(Long id, LoginUser operator) {
        WechatUserBindingDO binding = requireBinding(id);
        LocalDateTime now = LocalDateTime.now();
        bindingMapper.update(null, updateById(id)
                .set(COL_USER_ID, null)
                .set(COL_EMPLOYEE_ID, null)
                .set(COL_STATUS, "pending")
                .set(COL_UPDATED_AT, now));
        log("unbind", id, operator, "解绑微信 openid: " + binding.getOpenid());
    }

    public void disable(Long id, LoginUser operator) {
        WechatUserBindingDO binding = requireBinding(id);
        LocalDateTime now = LocalDateTime.now();
        bindingMapper.update(null, updateById(id)
                .set(COL_STATUS, "inactive")
                .set(COL_UPDATED_AT, now));
        log("disable", id, operator, "停用微信 openid: " + binding.getOpenid());
    }

    private void bind(WechatUserBindingDO binding, SysUserDO user, Long employeeId, String nickname, LoginUser operator, String action) {
        requireEmployee(employeeId);
        LocalDateTime now = LocalDateTime.now();
        bindingMapper.update(null, updateById(binding.getId())
                .set(COL_USER_ID, user.getId())
                .set(COL_EMPLOYEE_ID, employeeId)
                .set(COL_NICKNAME, StringUtils.hasText(nickname) ? nickname : user.getNickname())
                .set(COL_STATUS, "active")
                .set(COL_LAST_LOGIN_AT, now)
                .set(COL_UPDATED_AT, now));
        log(action, binding.getId(), operator, "绑定微信 openid: " + binding.getOpenid() + " 到用户: " + user.getUsername());
    }

    private WechatUserBindingDO requireBinding(Long id) {
        WechatUserBindingDO binding = bindingMapper.selectById(id);
        if (binding == null || deleted(binding.getDeleted())) {
            throw new BusinessException("微信绑定不存在");
        }
        return binding;
    }

    private WechatUserBindingDO findByOpenid(String openid) {
        return bindingMapper.selectOne(new QueryWrapper<WechatUserBindingDO>()
                .eq(COL_OPENID, openid)
                .eq(COL_DELETED, 0));
    }

    private SysUserDO requireUser(Long userId) {
        SysUserDO user = userMapper.selectById(userId);
        if (user == null || deleted(user.getDeleted())) {
            throw new BusinessException("绑定用户不存在");
        }
        if (!"active".equals(user.getStatus())) {
            throw new BusinessException("绑定用户已停用");
        }
        return user;
    }

    private void requireEmployee(Long employeeId) {
        if (employeeId == null) {
            return;
        }
        EmployeeDO employee = employeeMapper.selectById(employeeId);
        if (employee == null || deleted(employee.getDeleted())) {
            throw new BusinessException("绑定员工不存在");
        }
        if (!accountEligibilityService.isEmployeeActive(employeeId)) {
            throw new BusinessException("绑定员工已停用或离职");
        }
    }

    private UpdateWrapper<WechatUserBindingDO> updateById(Long id) {
        return new UpdateWrapper<WechatUserBindingDO>().eq(COL_ID, id);
    }

    private boolean deleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }

    private void log(String action, Long bindingId, LoginUser operator, String content) {
        operationLogService.record("wechat_binding", bindingId, action, operator, content);
    }
}

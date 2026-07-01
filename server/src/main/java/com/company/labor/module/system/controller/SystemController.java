package com.company.labor.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.system.domain.dto.WechatBindingUpdateDTO;
import com.company.labor.module.system.domain.entity.OperationLogDO;
import com.company.labor.module.system.domain.entity.SysRoleDO;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.domain.entity.WechatUserBindingDO;
import com.company.labor.module.system.mapper.OperationLogMapper;
import com.company.labor.module.system.mapper.SysRoleMapper;
import com.company.labor.module.system.service.SysUserService;
import com.company.labor.module.system.service.WechatBindingService;
import com.company.labor.security.LoginUser;
import com.company.labor.security.RoleExpressions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@PreAuthorize(RoleExpressions.ADMIN)
public class SystemController {
    private final SysUserService userService;
    private final SysRoleMapper roleMapper;
    private final OperationLogMapper operationLogMapper;
    private final WechatBindingService wechatBindingService;

    @GetMapping("/users")
    public Result<PageResult<SysUserDO>> users(@RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(PageResult.of(userService.page(new Page<>(page, pageSize))));
    }

    @GetMapping("/roles")
    public Result<PageResult<SysRoleDO>> roles(@RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(PageResult.of(roleMapper.selectPage(new Page<>(page, pageSize), null)));
    }

    @GetMapping("/logs")
    public Result<PageResult<OperationLogDO>> logs(@RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(PageResult.of(operationLogMapper.selectPage(new Page<>(page, pageSize), null)));
    }

    @GetMapping("/wechat-bindings")
    public Result<PageResult<WechatUserBindingDO>> wechatBindings(@RequestParam(defaultValue = "1") long page,
                                                                  @RequestParam(defaultValue = "20") long pageSize,
                                                                  @RequestParam(required = false) String status,
                                                                  @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(wechatBindingService.page(page, pageSize, status, keyword)));
    }

    @PostMapping("/wechat-bindings/{id}/bind")
    public Result<Void> bindWechat(@PathVariable Long id, @Valid @RequestBody WechatBindingUpdateDTO dto, Authentication authentication) {
        wechatBindingService.bind(id, dto, loginUser(authentication));
        return Result.success();
    }

    @PostMapping("/wechat-bindings/{id}/unbind")
    public Result<Void> unbindWechat(@PathVariable Long id, Authentication authentication) {
        wechatBindingService.unbind(id, loginUser(authentication));
        return Result.success();
    }

    @PostMapping("/wechat-bindings/{id}/disable")
    public Result<Void> disableWechat(@PathVariable Long id, Authentication authentication) {
        wechatBindingService.disable(id, loginUser(authentication));
        return Result.success();
    }

    private LoginUser loginUser(Authentication authentication) {
        return (LoginUser) authentication.getPrincipal();
    }
}

package com.company.labor.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.system.domain.dto.SysUserResetPasswordDTO;
import com.company.labor.module.system.domain.dto.SysUserRoleAssignDTO;
import com.company.labor.module.system.domain.dto.SysUserSaveDTO;
import com.company.labor.module.system.domain.dto.WechatBindingUpdateDTO;
import com.company.labor.module.system.domain.entity.OperationLogDO;
import com.company.labor.module.system.domain.entity.SysRoleDO;
import com.company.labor.module.system.domain.vo.SysUserVO;
import com.company.labor.module.system.domain.vo.OperationLogVO;
import com.company.labor.module.system.domain.vo.WechatBindingVO;
import com.company.labor.module.system.mapper.SysRoleMapper;
import com.company.labor.module.system.service.OperationLogService;
import com.company.labor.module.system.service.SystemUserAdminService;
import com.company.labor.module.system.service.WechatBindingService;
import com.company.labor.security.LoginUser;
import com.company.labor.security.RoleExpressions;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.format.annotation.DateTimeFormat;

@RestController
@RequestMapping("/api/system")
@RequiredArgsConstructor
@PreAuthorize(RoleExpressions.ADMIN)
public class SystemController {
    private final SystemUserAdminService userAdminService;
    private final SysRoleMapper roleMapper;
    private final OperationLogService operationLogService;
    private final WechatBindingService wechatBindingService;

    @GetMapping("/users")
    public Result<PageResult<SysUserVO>> users(@RequestParam(defaultValue = "1") long page,
                                               @RequestParam(defaultValue = "20") long pageSize,
                                               @RequestParam(required = false) String keyword,
                                               @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(userAdminService.page(page, pageSize, keyword, status)));
    }

    @PostMapping("/users")
    public Result<SysUserVO> createUser(@Valid @RequestBody SysUserSaveDTO dto, Authentication authentication) {
        return Result.success(userAdminService.create(dto, loginUser(authentication)));
    }

    @PutMapping("/users/{id}")
    public Result<SysUserVO> updateUser(@PathVariable Long id, @Valid @RequestBody SysUserSaveDTO dto, Authentication authentication) {
        return Result.success(userAdminService.update(id, dto, loginUser(authentication)));
    }

    @PostMapping("/users/{id}/roles")
    public Result<Void> assignUserRoles(@PathVariable Long id, @Valid @RequestBody SysUserRoleAssignDTO dto, Authentication authentication) {
        userAdminService.assignRoles(id, dto, loginUser(authentication));
        return Result.success();
    }

    @PostMapping("/users/{id}/reset-password")
    public Result<Void> resetUserPassword(@PathVariable Long id, @Valid @RequestBody SysUserResetPasswordDTO dto, Authentication authentication) {
        userAdminService.resetPassword(id, dto, loginUser(authentication));
        return Result.success();
    }

    @PostMapping("/users/{id}/disable")
    public Result<Void> disableUser(@PathVariable Long id, Authentication authentication) {
        userAdminService.disable(id, loginUser(authentication));
        return Result.success();
    }

    @GetMapping("/roles")
    public Result<PageResult<SysRoleDO>> roles(@RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(PageResult.of(roleMapper.selectPage(new Page<>(page, pageSize), null)));
    }

    @GetMapping("/logs")
    public Result<PageResult<OperationLogVO>> logs(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String module,
            @RequestParam(required = false) String action,
            @RequestParam(required = false) Long businessId,
            @RequestParam(required = false) Long operatorId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdFrom,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime createdTo) {
        Page<OperationLogDO> rows = operationLogService.page(
                page, pageSize, keyword, module, action, businessId, operatorId, createdFrom, createdTo);
        return Result.success(new PageResult<>(
                rows.getRecords().stream().map(OperationLogVO::from).toList(),
                rows.getTotal(), rows.getCurrent(), rows.getSize()));
    }

    @GetMapping("/wechat-bindings")
    public Result<PageResult<WechatBindingVO>> wechatBindings(@RequestParam(defaultValue = "1") long page,
                                                              @RequestParam(defaultValue = "20") long pageSize,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(wechatBindingService.pageView(page, pageSize, status, keyword)));
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

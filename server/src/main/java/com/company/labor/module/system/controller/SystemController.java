package com.company.labor.module.system.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.system.domain.entity.OperationLogDO;
import com.company.labor.module.system.domain.entity.SysRoleDO;
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.mapper.OperationLogMapper;
import com.company.labor.module.system.mapper.SysRoleMapper;
import com.company.labor.module.system.service.SysUserService;
import com.company.labor.security.RoleExpressions;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
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
}

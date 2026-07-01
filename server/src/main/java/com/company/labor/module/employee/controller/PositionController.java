package com.company.labor.module.employee.controller;

import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.employee.domain.entity.PositionDO;
import com.company.labor.module.employee.service.PositionService;
import com.company.labor.security.RoleExpressions;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/positions")
@RequiredArgsConstructor
@PreAuthorize(RoleExpressions.ORG_MANAGE)
public class PositionController {
    private final PositionService service;

    @GetMapping
    public Result<PageResult<PositionDO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long departmentId) {
        return Result.success(PageResult.of(service.search(page, pageSize, keyword, status, departmentId)));
    }

    @PostMapping
    public Result<PositionDO> create(@RequestBody PositionDO position) {
        return Result.success(service.createPosition(position));
    }

    @PutMapping("/{id}")
    public Result<PositionDO> update(@PathVariable Long id, @RequestBody PositionDO position) {
        return Result.success(service.updatePosition(id, position));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.deactivate(id);
        return Result.success();
    }
}

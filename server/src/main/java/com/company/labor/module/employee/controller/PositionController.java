package com.company.labor.module.employee.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.employee.domain.entity.PositionDO;
import com.company.labor.module.employee.service.PositionService;
import lombok.RequiredArgsConstructor;
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
public class PositionController {
    private final PositionService service;

    @GetMapping
    public Result<PageResult<PositionDO>> list(@RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long pageSize, @RequestParam(required = false) Long departmentId) {
        LambdaQueryWrapper<PositionDO> wrapper = new LambdaQueryWrapper<>();
        if (departmentId != null) wrapper.eq(PositionDO::getDepartmentId, departmentId);
        return Result.success(PageResult.of(service.page(new Page<>(page, pageSize), wrapper)));
    }

    @PostMapping
    public Result<PositionDO> create(@RequestBody PositionDO position) {
        if (position.getStatus() == null) position.setStatus("active");
        service.save(position);
        return Result.success(position);
    }

    @PutMapping("/{id}")
    public Result<PositionDO> update(@PathVariable Long id, @RequestBody PositionDO position) {
        position.setId(id);
        service.updateById(position);
        return Result.success(service.getById(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.removeById(id);
        return Result.success();
    }
}


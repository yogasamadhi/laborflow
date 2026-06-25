package com.company.labor.module.salary.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.salary.domain.entity.SalaryItemDO;
import com.company.labor.module.salary.service.SalaryItemService;
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
@RequestMapping("/api/salary/items")
@RequiredArgsConstructor
public class SalaryItemController {
    private final SalaryItemService service;

    @GetMapping
    public Result<PageResult<SalaryItemDO>> list(@RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(PageResult.of(service.page(new Page<>(page, pageSize))));
    }

    @PostMapping
    public Result<SalaryItemDO> create(@RequestBody SalaryItemDO item) {
        if (item.getStatus() == null) item.setStatus("active");
        if (item.getIsFixed() == null) item.setIsFixed(1);
        service.save(item);
        return Result.success(item);
    }

    @PutMapping("/{id}")
    public Result<SalaryItemDO> update(@PathVariable Long id, @RequestBody SalaryItemDO item) {
        item.setId(id);
        service.updateById(item);
        return Result.success(service.getById(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.removeById(id);
        return Result.success();
    }
}


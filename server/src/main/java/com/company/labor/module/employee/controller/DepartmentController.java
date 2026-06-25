package com.company.labor.module.employee.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.employee.domain.entity.DepartmentDO;
import com.company.labor.module.employee.service.DepartmentService;
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
@RequestMapping("/api/departments")
@RequiredArgsConstructor
public class DepartmentController {
    private final DepartmentService service;

    @GetMapping
    public Result<PageResult<DepartmentDO>> list(@RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(PageResult.of(service.page(new Page<>(page, pageSize))));
    }

    @PostMapping
    public Result<DepartmentDO> create(@RequestBody DepartmentDO department) {
        if (department.getStatus() == null) department.setStatus("active");
        service.save(department);
        return Result.success(department);
    }

    @PutMapping("/{id}")
    public Result<DepartmentDO> update(@PathVariable Long id, @RequestBody DepartmentDO department) {
        department.setId(id);
        service.updateById(department);
        return Result.success(service.getById(id));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.removeById(id);
        return Result.success();
    }
}


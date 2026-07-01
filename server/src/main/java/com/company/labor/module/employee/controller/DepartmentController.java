package com.company.labor.module.employee.controller;

import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.employee.domain.entity.DepartmentDO;
import com.company.labor.module.employee.service.DepartmentService;
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
@RequestMapping("/api/departments")
@RequiredArgsConstructor
@PreAuthorize(RoleExpressions.ORG_MANAGE)
public class DepartmentController {
    private final DepartmentService service;

    @GetMapping
    public Result<PageResult<DepartmentDO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(service.search(page, pageSize, keyword, status)));
    }

    @PostMapping
    public Result<DepartmentDO> create(@RequestBody DepartmentDO department) {
        return Result.success(service.createDepartment(department));
    }

    @PutMapping("/{id}")
    public Result<DepartmentDO> update(@PathVariable Long id, @RequestBody DepartmentDO department) {
        return Result.success(service.updateDepartment(id, department));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.deactivate(id);
        return Result.success();
    }
}

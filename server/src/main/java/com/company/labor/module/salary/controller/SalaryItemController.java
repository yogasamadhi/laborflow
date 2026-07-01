package com.company.labor.module.salary.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.salary.domain.dto.SalaryWriteDTO;
import com.company.labor.module.salary.domain.entity.SalaryItemDO;
import com.company.labor.module.salary.service.SalaryItemService;
import com.company.labor.security.RoleExpressions;
import jakarta.validation.Valid;
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

import static com.company.labor.common.util.RequestCopyUtils.copy;

@RestController
@RequestMapping("/api/salary/items")
@RequiredArgsConstructor
@PreAuthorize(RoleExpressions.SALARY_MANAGE)
public class SalaryItemController {
    private final SalaryItemService service;

    @GetMapping
    public Result<PageResult<SalaryItemDO>> list(@RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(PageResult.of(service.page(new Page<>(page, pageSize))));
    }

    @PostMapping
    public Result<SalaryItemDO> create(@Valid @RequestBody SalaryWriteDTO.SalaryItemRequest dto) {
        return Result.success(service.createItem(copy(dto, SalaryItemDO.class)));
    }

    @PutMapping("/{id}")
    public Result<SalaryItemDO> update(@PathVariable Long id, @Valid @RequestBody SalaryWriteDTO.SalaryItemRequest dto) {
        return Result.success(service.updateItem(id, copy(dto, SalaryItemDO.class)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.deleteItem(id);
        return Result.success();
    }

    @PostMapping("/{id}/restore")
    public Result<Void> restore(@PathVariable Long id) {
        service.restoreItem(id);
        return Result.success();
    }
}

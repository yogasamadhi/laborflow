package com.company.labor.module.employee.controller;

import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.employee.domain.dto.EmployeeWriteDTO;
import com.company.labor.module.employee.domain.dto.StatusUpdateDTO;
import com.company.labor.module.employee.domain.entity.EmployeeDO;
import com.company.labor.module.employee.service.EmployeeService;
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
@RequestMapping("/api/employees")
@RequiredArgsConstructor
@PreAuthorize(RoleExpressions.ORG_MANAGE)
public class EmployeeController {
    private final EmployeeService service;

    @GetMapping
    public Result<PageResult<EmployeeDO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long positionId) {
        return Result.success(PageResult.of(service.search(page, pageSize, keyword, status, departmentId, positionId)));
    }

    @PostMapping
    public Result<EmployeeDO> create(@Valid @RequestBody EmployeeWriteDTO.EmployeeRequest dto) {
        return Result.success(service.create(copy(dto, EmployeeDO.class)));
    }

    @GetMapping("/{id}")
    public Result<EmployeeDO> detail(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @PutMapping("/{id}")
    public Result<EmployeeDO> update(@PathVariable Long id, @Valid @RequestBody EmployeeWriteDTO.EmployeeRequest dto) {
        return Result.success(service.updateEmployee(id, copy(dto, EmployeeDO.class)));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.removeById(id);
        return Result.success();
    }

    @PutMapping("/{id}/status")
    public Result<Void> updateStatus(@PathVariable Long id, @Valid @RequestBody StatusUpdateDTO dto) {
        service.updateStatus(id, dto.getStatus());
        return Result.success();
    }
}

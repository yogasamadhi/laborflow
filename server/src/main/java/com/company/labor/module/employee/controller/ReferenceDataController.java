package com.company.labor.module.employee.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.employee.domain.entity.EmployeeDO;
import com.company.labor.module.employee.domain.entity.PositionDO;
import com.company.labor.module.employee.domain.vo.EmployeeReferenceVO;
import com.company.labor.module.employee.domain.vo.PositionReferenceVO;
import com.company.labor.module.employee.service.EmployeeService;
import com.company.labor.module.employee.service.PositionService;
import com.company.labor.security.RoleExpressions;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/reference")
@RequiredArgsConstructor
@PreAuthorize(RoleExpressions.COMPENSATION_MANAGE)
public class ReferenceDataController {
    private final EmployeeService employeeService;
    private final PositionService positionService;

    @GetMapping("/employees")
    public Result<PageResult<EmployeeReferenceVO>> employees(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long departmentId,
            @RequestParam(required = false) Long positionId) {
        Page<EmployeeDO> rows = employeeService.search(page, pageSize, keyword, status, departmentId, positionId);
        List<EmployeeReferenceVO> records = rows.getRecords().stream().map(EmployeeReferenceVO::from).toList();
        return Result.success(new PageResult<>(records, rows.getTotal(), rows.getCurrent(), rows.getSize()));
    }

    @GetMapping("/positions")
    public Result<PageResult<PositionReferenceVO>> positions(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) Long departmentId) {
        Page<PositionDO> rows = positionService.search(page, pageSize, keyword, status, departmentId);
        List<PositionReferenceVO> records = rows.getRecords().stream().map(PositionReferenceVO::from).toList();
        return Result.success(new PageResult<>(records, rows.getTotal(), rows.getCurrent(), rows.getSize()));
    }
}

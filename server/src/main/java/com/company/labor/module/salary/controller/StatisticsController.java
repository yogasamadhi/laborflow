package com.company.labor.module.salary.controller;

import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.result.Result;
import com.company.labor.module.salary.domain.dto.SalaryAnomalyHandleDTO;
import com.company.labor.module.salary.domain.vo.SalaryAnomalyVO;
import com.company.labor.module.salary.domain.vo.SalaryEmployeeHistoryVO;
import com.company.labor.module.salary.domain.vo.SalaryOverviewVO;
import com.company.labor.module.salary.domain.vo.SalaryTrendVO;
import com.company.labor.module.salary.service.StatisticsService;
import com.company.labor.security.LoginUser;
import com.company.labor.security.RoleExpressions;
import jakarta.validation.Valid;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics/salary")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService service;

    @GetMapping("/overview")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<SalaryOverviewVO> overview(@RequestParam(required = false) Long departmentId) {
        return Result.success(service.overview(departmentId));
    }

    @GetMapping("/monthly-trend")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<List<SalaryTrendVO>> monthlyTrend(@RequestParam(required = false) Long departmentId) {
        return Result.success(service.monthlyTrend(departmentId));
    }

    @GetMapping("/department-summary")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<List<Map<String, Object>>> departmentSummary(@RequestParam(required = false) Long departmentId) {
        return Result.success(service.departmentSummary(departmentId));
    }

    @GetMapping("/employee-history/{employeeId}")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<SalaryEmployeeHistoryVO> employeeHistory(@PathVariable Long employeeId) {
        return Result.success(service.employeeHistory(employeeId));
    }

    @GetMapping("/my-history")
    public Result<SalaryEmployeeHistoryVO> myHistory(Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        Long employeeId = loginUser.getUser().getEmployeeId();
        if (employeeId == null) {
            throw new BusinessException("当前账号未绑定员工");
        }
        return Result.success(service.employeeHistory(employeeId));
    }

    @GetMapping("/anomalies")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<List<SalaryAnomalyVO>> anomalies(@RequestParam(defaultValue = "0.3") double fluctuationRatio) {
        return Result.success(service.anomalies(fluctuationRatio));
    }

    @PostMapping("/anomalies/handle")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<Void> handleAnomaly(@Valid @RequestBody SalaryAnomalyHandleDTO dto) {
        service.handleAnomaly(dto.getAnomalyKey(), dto.getStatus(), dto.getRemark());
        return Result.success();
    }
}

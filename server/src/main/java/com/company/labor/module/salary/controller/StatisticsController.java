package com.company.labor.module.salary.controller;

import com.company.labor.common.result.Result;
import com.company.labor.module.salary.domain.vo.SalaryAnomalyVO;
import com.company.labor.module.salary.domain.vo.SalaryEmployeeHistoryVO;
import com.company.labor.module.salary.domain.vo.SalaryOverviewVO;
import com.company.labor.module.salary.domain.vo.SalaryTrendVO;
import com.company.labor.module.salary.service.StatisticsService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/statistics/salary")
@RequiredArgsConstructor
public class StatisticsController {
    private final StatisticsService service;

    @GetMapping("/overview")
    public Result<SalaryOverviewVO> overview() {
        return Result.success(service.overview());
    }

    @GetMapping("/monthly-trend")
    public Result<List<SalaryTrendVO>> monthlyTrend() {
        return Result.success(service.monthlyTrend());
    }

    @GetMapping("/department-summary")
    public Result<List<Map<String, Object>>> departmentSummary() {
        return Result.success(service.departmentSummary());
    }

    @GetMapping("/employee-history/{employeeId}")
    public Result<SalaryEmployeeHistoryVO> employeeHistory(@PathVariable Long employeeId) {
        return Result.success(service.employeeHistory(employeeId));
    }

    @GetMapping("/anomalies")
    public Result<List<SalaryAnomalyVO>> anomalies(@RequestParam(defaultValue = "0.3") double fluctuationRatio) {
        return Result.success(service.anomalies(fluctuationRatio));
    }
}

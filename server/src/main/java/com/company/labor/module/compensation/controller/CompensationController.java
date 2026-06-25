package com.company.labor.module.compensation.controller;

import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.compensation.domain.entity.AttendanceRecordDO;
import com.company.labor.module.compensation.domain.entity.PerformanceIndicatorDO;
import com.company.labor.module.compensation.domain.entity.PerformancePlanDO;
import com.company.labor.module.compensation.domain.entity.PerformanceScoreDO;
import com.company.labor.module.compensation.domain.entity.ProjectBonusAllocationDO;
import com.company.labor.module.compensation.domain.entity.ProjectBonusPoolDO;
import com.company.labor.module.compensation.domain.vo.CompensationDashboardVO;
import com.company.labor.module.compensation.service.CompensationService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/compensation")
@RequiredArgsConstructor
public class CompensationController {
    private final CompensationService service;

    @GetMapping("/attendance")
    public Result<PageResult<AttendanceRecordDO>> attendance(@RequestParam(defaultValue = "1") long page,
                                                             @RequestParam(defaultValue = "20") long pageSize,
                                                             @RequestParam(required = false) Long employeeId,
                                                             @RequestParam(required = false) String month) {
        return Result.success(PageResult.of(service.attendanceRecords(page, pageSize, employeeId, month)));
    }

    @PostMapping("/attendance")
    public Result<AttendanceRecordDO> saveAttendance(@RequestBody AttendanceRecordDO record) {
        return Result.success(service.saveAttendance(record));
    }

    @PutMapping("/attendance/{id}")
    public Result<AttendanceRecordDO> updateAttendance(@PathVariable Long id, @RequestBody AttendanceRecordDO record) {
        return Result.success(service.updateAttendance(id, record));
    }

    @GetMapping("/indicators")
    public Result<PageResult<PerformanceIndicatorDO>> indicators(@RequestParam(defaultValue = "1") long page,
                                                                 @RequestParam(defaultValue = "20") long pageSize,
                                                                 @RequestParam(required = false) Long positionId,
                                                                 @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(service.indicators(page, pageSize, positionId, status)));
    }

    @PostMapping("/indicators")
    public Result<PerformanceIndicatorDO> saveIndicator(@RequestBody PerformanceIndicatorDO indicator) {
        return Result.success(service.saveIndicator(indicator));
    }

    @PutMapping("/indicators/{id}")
    public Result<PerformanceIndicatorDO> updateIndicator(@PathVariable Long id, @RequestBody PerformanceIndicatorDO indicator) {
        return Result.success(service.updateIndicator(id, indicator));
    }

    @GetMapping("/plans")
    public Result<PageResult<PerformancePlanDO>> plans(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "20") long pageSize,
                                                       @RequestParam(required = false) String month,
                                                       @RequestParam(required = false) Long projectId) {
        return Result.success(PageResult.of(service.plans(page, pageSize, month, projectId)));
    }

    @PostMapping("/plans")
    public Result<PerformancePlanDO> savePlan(@RequestBody PerformancePlanDO plan) {
        return Result.success(service.savePlan(plan));
    }

    @PutMapping("/plans/{id}")
    public Result<PerformancePlanDO> updatePlan(@PathVariable Long id, @RequestBody PerformancePlanDO plan) {
        return Result.success(service.updatePlan(id, plan));
    }

    @GetMapping("/scores")
    public Result<PageResult<PerformanceScoreDO>> scores(@RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "20") long pageSize,
                                                         @RequestParam(required = false) String month,
                                                         @RequestParam(required = false) Long employeeId,
                                                         @RequestParam(required = false) Long projectId) {
        return Result.success(PageResult.of(service.scores(page, pageSize, month, employeeId, projectId)));
    }

    @PostMapping("/scores")
    public Result<PerformanceScoreDO> saveScore(@RequestBody PerformanceScoreDO score) {
        return Result.success(service.saveScore(score));
    }

    @PutMapping("/scores/{id}")
    public Result<PerformanceScoreDO> updateScore(@PathVariable Long id, @RequestBody PerformanceScoreDO score) {
        return Result.success(service.updateScore(id, score));
    }

    @PostMapping("/scores/{id}/confirm")
    public Result<Void> confirmScore(@PathVariable Long id) {
        service.confirmScore(id);
        return Result.success();
    }

    @GetMapping("/bonus-pools")
    public Result<PageResult<ProjectBonusPoolDO>> bonusPools(@RequestParam(defaultValue = "1") long page,
                                                             @RequestParam(defaultValue = "20") long pageSize,
                                                             @RequestParam(required = false) Long projectId,
                                                             @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(service.bonusPools(page, pageSize, projectId, status)));
    }

    @PostMapping("/bonus-pools")
    public Result<ProjectBonusPoolDO> saveBonusPool(@RequestBody ProjectBonusPoolDO pool) {
        return Result.success(service.saveBonusPool(pool));
    }

    @PutMapping("/bonus-pools/{id}")
    public Result<ProjectBonusPoolDO> updateBonusPool(@PathVariable Long id, @RequestBody ProjectBonusPoolDO pool) {
        return Result.success(service.updateBonusPool(id, pool));
    }

    @PostMapping("/bonus-pools/{id}/release")
    public Result<Void> releaseBonusPool(@PathVariable Long id) {
        service.releaseBonusPool(id);
        return Result.success();
    }

    @GetMapping("/bonus-allocations")
    public Result<PageResult<ProjectBonusAllocationDO>> bonusAllocations(@RequestParam(defaultValue = "1") long page,
                                                                         @RequestParam(defaultValue = "20") long pageSize,
                                                                         @RequestParam(required = false) Long poolId,
                                                                         @RequestParam(required = false) String paymentMonth) {
        return Result.success(PageResult.of(service.bonusAllocations(page, pageSize, poolId, paymentMonth)));
    }

    @PostMapping("/bonus-allocations")
    public Result<ProjectBonusAllocationDO> saveBonusAllocation(@RequestBody ProjectBonusAllocationDO allocation) {
        return Result.success(service.saveBonusAllocation(allocation));
    }

    @PutMapping("/bonus-allocations/{id}")
    public Result<ProjectBonusAllocationDO> updateBonusAllocation(@PathVariable Long id, @RequestBody ProjectBonusAllocationDO allocation) {
        return Result.success(service.updateBonusAllocation(id, allocation));
    }

    @PostMapping("/bonus-allocations/{id}/confirm")
    public Result<Void> confirmBonusAllocation(@PathVariable Long id) {
        service.confirmBonusAllocation(id);
        return Result.success();
    }

    @GetMapping("/dashboard")
    public Result<CompensationDashboardVO> dashboard(@RequestParam(required = false) String month) {
        return Result.success(service.dashboard(month));
    }
}

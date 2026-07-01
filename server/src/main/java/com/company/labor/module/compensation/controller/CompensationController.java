package com.company.labor.module.compensation.controller;

import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.compensation.domain.dto.CompensationWriteDTO;
import com.company.labor.module.compensation.domain.entity.AttendanceRecordDO;
import com.company.labor.module.compensation.domain.entity.PerformanceIndicatorDO;
import com.company.labor.module.compensation.domain.entity.PerformancePlanDO;
import com.company.labor.module.compensation.domain.entity.PerformanceScoreDO;
import com.company.labor.module.compensation.domain.entity.ProjectBonusAllocationDO;
import com.company.labor.module.compensation.domain.entity.ProjectBonusPoolDO;
import com.company.labor.module.compensation.domain.vo.CompensationDashboardVO;
import com.company.labor.module.compensation.service.CompensationService;
import com.company.labor.security.RoleExpressions;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
@RequestMapping("/api/compensation")
@RequiredArgsConstructor
@PreAuthorize(RoleExpressions.COMPENSATION_MANAGE)
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
    public Result<AttendanceRecordDO> saveAttendance(@RequestBody CompensationWriteDTO.AttendanceRecordRequest dto) {
        return Result.success(service.saveAttendance(copy(dto, AttendanceRecordDO.class)));
    }

    @PutMapping("/attendance/{id}")
    public Result<AttendanceRecordDO> updateAttendance(@PathVariable Long id, @RequestBody CompensationWriteDTO.AttendanceRecordRequest dto) {
        return Result.success(service.updateAttendance(id, copy(dto, AttendanceRecordDO.class)));
    }

    @GetMapping("/indicators")
    public Result<PageResult<PerformanceIndicatorDO>> indicators(@RequestParam(defaultValue = "1") long page,
                                                                 @RequestParam(defaultValue = "20") long pageSize,
                                                                 @RequestParam(required = false) Long positionId,
                                                                 @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(service.indicators(page, pageSize, positionId, status)));
    }

    @PostMapping("/indicators")
    public Result<PerformanceIndicatorDO> saveIndicator(@RequestBody CompensationWriteDTO.PerformanceIndicatorRequest dto) {
        return Result.success(service.saveIndicator(copy(dto, PerformanceIndicatorDO.class)));
    }

    @PutMapping("/indicators/{id}")
    public Result<PerformanceIndicatorDO> updateIndicator(@PathVariable Long id, @RequestBody CompensationWriteDTO.PerformanceIndicatorRequest dto) {
        return Result.success(service.updateIndicator(id, copy(dto, PerformanceIndicatorDO.class)));
    }

    @GetMapping("/plans")
    public Result<PageResult<PerformancePlanDO>> plans(@RequestParam(defaultValue = "1") long page,
                                                       @RequestParam(defaultValue = "20") long pageSize,
                                                       @RequestParam(required = false) String month,
                                                       @RequestParam(required = false) Long projectId) {
        return Result.success(PageResult.of(service.plans(page, pageSize, month, projectId)));
    }

    @PostMapping("/plans")
    public Result<PerformancePlanDO> savePlan(@RequestBody CompensationWriteDTO.PerformancePlanRequest dto) {
        return Result.success(service.savePlan(copy(dto, PerformancePlanDO.class)));
    }

    @PutMapping("/plans/{id}")
    public Result<PerformancePlanDO> updatePlan(@PathVariable Long id, @RequestBody CompensationWriteDTO.PerformancePlanRequest dto) {
        return Result.success(service.updatePlan(id, copy(dto, PerformancePlanDO.class)));
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
    public Result<PerformanceScoreDO> saveScore(@RequestBody CompensationWriteDTO.PerformanceScoreRequest dto) {
        return Result.success(service.saveScore(copy(dto, PerformanceScoreDO.class)));
    }

    @PutMapping("/scores/{id}")
    public Result<PerformanceScoreDO> updateScore(@PathVariable Long id, @RequestBody CompensationWriteDTO.PerformanceScoreRequest dto) {
        return Result.success(service.updateScore(id, copy(dto, PerformanceScoreDO.class)));
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
    public Result<ProjectBonusPoolDO> saveBonusPool(@RequestBody CompensationWriteDTO.ProjectBonusPoolRequest dto) {
        return Result.success(service.saveBonusPool(copy(dto, ProjectBonusPoolDO.class)));
    }

    @PutMapping("/bonus-pools/{id}")
    public Result<ProjectBonusPoolDO> updateBonusPool(@PathVariable Long id, @RequestBody CompensationWriteDTO.ProjectBonusPoolRequest dto) {
        return Result.success(service.updateBonusPool(id, copy(dto, ProjectBonusPoolDO.class)));
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
    public Result<ProjectBonusAllocationDO> saveBonusAllocation(@RequestBody CompensationWriteDTO.ProjectBonusAllocationRequest dto) {
        return Result.success(service.saveBonusAllocation(copy(dto, ProjectBonusAllocationDO.class)));
    }

    @PutMapping("/bonus-allocations/{id}")
    public Result<ProjectBonusAllocationDO> updateBonusAllocation(@PathVariable Long id, @RequestBody CompensationWriteDTO.ProjectBonusAllocationRequest dto) {
        return Result.success(service.updateBonusAllocation(id, copy(dto, ProjectBonusAllocationDO.class)));
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

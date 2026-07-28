package com.company.labor.module.compensation.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.module.compensation.domain.entity.AttendanceRecordDO;
import com.company.labor.module.compensation.domain.entity.PerformanceIndicatorDO;
import com.company.labor.module.compensation.domain.entity.PerformancePlanDO;
import com.company.labor.module.compensation.domain.entity.PerformanceScoreDO;
import com.company.labor.module.compensation.domain.entity.ProjectBonusAllocationDO;
import com.company.labor.module.compensation.domain.entity.ProjectBonusPoolDO;
import com.company.labor.module.compensation.domain.entity.SalaryDeductionRecordDO;
import com.company.labor.module.compensation.domain.dto.CompensationWriteDTO;
import com.company.labor.module.compensation.domain.vo.CompensationDashboardVO;
import com.company.labor.module.compensation.domain.vo.PerformancePlanDetailVO;
import com.company.labor.module.compensation.domain.vo.PerformanceScoreDetailVO;
import java.util.List;

public interface CompensationService {
    Page<AttendanceRecordDO> attendanceRecords(long page, long pageSize, Long employeeId, String month);
    AttendanceRecordDO saveAttendance(AttendanceRecordDO record);
    AttendanceRecordDO updateAttendance(Long id, AttendanceRecordDO record);

    Page<SalaryDeductionRecordDO> salaryDeductions(long page, long pageSize, Long employeeId, String month, String type);
    SalaryDeductionRecordDO saveSalaryDeduction(SalaryDeductionRecordDO record);
    SalaryDeductionRecordDO updateSalaryDeduction(Long id, SalaryDeductionRecordDO record);
    void deleteSalaryDeduction(Long id);

    Page<PerformanceIndicatorDO> indicators(long page, long pageSize, Long positionId, String status);
    PerformanceIndicatorDO saveIndicator(PerformanceIndicatorDO indicator);
    PerformanceIndicatorDO updateIndicator(Long id, PerformanceIndicatorDO indicator);

    Page<PerformancePlanDO> plans(long page, long pageSize, String month, Long projectId);
    PerformancePlanDetailVO savePlan(PerformancePlanDO plan, List<Long> indicatorIds);
    PerformancePlanDetailVO updatePlan(Long id, PerformancePlanDO plan, List<Long> indicatorIds);
    PerformancePlanDetailVO planDetail(Long id);
    void activatePlan(Long id);
    void closePlan(Long id);

    Page<PerformanceScoreDO> scores(long page, long pageSize, String month, Long employeeId, Long projectId);
    List<PerformanceScoreDetailVO> generateScores(Long planId, List<Long> employeeIds);
    PerformanceScoreDetailVO scoreDetail(Long id);
    PerformanceScoreDetailVO updateScore(Long id, CompensationWriteDTO.PerformanceScoreUpdateRequest dto);
    PerformanceScoreDetailVO refreshScore(Long id);
    void confirmScore(Long id);

    Page<ProjectBonusPoolDO> bonusPools(long page, long pageSize, Long projectId, String status);
    ProjectBonusPoolDO saveBonusPool(ProjectBonusPoolDO pool);
    ProjectBonusPoolDO updateBonusPool(Long id, ProjectBonusPoolDO pool);
    void releaseBonusPool(Long id);

    Page<ProjectBonusAllocationDO> bonusAllocations(long page, long pageSize, Long poolId, String paymentMonth);
    ProjectBonusAllocationDO saveBonusAllocation(ProjectBonusAllocationDO allocation);
    ProjectBonusAllocationDO updateBonusAllocation(Long id, ProjectBonusAllocationDO allocation);
    void confirmBonusAllocation(Long id);

    CompensationDashboardVO dashboard(String month);
}

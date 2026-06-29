package com.company.labor.module.project.controller;

import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.project.domain.dto.FileAttachDTO;
import com.company.labor.module.project.domain.dto.GenerateTeamSettlementDTO;
import com.company.labor.module.project.domain.dto.ProjectActionDTO;
import com.company.labor.module.project.domain.entity.DailyWorkReportDO;
import com.company.labor.module.project.domain.entity.MachineryEquipmentDO;
import com.company.labor.module.project.domain.entity.MachineryWorkRecordDO;
import com.company.labor.module.project.domain.entity.MaterialRecordDO;
import com.company.labor.module.project.domain.entity.ProjectCostRecordDO;
import com.company.labor.module.project.domain.entity.ProjectDocumentArchiveDO;
import com.company.labor.module.project.domain.entity.ProjectDO;
import com.company.labor.module.project.domain.entity.ProjectPaymentRecordDO;
import com.company.labor.module.project.domain.entity.ProjectProcessPriceDO;
import com.company.labor.module.project.domain.entity.ProjectSectionDO;
import com.company.labor.module.project.domain.entity.TeamDO;
import com.company.labor.module.project.domain.entity.TeamSettlementDO;
import com.company.labor.module.project.domain.entity.TeamUnitPriceDO;
import com.company.labor.module.project.domain.entity.VariationOrderDO;
import com.company.labor.module.project.domain.entity.WorkProcessDO;
import com.company.labor.module.project.domain.vo.ProjectDashboardVO;
import com.company.labor.module.project.service.ProjectManagementService;
import com.company.labor.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
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
@RequiredArgsConstructor
public class ProjectManagementController {
    private final ProjectManagementService service;

    @GetMapping("/api/projects")
    public Result<PageResult<ProjectDO>> projects(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "20") long pageSize,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(service.projects(page, pageSize, keyword, status)));
    }

    @PostMapping("/api/projects")
    public Result<ProjectDO> saveProject(@RequestBody ProjectDO project) {
        return Result.success(service.saveProject(project));
    }

    @GetMapping("/api/projects/{id}")
    public Result<ProjectDO> project(@PathVariable Long id) {
        return Result.success(service.project(id));
    }

    @PutMapping("/api/projects/{id}")
    public Result<ProjectDO> updateProject(@PathVariable Long id, @RequestBody ProjectDO project) {
        return Result.success(service.updateProject(id, project));
    }

    @DeleteMapping("/api/projects/{id}")
    public Result<Void> deleteProject(@PathVariable Long id) {
        service.deleteProject(id);
        return Result.success();
    }

    @PostMapping("/api/projects/{id}/status")
    public Result<Void> updateProjectStatus(@PathVariable Long id, @RequestBody ProjectActionDTO dto) {
        service.updateProjectStatus(id, dto.getStatus());
        return Result.success();
    }

    @GetMapping({"/api/project-sections", "/api/projects/{projectId}/sections"})
    public Result<PageResult<ProjectSectionDO>> sections(@PathVariable(required = false) Long projectId,
                                                         @RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "20") long pageSize,
                                                         @RequestParam(required = false) Long filterProjectId,
                                                         @RequestParam(required = false) String keyword) {
        return Result.success(PageResult.of(service.sections(page, pageSize, projectId == null ? filterProjectId : projectId, keyword)));
    }

    @PostMapping({"/api/project-sections", "/api/projects/{projectId}/sections"})
    public Result<ProjectSectionDO> saveSection(@PathVariable(required = false) Long projectId, @RequestBody ProjectSectionDO section) {
        if (projectId != null) section.setProjectId(projectId);
        return Result.success(service.saveSection(section));
    }

    @PutMapping("/api/project-sections/{id}")
    public Result<ProjectSectionDO> updateSection(@PathVariable Long id, @RequestBody ProjectSectionDO section) {
        return Result.success(service.updateSection(id, section));
    }

    @DeleteMapping("/api/project-sections/{id}")
    public Result<Void> deleteSection(@PathVariable Long id) {
        service.deleteSection(id);
        return Result.success();
    }

    @GetMapping("/api/work-processes")
    public Result<PageResult<WorkProcessDO>> workProcesses(@RequestParam(defaultValue = "1") long page,
                                                           @RequestParam(defaultValue = "20") long pageSize,
                                                           @RequestParam(required = false) String keyword,
                                                           @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(service.workProcesses(page, pageSize, keyword, status)));
    }

    @PostMapping("/api/work-processes")
    public Result<WorkProcessDO> saveWorkProcess(@RequestBody WorkProcessDO process) {
        return Result.success(service.saveWorkProcess(process));
    }

    @PutMapping("/api/work-processes/{id}")
    public Result<WorkProcessDO> updateWorkProcess(@PathVariable Long id, @RequestBody WorkProcessDO process) {
        return Result.success(service.updateWorkProcess(id, process));
    }

    @DeleteMapping("/api/work-processes/{id}")
    public Result<Void> deactivateWorkProcess(@PathVariable Long id) {
        service.deactivateWorkProcess(id);
        return Result.success();
    }

    @GetMapping("/api/teams")
    public Result<PageResult<TeamDO>> teams(@RequestParam(defaultValue = "1") long page,
                                            @RequestParam(defaultValue = "20") long pageSize,
                                            @RequestParam(required = false) String keyword,
                                            @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(service.teams(page, pageSize, keyword, status)));
    }

    @PostMapping("/api/teams")
    public Result<TeamDO> saveTeam(@RequestBody TeamDO team) {
        return Result.success(service.saveTeam(team));
    }

    @PutMapping("/api/teams/{id}")
    public Result<TeamDO> updateTeam(@PathVariable Long id, @RequestBody TeamDO team) {
        return Result.success(service.updateTeam(id, team));
    }

    @DeleteMapping("/api/teams/{id}")
    public Result<Void> deactivateTeam(@PathVariable Long id) {
        service.deactivateTeam(id);
        return Result.success();
    }

    @GetMapping("/api/team-unit-prices")
    public Result<PageResult<TeamUnitPriceDO>> teamUnitPrices(@RequestParam(defaultValue = "1") long page,
                                                              @RequestParam(defaultValue = "20") long pageSize,
                                                              @RequestParam(required = false) Long projectId,
                                                              @RequestParam(required = false) Long teamId) {
        return Result.success(PageResult.of(service.teamUnitPrices(page, pageSize, projectId, teamId)));
    }

    @PostMapping("/api/team-unit-prices")
    public Result<TeamUnitPriceDO> saveTeamUnitPrice(@RequestBody TeamUnitPriceDO price) {
        return Result.success(service.saveTeamUnitPrice(price));
    }

    @PutMapping("/api/team-unit-prices/{id}")
    public Result<TeamUnitPriceDO> updateTeamUnitPrice(@PathVariable Long id, @RequestBody TeamUnitPriceDO price) {
        return Result.success(service.updateTeamUnitPrice(id, price));
    }

    @GetMapping("/api/project-process-prices")
    public Result<PageResult<ProjectProcessPriceDO>> projectProcessPrices(@RequestParam(defaultValue = "1") long page,
                                                                          @RequestParam(defaultValue = "20") long pageSize,
                                                                          @RequestParam(required = false) Long projectId) {
        return Result.success(PageResult.of(service.projectProcessPrices(page, pageSize, projectId)));
    }

    @PostMapping("/api/project-process-prices")
    public Result<ProjectProcessPriceDO> saveProjectProcessPrice(@RequestBody ProjectProcessPriceDO price) {
        return Result.success(service.saveProjectProcessPrice(price));
    }

    @PutMapping("/api/project-process-prices/{id}")
    public Result<ProjectProcessPriceDO> updateProjectProcessPrice(@PathVariable Long id, @RequestBody ProjectProcessPriceDO price) {
        return Result.success(service.updateProjectProcessPrice(id, price));
    }

    @GetMapping("/api/daily-work-reports")
    public Result<PageResult<DailyWorkReportDO>> dailyReports(@RequestParam(defaultValue = "1") long page,
                                                              @RequestParam(defaultValue = "20") long pageSize,
                                                              @RequestParam(required = false) Long projectId,
                                                              @RequestParam(required = false) Long teamId,
                                                              @RequestParam(required = false) String status,
                                                              @RequestParam(required = false) Boolean mine,
                                                              Authentication authentication) {
        Long createdBy = Boolean.TRUE.equals(mine) ? currentUserId(authentication) : null;
        return Result.success(PageResult.of(service.dailyReports(page, pageSize, projectId, teamId, status, createdBy)));
    }

    @PostMapping("/api/daily-work-reports")
    public Result<DailyWorkReportDO> saveDailyReport(@RequestBody DailyWorkReportDO report, Authentication authentication) {
        Long userId = currentUserId(authentication);
        report.setCreatedBy(userId);
        report.setUpdatedBy(userId);
        return Result.success(service.saveDailyReport(report));
    }

    @PutMapping("/api/daily-work-reports/{id}")
    public Result<DailyWorkReportDO> updateDailyReport(@PathVariable Long id, @RequestBody DailyWorkReportDO report, Authentication authentication) {
        report.setUpdatedBy(currentUserId(authentication));
        return Result.success(service.updateDailyReport(id, report));
    }

    @PostMapping("/api/daily-work-reports/{id}/submit")
    public Result<Void> submitDailyReport(@PathVariable Long id) {
        service.submitDailyReport(id);
        return Result.success();
    }

    @PostMapping("/api/daily-work-reports/{id}/pm-confirm")
    public Result<Void> pmConfirmDailyReport(@PathVariable Long id, @RequestBody(required = false) ProjectActionDTO dto) {
        service.pmConfirmDailyReport(id, dto);
        return Result.success();
    }

    @PostMapping("/api/daily-work-reports/{id}/final-confirm")
    public Result<Void> finalConfirmDailyReport(@PathVariable Long id, @RequestBody(required = false) ProjectActionDTO dto) {
        service.finalConfirmDailyReport(id, dto);
        return Result.success();
    }

    @PostMapping("/api/daily-work-reports/{id}/reject")
    public Result<Void> rejectDailyReport(@PathVariable Long id, @RequestBody(required = false) ProjectActionDTO dto) {
        service.rejectDailyReport(id, dto);
        return Result.success();
    }

    @PostMapping("/api/daily-work-reports/{id}/files")
    public Result<Void> attachDailyReportFile(@PathVariable Long id, @RequestBody FileAttachDTO dto) {
        service.attachDailyReportFile(id, dto);
        return Result.success();
    }

    @GetMapping("/api/team-settlements")
    public Result<PageResult<TeamSettlementDO>> teamSettlements(@RequestParam(defaultValue = "1") long page,
                                                                @RequestParam(defaultValue = "20") long pageSize,
                                                                @RequestParam(required = false) Long projectId,
                                                                @RequestParam(required = false) Long teamId,
                                                                @RequestParam(required = false) String settlementMonth) {
        return Result.success(PageResult.of(service.teamSettlements(page, pageSize, projectId, teamId, settlementMonth)));
    }

    @PostMapping("/api/team-settlements/generate")
    public Result<TeamSettlementDO> generateTeamSettlement(@RequestBody GenerateTeamSettlementDTO dto) {
        return Result.success(service.generateTeamSettlement(dto));
    }

    @PostMapping("/api/team-settlements/{id}/confirm")
    public Result<Void> confirmTeamSettlement(@PathVariable Long id) {
        service.confirmTeamSettlement(id);
        return Result.success();
    }

    @PostMapping("/api/team-settlements/{id}/mark-paid")
    public Result<Void> markTeamSettlementPaid(@PathVariable Long id) {
        service.markTeamSettlementPaid(id);
        return Result.success();
    }

    @GetMapping("/api/variation-orders")
    public Result<PageResult<VariationOrderDO>> variationOrders(@RequestParam(defaultValue = "1") long page,
                                                                @RequestParam(defaultValue = "20") long pageSize,
                                                                @RequestParam(required = false) Long projectId,
                                                                @RequestParam(required = false) String status,
                                                                @RequestParam(required = false) Boolean mine,
                                                                Authentication authentication) {
        Long createdBy = Boolean.TRUE.equals(mine) ? currentUserId(authentication) : null;
        return Result.success(PageResult.of(service.variationOrders(page, pageSize, projectId, status, createdBy)));
    }

    @PostMapping("/api/variation-orders")
    public Result<VariationOrderDO> saveVariationOrder(@RequestBody VariationOrderDO order, Authentication authentication) {
        Long userId = currentUserId(authentication);
        order.setCreatedBy(userId);
        order.setUpdatedBy(userId);
        return Result.success(service.saveVariationOrder(order));
    }

    @PutMapping("/api/variation-orders/{id}")
    public Result<VariationOrderDO> updateVariationOrder(@PathVariable Long id, @RequestBody VariationOrderDO order, Authentication authentication) {
        order.setUpdatedBy(currentUserId(authentication));
        return Result.success(service.updateVariationOrder(id, order));
    }

    @PostMapping("/api/variation-orders/{id}/submit")
    public Result<Void> submitVariationOrder(@PathVariable Long id) {
        service.submitVariationOrder(id);
        return Result.success();
    }

    @PostMapping("/api/variation-orders/{id}/confirm")
    public Result<Void> confirmVariationOrder(@PathVariable Long id, @RequestBody(required = false) ProjectActionDTO dto) {
        service.confirmVariationOrder(id, dto);
        return Result.success();
    }

    @PostMapping("/api/variation-orders/{id}/reject")
    public Result<Void> rejectVariationOrder(@PathVariable Long id, @RequestBody(required = false) ProjectActionDTO dto) {
        service.rejectVariationOrder(id, dto);
        return Result.success();
    }

    @PostMapping("/api/variation-orders/{id}/files")
    public Result<Void> attachVariationOrderFile(@PathVariable Long id, @RequestBody FileAttachDTO dto) {
        service.attachVariationOrderFile(id, dto);
        return Result.success();
    }

    @GetMapping("/api/project-cost-records")
    public Result<PageResult<ProjectCostRecordDO>> costRecords(@RequestParam(defaultValue = "1") long page,
                                                               @RequestParam(defaultValue = "20") long pageSize,
                                                               @RequestParam(required = false) Long projectId) {
        return Result.success(PageResult.of(service.costRecords(page, pageSize, projectId)));
    }

    @PostMapping("/api/project-cost-records")
    public Result<ProjectCostRecordDO> saveCostRecord(@RequestBody ProjectCostRecordDO record) {
        return Result.success(service.saveCostRecord(record));
    }

    @PutMapping("/api/project-cost-records/{id}")
    public Result<ProjectCostRecordDO> updateCostRecord(@PathVariable Long id, @RequestBody ProjectCostRecordDO record) {
        return Result.success(service.updateCostRecord(id, record));
    }

    @GetMapping("/api/project-payment-records")
    public Result<PageResult<ProjectPaymentRecordDO>> paymentRecords(@RequestParam(defaultValue = "1") long page,
                                                                     @RequestParam(defaultValue = "20") long pageSize,
                                                                     @RequestParam(required = false) Long projectId,
                                                                     @RequestParam(required = false) String recordType) {
        return Result.success(PageResult.of(service.paymentRecords(page, pageSize, projectId, recordType)));
    }

    @PostMapping("/api/project-payment-records")
    public Result<ProjectPaymentRecordDO> savePaymentRecord(@RequestBody ProjectPaymentRecordDO record) {
        return Result.success(service.savePaymentRecord(record));
    }

    @PutMapping("/api/project-payment-records/{id}")
    public Result<ProjectPaymentRecordDO> updatePaymentRecord(@PathVariable Long id, @RequestBody ProjectPaymentRecordDO record) {
        return Result.success(service.updatePaymentRecord(id, record));
    }

    @GetMapping("/api/machinery-equipments")
    public Result<PageResult<MachineryEquipmentDO>> machineryEquipments(@RequestParam(defaultValue = "1") long page,
                                                                        @RequestParam(defaultValue = "20") long pageSize,
                                                                        @RequestParam(required = false) String keyword,
                                                                        @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(service.machineryEquipments(page, pageSize, keyword, status)));
    }

    @PostMapping("/api/machinery-equipments")
    public Result<MachineryEquipmentDO> saveMachineryEquipment(@RequestBody MachineryEquipmentDO equipment) {
        return Result.success(service.saveMachineryEquipment(equipment));
    }

    @PutMapping("/api/machinery-equipments/{id}")
    public Result<MachineryEquipmentDO> updateMachineryEquipment(@PathVariable Long id, @RequestBody MachineryEquipmentDO equipment) {
        return Result.success(service.updateMachineryEquipment(id, equipment));
    }

    @GetMapping("/api/machinery-work-records")
    public Result<PageResult<MachineryWorkRecordDO>> machineryWorkRecords(@RequestParam(defaultValue = "1") long page,
                                                                          @RequestParam(defaultValue = "20") long pageSize,
                                                                          @RequestParam(required = false) Long projectId,
                                                                          @RequestParam(required = false) Long equipmentId) {
        return Result.success(PageResult.of(service.machineryWorkRecords(page, pageSize, projectId, equipmentId)));
    }

    @PostMapping("/api/machinery-work-records")
    public Result<MachineryWorkRecordDO> saveMachineryWorkRecord(@RequestBody MachineryWorkRecordDO record) {
        return Result.success(service.saveMachineryWorkRecord(record));
    }

    @PutMapping("/api/machinery-work-records/{id}")
    public Result<MachineryWorkRecordDO> updateMachineryWorkRecord(@PathVariable Long id, @RequestBody MachineryWorkRecordDO record) {
        return Result.success(service.updateMachineryWorkRecord(id, record));
    }

    @GetMapping("/api/material-records")
    public Result<PageResult<MaterialRecordDO>> materialRecords(@RequestParam(defaultValue = "1") long page,
                                                                @RequestParam(defaultValue = "20") long pageSize,
                                                                @RequestParam(required = false) Long projectId,
                                                                @RequestParam(required = false) String recordType) {
        return Result.success(PageResult.of(service.materialRecords(page, pageSize, projectId, recordType)));
    }

    @PostMapping("/api/material-records")
    public Result<MaterialRecordDO> saveMaterialRecord(@RequestBody MaterialRecordDO record) {
        return Result.success(service.saveMaterialRecord(record));
    }

    @PutMapping("/api/material-records/{id}")
    public Result<MaterialRecordDO> updateMaterialRecord(@PathVariable Long id, @RequestBody MaterialRecordDO record) {
        return Result.success(service.updateMaterialRecord(id, record));
    }

    @GetMapping("/api/project-document-archives")
    public Result<PageResult<ProjectDocumentArchiveDO>> documentArchives(@RequestParam(defaultValue = "1") long page,
                                                                         @RequestParam(defaultValue = "20") long pageSize,
                                                                         @RequestParam(required = false) Long projectId,
                                                                         @RequestParam(required = false) String documentType) {
        return Result.success(PageResult.of(service.documentArchives(page, pageSize, projectId, documentType)));
    }

    @PostMapping("/api/project-document-archives")
    public Result<ProjectDocumentArchiveDO> saveDocumentArchive(@RequestBody ProjectDocumentArchiveDO archive) {
        return Result.success(service.saveDocumentArchive(archive));
    }

    @PutMapping("/api/project-document-archives/{id}")
    public Result<ProjectDocumentArchiveDO> updateDocumentArchive(@PathVariable Long id, @RequestBody ProjectDocumentArchiveDO archive) {
        return Result.success(service.updateDocumentArchive(id, archive));
    }

    @GetMapping("/api/project-dashboard/{projectId}")
    public Result<ProjectDashboardVO> dashboard(@PathVariable Long projectId) {
        return Result.success(service.dashboard(projectId));
    }

    private Long currentUserId(Authentication authentication) {
        if (authentication == null || authentication.getPrincipal() == null) {
            return null;
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return loginUser.getUser().getId();
    }
}

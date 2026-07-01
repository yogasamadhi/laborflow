package com.company.labor.module.project.controller;

import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.project.domain.dto.FileAttachDTO;
import com.company.labor.module.project.domain.dto.GenerateTeamSettlementDTO;
import com.company.labor.module.project.domain.dto.ProjectActionDTO;
import com.company.labor.module.project.domain.entity.DailyWorkReportDO;
import com.company.labor.module.project.domain.entity.DailyWorkReportFileDO;
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
import com.company.labor.module.project.domain.entity.VariationOrderConfirmDO;
import com.company.labor.module.project.domain.entity.VariationOrderDO;
import com.company.labor.module.project.domain.entity.VariationOrderFileDO;
import com.company.labor.module.project.domain.entity.WorkQuantityConfirmDO;
import com.company.labor.module.project.domain.entity.WorkProcessDO;
import com.company.labor.module.project.domain.vo.ProjectDashboardVO;
import com.company.labor.module.project.service.ProjectAccessService;
import com.company.labor.module.project.service.ProjectManagementService;
import com.company.labor.security.AccessControlSupport;
import com.company.labor.security.RoleExpressions;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
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
    private final ProjectAccessService projectAccessService;

    @GetMapping("/api/projects")
    public Result<PageResult<ProjectDO>> projects(@RequestParam(defaultValue = "1") long page,
                                                  @RequestParam(defaultValue = "20") long pageSize,
                                                  @RequestParam(required = false) String keyword,
                                                  @RequestParam(required = false) String status,
                                                  Authentication authentication) {
        return Result.success(PageResult.of(service.projects(page, pageSize, keyword, status, scopedProjectIds(authentication))));
    }

    @PostMapping("/api/projects")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<ProjectDO> saveProject(@RequestBody ProjectDO project) {
        return Result.success(service.saveProject(project));
    }

    @GetMapping("/api/projects/{id}")
    public Result<ProjectDO> project(@PathVariable Long id, Authentication authentication) {
        projectAccessService.requireProjectAccess(id, authentication);
        return Result.success(service.project(id));
    }

    @PutMapping("/api/projects/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<ProjectDO> updateProject(@PathVariable Long id, @RequestBody ProjectDO project) {
        return Result.success(service.updateProject(id, project));
    }

    @DeleteMapping("/api/projects/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> deleteProject(@PathVariable Long id) {
        service.deleteProject(id);
        return Result.success();
    }

    @PostMapping("/api/projects/{id}/status")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> updateProjectStatus(@PathVariable Long id, @RequestBody ProjectActionDTO dto) {
        service.updateProjectStatus(id, dto.getStatus());
        return Result.success();
    }

    @GetMapping({"/api/project-sections", "/api/projects/{projectId}/sections"})
    public Result<PageResult<ProjectSectionDO>> sections(@PathVariable(required = false) Long projectId,
                                                         @RequestParam(defaultValue = "1") long page,
                                                         @RequestParam(defaultValue = "20") long pageSize,
                                                         @RequestParam(required = false) Long filterProjectId,
                                                         @RequestParam(required = false) String keyword,
                                                         Authentication authentication) {
        Long scopedProjectId = projectId == null ? filterProjectId : projectId;
        requireProjectFilterAccess(scopedProjectId, authentication);
        return Result.success(PageResult.of(service.sections(page, pageSize, scopedProjectId, keyword)));
    }

    @PostMapping({"/api/project-sections", "/api/projects/{projectId}/sections"})
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<ProjectSectionDO> saveSection(@PathVariable(required = false) Long projectId, @RequestBody ProjectSectionDO section) {
        if (projectId != null) section.setProjectId(projectId);
        return Result.success(service.saveSection(section));
    }

    @PutMapping("/api/project-sections/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<ProjectSectionDO> updateSection(@PathVariable Long id, @RequestBody ProjectSectionDO section) {
        return Result.success(service.updateSection(id, section));
    }

    @DeleteMapping("/api/project-sections/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
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
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<WorkProcessDO> saveWorkProcess(@RequestBody WorkProcessDO process) {
        return Result.success(service.saveWorkProcess(process));
    }

    @PutMapping("/api/work-processes/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<WorkProcessDO> updateWorkProcess(@PathVariable Long id, @RequestBody WorkProcessDO process) {
        return Result.success(service.updateWorkProcess(id, process));
    }

    @DeleteMapping("/api/work-processes/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
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
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<TeamDO> saveTeam(@RequestBody TeamDO team) {
        return Result.success(service.saveTeam(team));
    }

    @PutMapping("/api/teams/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<TeamDO> updateTeam(@PathVariable Long id, @RequestBody TeamDO team) {
        return Result.success(service.updateTeam(id, team));
    }

    @DeleteMapping("/api/teams/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> deactivateTeam(@PathVariable Long id) {
        service.deactivateTeam(id);
        return Result.success();
    }

    @GetMapping("/api/team-unit-prices")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<PageResult<TeamUnitPriceDO>> teamUnitPrices(@RequestParam(defaultValue = "1") long page,
                                                              @RequestParam(defaultValue = "20") long pageSize,
                                                              @RequestParam(required = false) Long projectId,
                                                              @RequestParam(required = false) Long teamId) {
        return Result.success(PageResult.of(service.teamUnitPrices(page, pageSize, projectId, teamId)));
    }

    @PostMapping("/api/team-unit-prices")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<TeamUnitPriceDO> saveTeamUnitPrice(@RequestBody TeamUnitPriceDO price) {
        return Result.success(service.saveTeamUnitPrice(price));
    }

    @PutMapping("/api/team-unit-prices/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<TeamUnitPriceDO> updateTeamUnitPrice(@PathVariable Long id, @RequestBody TeamUnitPriceDO price) {
        return Result.success(service.updateTeamUnitPrice(id, price));
    }

    @GetMapping("/api/project-process-prices")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<PageResult<ProjectProcessPriceDO>> projectProcessPrices(@RequestParam(defaultValue = "1") long page,
                                                                          @RequestParam(defaultValue = "20") long pageSize,
                                                                          @RequestParam(required = false) Long projectId) {
        return Result.success(PageResult.of(service.projectProcessPrices(page, pageSize, projectId)));
    }

    @PostMapping("/api/project-process-prices")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<ProjectProcessPriceDO> saveProjectProcessPrice(@RequestBody ProjectProcessPriceDO price) {
        return Result.success(service.saveProjectProcessPrice(price));
    }

    @PutMapping("/api/project-process-prices/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
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
        if (projectId != null) {
            projectAccessService.requireProjectAccess(projectId, authentication);
        }
        Long createdBy = scopedCreatedBy(authentication, mine);
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
        requireDailyReportOwnerIfNeeded(id, authentication);
        report.setUpdatedBy(currentUserId(authentication));
        return Result.success(service.updateDailyReport(id, report));
    }

    @PostMapping("/api/daily-work-reports/{id}/submit")
    public Result<Void> submitDailyReport(@PathVariable Long id, Authentication authentication) {
        requireDailyReportOwnerIfNeeded(id, authentication);
        service.submitDailyReport(id);
        return Result.success();
    }

    @PostMapping("/api/daily-work-reports/{id}/pm-confirm")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> pmConfirmDailyReport(@PathVariable Long id, @RequestBody(required = false) ProjectActionDTO dto) {
        service.pmConfirmDailyReport(id, dto);
        return Result.success();
    }

    @PostMapping("/api/daily-work-reports/{id}/final-confirm")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> finalConfirmDailyReport(@PathVariable Long id, @RequestBody(required = false) ProjectActionDTO dto) {
        service.finalConfirmDailyReport(id, dto);
        return Result.success();
    }

    @PostMapping("/api/daily-work-reports/{id}/reject")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> rejectDailyReport(@PathVariable Long id, @RequestBody(required = false) ProjectActionDTO dto) {
        service.rejectDailyReport(id, dto);
        return Result.success();
    }

    @PostMapping("/api/daily-work-reports/{id}/files")
    public Result<Void> attachDailyReportFile(@PathVariable Long id, @RequestBody FileAttachDTO dto, Authentication authentication) {
        requireDailyReportOwnerIfNeeded(id, authentication);
        service.attachDailyReportFile(id, dto);
        return Result.success();
    }

    @GetMapping("/api/daily-work-reports/{id}/confirms")
    public Result<List<WorkQuantityConfirmDO>> dailyReportConfirms(@PathVariable Long id, Authentication authentication) {
        requireDailyReportOwnerIfNeeded(id, authentication);
        return Result.success(service.dailyReportConfirms(id));
    }

    @GetMapping("/api/daily-work-reports/{id}/files")
    public Result<List<DailyWorkReportFileDO>> dailyReportFiles(@PathVariable Long id, Authentication authentication) {
        requireDailyReportOwnerIfNeeded(id, authentication);
        return Result.success(service.dailyReportFiles(id));
    }

    @GetMapping("/api/team-settlements")
    @PreAuthorize(RoleExpressions.PROJECT_FINANCE)
    public Result<PageResult<TeamSettlementDO>> teamSettlements(@RequestParam(defaultValue = "1") long page,
                                                                @RequestParam(defaultValue = "20") long pageSize,
                                                                @RequestParam(required = false) Long projectId,
                                                                @RequestParam(required = false) Long teamId,
                                                                @RequestParam(required = false) String settlementMonth) {
        return Result.success(PageResult.of(service.teamSettlements(page, pageSize, projectId, teamId, settlementMonth)));
    }

    @PostMapping("/api/team-settlements/generate")
    @PreAuthorize(RoleExpressions.PROJECT_FINANCE)
    public Result<TeamSettlementDO> generateTeamSettlement(@RequestBody GenerateTeamSettlementDTO dto) {
        return Result.success(service.generateTeamSettlement(dto));
    }

    @PostMapping("/api/team-settlements/{id}/confirm")
    @PreAuthorize(RoleExpressions.PROJECT_FINANCE)
    public Result<Void> confirmTeamSettlement(@PathVariable Long id) {
        service.confirmTeamSettlement(id);
        return Result.success();
    }

    @PostMapping("/api/team-settlements/{id}/mark-paid")
    @PreAuthorize(RoleExpressions.PROJECT_FINANCE)
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
        if (projectId != null) {
            projectAccessService.requireProjectAccess(projectId, authentication);
        }
        Long createdBy = scopedCreatedBy(authentication, mine);
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
        requireVariationOrderOwnerIfNeeded(id, authentication);
        order.setUpdatedBy(currentUserId(authentication));
        return Result.success(service.updateVariationOrder(id, order));
    }

    @PostMapping("/api/variation-orders/{id}/submit")
    public Result<Void> submitVariationOrder(@PathVariable Long id, Authentication authentication) {
        requireVariationOrderOwnerIfNeeded(id, authentication);
        service.submitVariationOrder(id);
        return Result.success();
    }

    @PostMapping("/api/variation-orders/{id}/confirm")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> confirmVariationOrder(@PathVariable Long id, @RequestBody(required = false) ProjectActionDTO dto) {
        service.confirmVariationOrder(id, dto);
        return Result.success();
    }

    @PostMapping("/api/variation-orders/{id}/reject")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> rejectVariationOrder(@PathVariable Long id, @RequestBody(required = false) ProjectActionDTO dto) {
        service.rejectVariationOrder(id, dto);
        return Result.success();
    }

    @PostMapping("/api/variation-orders/{id}/files")
    public Result<Void> attachVariationOrderFile(@PathVariable Long id, @RequestBody FileAttachDTO dto, Authentication authentication) {
        requireVariationOrderOwnerIfNeeded(id, authentication);
        service.attachVariationOrderFile(id, dto);
        return Result.success();
    }

    @GetMapping("/api/variation-orders/{id}/confirms")
    public Result<List<VariationOrderConfirmDO>> variationOrderConfirms(@PathVariable Long id, Authentication authentication) {
        requireVariationOrderOwnerIfNeeded(id, authentication);
        return Result.success(service.variationOrderConfirms(id));
    }

    @GetMapping("/api/variation-orders/{id}/files")
    public Result<List<VariationOrderFileDO>> variationOrderFiles(@PathVariable Long id, Authentication authentication) {
        requireVariationOrderOwnerIfNeeded(id, authentication);
        return Result.success(service.variationOrderFiles(id));
    }

    @GetMapping("/api/project-cost-records")
    @PreAuthorize(RoleExpressions.PROJECT_FINANCE)
    public Result<PageResult<ProjectCostRecordDO>> costRecords(@RequestParam(defaultValue = "1") long page,
                                                               @RequestParam(defaultValue = "20") long pageSize,
                                                               @RequestParam(required = false) Long projectId) {
        return Result.success(PageResult.of(service.costRecords(page, pageSize, projectId)));
    }

    @PostMapping("/api/project-cost-records")
    @PreAuthorize(RoleExpressions.PROJECT_FINANCE)
    public Result<ProjectCostRecordDO> saveCostRecord(@RequestBody ProjectCostRecordDO record) {
        return Result.success(service.saveCostRecord(record));
    }

    @PutMapping("/api/project-cost-records/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_FINANCE)
    public Result<ProjectCostRecordDO> updateCostRecord(@PathVariable Long id, @RequestBody ProjectCostRecordDO record) {
        return Result.success(service.updateCostRecord(id, record));
    }

    @GetMapping("/api/project-payment-records")
    @PreAuthorize(RoleExpressions.PROJECT_FINANCE)
    public Result<PageResult<ProjectPaymentRecordDO>> paymentRecords(@RequestParam(defaultValue = "1") long page,
                                                                     @RequestParam(defaultValue = "20") long pageSize,
                                                                     @RequestParam(required = false) Long projectId,
                                                                     @RequestParam(required = false) String recordType) {
        return Result.success(PageResult.of(service.paymentRecords(page, pageSize, projectId, recordType)));
    }

    @PostMapping("/api/project-payment-records")
    @PreAuthorize(RoleExpressions.PROJECT_FINANCE)
    public Result<ProjectPaymentRecordDO> savePaymentRecord(@RequestBody ProjectPaymentRecordDO record) {
        return Result.success(service.savePaymentRecord(record));
    }

    @PutMapping("/api/project-payment-records/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_FINANCE)
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
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<MachineryEquipmentDO> saveMachineryEquipment(@RequestBody MachineryEquipmentDO equipment) {
        return Result.success(service.saveMachineryEquipment(equipment));
    }

    @PutMapping("/api/machinery-equipments/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
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
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<MachineryWorkRecordDO> saveMachineryWorkRecord(@RequestBody MachineryWorkRecordDO record) {
        return Result.success(service.saveMachineryWorkRecord(record));
    }

    @PutMapping("/api/machinery-work-records/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
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
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<MaterialRecordDO> saveMaterialRecord(@RequestBody MaterialRecordDO record) {
        return Result.success(service.saveMaterialRecord(record));
    }

    @PutMapping("/api/material-records/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
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
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<ProjectDocumentArchiveDO> saveDocumentArchive(@RequestBody ProjectDocumentArchiveDO archive) {
        return Result.success(service.saveDocumentArchive(archive));
    }

    @PutMapping("/api/project-document-archives/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<ProjectDocumentArchiveDO> updateDocumentArchive(@PathVariable Long id, @RequestBody ProjectDocumentArchiveDO archive) {
        return Result.success(service.updateDocumentArchive(id, archive));
    }

    @GetMapping("/api/project-dashboard/{projectId}")
    @PreAuthorize(RoleExpressions.PROJECT_FINANCE)
    public Result<ProjectDashboardVO> dashboard(@PathVariable Long projectId) {
        return Result.success(service.dashboard(projectId));
    }

    private Long currentUserId(Authentication authentication) {
        return AccessControlSupport.currentUserId(authentication);
    }

    private Long scopedCreatedBy(Authentication authentication, Boolean mine) {
        if (!AccessControlSupport.hasProjectManageRole(authentication)) {
            return currentUserId(authentication);
        }
        return Boolean.TRUE.equals(mine) ? currentUserId(authentication) : null;
    }

    private Set<Long> scopedProjectIds(Authentication authentication) {
        if (projectAccessService.hasProjectWideAccess(authentication)) {
            return null;
        }
        return projectAccessService.authorizedProjectIds(authentication);
    }

    private void requireProjectFilterAccess(Long projectId, Authentication authentication) {
        if (projectAccessService.hasProjectWideAccess(authentication)) {
            return;
        }
        if (projectId == null) {
            throw new BusinessException("请选择授权项目");
        }
        projectAccessService.requireProjectAccess(projectId, authentication);
    }

    private void requireDailyReportOwnerIfNeeded(Long id, Authentication authentication) {
        if (!AccessControlSupport.hasProjectManageRole(authentication)) {
            service.requireDailyReportOwner(id, currentUserId(authentication));
        }
    }

    private void requireVariationOrderOwnerIfNeeded(Long id, Authentication authentication) {
        if (!AccessControlSupport.hasProjectManageRole(authentication)) {
            service.requireVariationOrderOwner(id, currentUserId(authentication));
        }
    }
}

package com.company.labor.module.project.controller;

import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.project.domain.dto.FileAttachDTO;
import com.company.labor.module.project.domain.dto.GenerateTeamSettlementDTO;
import com.company.labor.module.project.domain.dto.ProjectActionDTO;
import com.company.labor.module.project.domain.dto.ProjectWriteDTO;
import com.company.labor.module.project.domain.entity.DailyWorkReportDO;
import com.company.labor.module.project.domain.entity.DailyWorkReportFileDO;
import com.company.labor.module.project.domain.entity.MachineryEquipmentDO;
import com.company.labor.module.project.domain.entity.MachineryWorkRecordDO;
import com.company.labor.module.project.domain.entity.MaterialRecordDO;
import com.company.labor.module.project.domain.entity.ProjectCostRecordDO;
import com.company.labor.module.project.domain.entity.ProjectContractItemDO;
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
import jakarta.validation.Valid;
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

import static com.company.labor.common.util.RequestCopyUtils.copy;

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
    public Result<ProjectDO> saveProject(@Valid @RequestBody ProjectWriteDTO.ProjectRequest dto) {
        return Result.success(service.saveProject(copy(dto, ProjectDO.class)));
    }

    @GetMapping("/api/projects/{id}")
    public Result<ProjectDO> project(@PathVariable Long id, Authentication authentication) {
        projectAccessService.requireProjectAccess(id, authentication);
        return Result.success(service.project(id));
    }

    @PutMapping("/api/projects/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<ProjectDO> updateProject(@PathVariable Long id, @Valid @RequestBody ProjectWriteDTO.ProjectRequest dto) {
        return Result.success(service.updateProject(id, copy(dto, ProjectDO.class)));
    }

    @DeleteMapping("/api/projects/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> deleteProject(@PathVariable Long id) {
        service.deleteProject(id);
        return Result.success();
    }

    @PostMapping("/api/projects/{id}/restore")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> restoreProject(@PathVariable Long id) {
        service.restoreProject(id);
        return Result.success();
    }

    @PostMapping("/api/projects/{id}/status")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> updateProjectStatus(@PathVariable Long id, @Valid @RequestBody ProjectActionDTO dto) {
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
    public Result<ProjectSectionDO> saveSection(@PathVariable(required = false) Long projectId, @Valid @RequestBody ProjectWriteDTO.SectionRequest dto) {
        ProjectSectionDO section = copy(dto, ProjectSectionDO.class);
        if (projectId != null) section.setProjectId(projectId);
        return Result.success(service.saveSection(section));
    }

    @PutMapping("/api/project-sections/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<ProjectSectionDO> updateSection(@PathVariable Long id, @Valid @RequestBody ProjectWriteDTO.SectionRequest dto) {
        return Result.success(service.updateSection(id, copy(dto, ProjectSectionDO.class)));
    }

    @DeleteMapping("/api/project-sections/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> deleteSection(@PathVariable Long id) {
        service.deleteSection(id);
        return Result.success();
    }

    @PostMapping("/api/project-sections/{id}/restore")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> restoreSection(@PathVariable Long id) {
        service.restoreSection(id);
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
    public Result<WorkProcessDO> saveWorkProcess(@Valid @RequestBody ProjectWriteDTO.WorkProcessRequest dto) {
        return Result.success(service.saveWorkProcess(copy(dto, WorkProcessDO.class)));
    }

    @PutMapping("/api/work-processes/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<WorkProcessDO> updateWorkProcess(@PathVariable Long id, @Valid @RequestBody ProjectWriteDTO.WorkProcessRequest dto) {
        return Result.success(service.updateWorkProcess(id, copy(dto, WorkProcessDO.class)));
    }

    @DeleteMapping("/api/work-processes/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> deleteWorkProcess(@PathVariable Long id) {
        service.deleteWorkProcess(id);
        return Result.success();
    }

    @PostMapping("/api/work-processes/{id}/restore")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> restoreWorkProcess(@PathVariable Long id) {
        service.restoreWorkProcess(id);
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
    public Result<TeamDO> saveTeam(@Valid @RequestBody ProjectWriteDTO.TeamRequest dto) {
        return Result.success(service.saveTeam(copy(dto, TeamDO.class)));
    }

    @PutMapping("/api/teams/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<TeamDO> updateTeam(@PathVariable Long id, @Valid @RequestBody ProjectWriteDTO.TeamRequest dto) {
        return Result.success(service.updateTeam(id, copy(dto, TeamDO.class)));
    }

    @DeleteMapping("/api/teams/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> deleteTeam(@PathVariable Long id) {
        service.deleteTeam(id);
        return Result.success();
    }

    @PostMapping("/api/teams/{id}/restore")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> restoreTeam(@PathVariable Long id) {
        service.restoreTeam(id);
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
    public Result<TeamUnitPriceDO> saveTeamUnitPrice(@Valid @RequestBody ProjectWriteDTO.TeamUnitPriceRequest dto) {
        return Result.success(service.saveTeamUnitPrice(copy(dto, TeamUnitPriceDO.class)));
    }

    @PutMapping("/api/team-unit-prices/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<TeamUnitPriceDO> updateTeamUnitPrice(@PathVariable Long id, @Valid @RequestBody ProjectWriteDTO.TeamUnitPriceRequest dto) {
        return Result.success(service.updateTeamUnitPrice(id, copy(dto, TeamUnitPriceDO.class)));
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
    public Result<ProjectProcessPriceDO> saveProjectProcessPrice(@Valid @RequestBody ProjectWriteDTO.ProjectProcessPriceRequest dto) {
        return Result.success(service.saveProjectProcessPrice(copy(dto, ProjectProcessPriceDO.class)));
    }

    @PutMapping("/api/project-process-prices/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<ProjectProcessPriceDO> updateProjectProcessPrice(@PathVariable Long id, @Valid @RequestBody ProjectWriteDTO.ProjectProcessPriceRequest dto) {
        return Result.success(service.updateProjectProcessPrice(id, copy(dto, ProjectProcessPriceDO.class)));
    }

    @GetMapping("/api/project-contract-items")
    public Result<PageResult<ProjectContractItemDO>> contractItems(@RequestParam(defaultValue = "1") long page,
                                                                   @RequestParam(defaultValue = "20") long pageSize,
                                                                   @RequestParam(required = false) Long projectId,
                                                                   @RequestParam(required = false) Long sectionId,
                                                                   @RequestParam(required = false) Long processId,
                                                                   @RequestParam(required = false) String status,
                                                                   Authentication authentication) {
        requireProjectFilterAccess(projectId, authentication);
        return Result.success(PageResult.of(service.contractItems(page, pageSize, projectId, sectionId, processId, status)));
    }

    @PostMapping("/api/project-contract-items")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<ProjectContractItemDO> saveContractItem(@Valid @RequestBody ProjectWriteDTO.ProjectContractItemRequest dto) {
        return Result.success(service.saveContractItem(copy(dto, ProjectContractItemDO.class)));
    }

    @PutMapping("/api/project-contract-items/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<ProjectContractItemDO> updateContractItem(@PathVariable Long id, @Valid @RequestBody ProjectWriteDTO.ProjectContractItemRequest dto) {
        return Result.success(service.updateContractItem(id, copy(dto, ProjectContractItemDO.class)));
    }

    @DeleteMapping("/api/project-contract-items/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> deleteContractItem(@PathVariable Long id) {
        service.deleteContractItem(id);
        return Result.success();
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
    public Result<DailyWorkReportDO> saveDailyReport(@Valid @RequestBody ProjectWriteDTO.DailyWorkReportRequest dto, Authentication authentication) {
        projectAccessService.requireProjectAccess(dto.getProjectId(), authentication);
        DailyWorkReportDO report = copy(dto, DailyWorkReportDO.class);
        Long userId = currentUserId(authentication);
        report.setCreatedBy(userId);
        report.setUpdatedBy(userId);
        return Result.success(service.saveDailyReport(report));
    }

    @PutMapping("/api/daily-work-reports/{id}")
    public Result<DailyWorkReportDO> updateDailyReport(@PathVariable Long id, @Valid @RequestBody ProjectWriteDTO.DailyWorkReportRequest dto, Authentication authentication) {
        requireDailyReportOwnerIfNeeded(id, authentication);
        projectAccessService.requireProjectAccess(dto.getProjectId(), authentication);
        DailyWorkReportDO report = copy(dto, DailyWorkReportDO.class);
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
    public Result<Void> pmConfirmDailyReport(@PathVariable Long id, @Valid @RequestBody(required = false) ProjectActionDTO dto) {
        service.pmConfirmDailyReport(id, dto);
        return Result.success();
    }

    @PostMapping("/api/daily-work-reports/{id}/final-confirm")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> finalConfirmDailyReport(@PathVariable Long id, @Valid @RequestBody(required = false) ProjectActionDTO dto) {
        service.finalConfirmDailyReport(id, dto);
        return Result.success();
    }

    @PostMapping("/api/daily-work-reports/{id}/reject")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> rejectDailyReport(@PathVariable Long id, @Valid @RequestBody(required = false) ProjectActionDTO dto) {
        service.rejectDailyReport(id, dto);
        return Result.success();
    }

    @PostMapping("/api/daily-work-reports/{id}/files")
    public Result<Void> attachDailyReportFile(@PathVariable Long id, @Valid @RequestBody FileAttachDTO dto, Authentication authentication) {
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
    public Result<TeamSettlementDO> generateTeamSettlement(@Valid @RequestBody GenerateTeamSettlementDTO dto) {
        return Result.success(service.generateTeamSettlement(dto));
    }

    @PostMapping("/api/team-settlements/{id}/confirm")
    @PreAuthorize(RoleExpressions.PROJECT_FINANCE)
    public Result<Void> confirmTeamSettlement(@PathVariable Long id, @Valid @RequestBody(required = false) ProjectActionDTO dto) {
        service.confirmTeamSettlement(id, dto);
        return Result.success();
    }

    @PostMapping("/api/team-settlements/{id}/reject")
    @PreAuthorize(RoleExpressions.PROJECT_FINANCE)
    public Result<Void> rejectTeamSettlement(@PathVariable Long id, @Valid @RequestBody(required = false) ProjectActionDTO dto) {
        service.rejectTeamSettlement(id, dto);
        return Result.success();
    }

    @PostMapping("/api/team-settlements/{id}/mark-paid")
    @PreAuthorize(RoleExpressions.PROJECT_FINANCE)
    public Result<Void> markTeamSettlementPaid(@PathVariable Long id, @Valid @RequestBody(required = false) ProjectActionDTO dto) {
        service.markTeamSettlementPaid(id, dto);
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
    public Result<VariationOrderDO> saveVariationOrder(@Valid @RequestBody ProjectWriteDTO.VariationOrderRequest dto, Authentication authentication) {
        projectAccessService.requireProjectAccess(dto.getProjectId(), authentication);
        VariationOrderDO order = copy(dto, VariationOrderDO.class);
        Long userId = currentUserId(authentication);
        order.setCreatedBy(userId);
        order.setUpdatedBy(userId);
        return Result.success(service.saveVariationOrder(order));
    }

    @PutMapping("/api/variation-orders/{id}")
    public Result<VariationOrderDO> updateVariationOrder(@PathVariable Long id, @Valid @RequestBody ProjectWriteDTO.VariationOrderRequest dto, Authentication authentication) {
        requireVariationOrderOwnerIfNeeded(id, authentication);
        projectAccessService.requireProjectAccess(dto.getProjectId(), authentication);
        VariationOrderDO order = copy(dto, VariationOrderDO.class);
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
    public Result<Void> confirmVariationOrder(@PathVariable Long id, @Valid @RequestBody(required = false) ProjectActionDTO dto) {
        service.confirmVariationOrder(id, dto);
        return Result.success();
    }

    @PostMapping("/api/variation-orders/{id}/reject")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<Void> rejectVariationOrder(@PathVariable Long id, @Valid @RequestBody(required = false) ProjectActionDTO dto) {
        service.rejectVariationOrder(id, dto);
        return Result.success();
    }

    @PostMapping("/api/variation-orders/{id}/files")
    public Result<Void> attachVariationOrderFile(@PathVariable Long id, @Valid @RequestBody FileAttachDTO dto, Authentication authentication) {
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
    public Result<ProjectCostRecordDO> saveCostRecord(@Valid @RequestBody ProjectWriteDTO.ProjectCostRecordRequest dto) {
        return Result.success(service.saveCostRecord(copy(dto, ProjectCostRecordDO.class)));
    }

    @PutMapping("/api/project-cost-records/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_FINANCE)
    public Result<ProjectCostRecordDO> updateCostRecord(@PathVariable Long id, @Valid @RequestBody ProjectWriteDTO.ProjectCostRecordRequest dto) {
        return Result.success(service.updateCostRecord(id, copy(dto, ProjectCostRecordDO.class)));
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
    public Result<ProjectPaymentRecordDO> savePaymentRecord(@Valid @RequestBody ProjectWriteDTO.ProjectPaymentRecordRequest dto) {
        return Result.success(service.savePaymentRecord(copy(dto, ProjectPaymentRecordDO.class)));
    }

    @PutMapping("/api/project-payment-records/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_FINANCE)
    public Result<ProjectPaymentRecordDO> updatePaymentRecord(@PathVariable Long id, @Valid @RequestBody ProjectWriteDTO.ProjectPaymentRecordRequest dto) {
        return Result.success(service.updatePaymentRecord(id, copy(dto, ProjectPaymentRecordDO.class)));
    }

    @GetMapping("/api/machinery-equipments")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<PageResult<MachineryEquipmentDO>> machineryEquipments(@RequestParam(defaultValue = "1") long page,
                                                                        @RequestParam(defaultValue = "20") long pageSize,
                                                                        @RequestParam(required = false) String keyword,
                                                                        @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(service.machineryEquipments(page, pageSize, keyword, status)));
    }

    @PostMapping("/api/machinery-equipments")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<MachineryEquipmentDO> saveMachineryEquipment(@Valid @RequestBody ProjectWriteDTO.MachineryEquipmentRequest dto) {
        return Result.success(service.saveMachineryEquipment(copy(dto, MachineryEquipmentDO.class)));
    }

    @PutMapping("/api/machinery-equipments/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<MachineryEquipmentDO> updateMachineryEquipment(@PathVariable Long id, @Valid @RequestBody ProjectWriteDTO.MachineryEquipmentRequest dto) {
        return Result.success(service.updateMachineryEquipment(id, copy(dto, MachineryEquipmentDO.class)));
    }

    @GetMapping("/api/machinery-work-records")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<PageResult<MachineryWorkRecordDO>> machineryWorkRecords(@RequestParam(defaultValue = "1") long page,
                                                                          @RequestParam(defaultValue = "20") long pageSize,
                                                                          @RequestParam(required = false) Long projectId,
                                                                          @RequestParam(required = false) Long equipmentId) {
        return Result.success(PageResult.of(service.machineryWorkRecords(page, pageSize, projectId, equipmentId)));
    }

    @PostMapping("/api/machinery-work-records")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<MachineryWorkRecordDO> saveMachineryWorkRecord(@Valid @RequestBody ProjectWriteDTO.MachineryWorkRecordRequest dto) {
        return Result.success(service.saveMachineryWorkRecord(copy(dto, MachineryWorkRecordDO.class)));
    }

    @PutMapping("/api/machinery-work-records/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<MachineryWorkRecordDO> updateMachineryWorkRecord(@PathVariable Long id, @Valid @RequestBody ProjectWriteDTO.MachineryWorkRecordRequest dto) {
        return Result.success(service.updateMachineryWorkRecord(id, copy(dto, MachineryWorkRecordDO.class)));
    }

    @GetMapping("/api/material-records")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<PageResult<MaterialRecordDO>> materialRecords(@RequestParam(defaultValue = "1") long page,
                                                                @RequestParam(defaultValue = "20") long pageSize,
                                                                @RequestParam(required = false) Long projectId,
                                                                @RequestParam(required = false) String recordType) {
        return Result.success(PageResult.of(service.materialRecords(page, pageSize, projectId, recordType)));
    }

    @PostMapping("/api/material-records")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<MaterialRecordDO> saveMaterialRecord(@Valid @RequestBody ProjectWriteDTO.MaterialRecordRequest dto) {
        return Result.success(service.saveMaterialRecord(copy(dto, MaterialRecordDO.class)));
    }

    @PutMapping("/api/material-records/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<MaterialRecordDO> updateMaterialRecord(@PathVariable Long id, @Valid @RequestBody ProjectWriteDTO.MaterialRecordRequest dto) {
        return Result.success(service.updateMaterialRecord(id, copy(dto, MaterialRecordDO.class)));
    }

    @GetMapping("/api/project-document-archives")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<PageResult<ProjectDocumentArchiveDO>> documentArchives(@RequestParam(defaultValue = "1") long page,
                                                                         @RequestParam(defaultValue = "20") long pageSize,
                                                                         @RequestParam(required = false) Long projectId,
                                                                         @RequestParam(required = false) String documentType) {
        return Result.success(PageResult.of(service.documentArchives(page, pageSize, projectId, documentType)));
    }

    @PostMapping("/api/project-document-archives")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<ProjectDocumentArchiveDO> saveDocumentArchive(@Valid @RequestBody ProjectWriteDTO.ProjectDocumentArchiveRequest dto) {
        return Result.success(service.saveDocumentArchive(copy(dto, ProjectDocumentArchiveDO.class)));
    }

    @PutMapping("/api/project-document-archives/{id}")
    @PreAuthorize(RoleExpressions.PROJECT_MANAGE)
    public Result<ProjectDocumentArchiveDO> updateDocumentArchive(@PathVariable Long id, @Valid @RequestBody ProjectWriteDTO.ProjectDocumentArchiveRequest dto) {
        return Result.success(service.updateDocumentArchive(id, copy(dto, ProjectDocumentArchiveDO.class)));
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

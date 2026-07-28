package com.company.labor.module.project.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.module.project.domain.dto.FileAttachDTO;
import com.company.labor.module.project.domain.dto.GenerateTeamSettlementDTO;
import com.company.labor.module.project.domain.dto.DailyReportPricingPreviewDTO;
import com.company.labor.module.project.domain.dto.ProjectActionDTO;
import com.company.labor.module.project.domain.dto.VariationConfirmDTO;
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
import com.company.labor.module.project.domain.vo.BusinessFileVO;
import com.company.labor.module.project.domain.vo.DailyReportPricingVO;
import com.company.labor.module.project.domain.vo.DailyWorkReportDetailVO;
import com.company.labor.module.project.domain.vo.TeamSettlementDetailVO;
import com.company.labor.module.project.domain.vo.VariationOrderDetailVO;
import java.util.List;
import java.util.Set;

public interface ProjectManagementService {
    Page<ProjectDO> projects(long page, long pageSize, String keyword, String status, Set<Long> scopedProjectIds);
    ProjectDO project(Long id);
    ProjectDO saveProject(ProjectDO project);
    ProjectDO updateProject(Long id, ProjectDO project);
    void updateProjectStatus(Long id, String status);
    void deleteProject(Long id);
    void restoreProject(Long id);

    Page<ProjectSectionDO> sections(long page, long pageSize, Long projectId, String keyword);
    ProjectSectionDO saveSection(ProjectSectionDO section);
    ProjectSectionDO updateSection(Long id, ProjectSectionDO section);
    void deleteSection(Long id);
    void restoreSection(Long id);

    Page<WorkProcessDO> workProcesses(long page, long pageSize, String keyword, String status);
    WorkProcessDO saveWorkProcess(WorkProcessDO process);
    WorkProcessDO updateWorkProcess(Long id, WorkProcessDO process);
    void deleteWorkProcess(Long id);
    void restoreWorkProcess(Long id);

    Page<TeamDO> teams(long page, long pageSize, String keyword, String status);
    TeamDO saveTeam(TeamDO team);
    TeamDO updateTeam(Long id, TeamDO team);
    void deleteTeam(Long id);
    void restoreTeam(Long id);

    Page<TeamUnitPriceDO> teamUnitPrices(long page, long pageSize, Long projectId, Long teamId);
    TeamUnitPriceDO saveTeamUnitPrice(TeamUnitPriceDO price);
    TeamUnitPriceDO updateTeamUnitPrice(Long id, TeamUnitPriceDO price);

    Page<ProjectProcessPriceDO> projectProcessPrices(long page, long pageSize, Long projectId);
    ProjectProcessPriceDO saveProjectProcessPrice(ProjectProcessPriceDO price);
    ProjectProcessPriceDO updateProjectProcessPrice(Long id, ProjectProcessPriceDO price);

    Page<ProjectContractItemDO> contractItems(long page, long pageSize, Long projectId, Long sectionId, Long processId, String status);
    ProjectContractItemDO saveContractItem(ProjectContractItemDO item);
    ProjectContractItemDO updateContractItem(Long id, ProjectContractItemDO item);
    void deleteContractItem(Long id);

    Page<DailyWorkReportDO> dailyReports(long page, long pageSize, Long projectId, Long teamId, String status, Long createdBy);
    DailyWorkReportDetailVO dailyReportDetail(Long id);
    DailyReportPricingVO previewDailyReportPricing(DailyReportPricingPreviewDTO dto);
    DailyWorkReportDO saveDailyReport(DailyWorkReportDO report);
    DailyWorkReportDO updateDailyReport(Long id, DailyWorkReportDO report);
    void submitDailyReport(Long id);
    void pmConfirmDailyReport(Long id, ProjectActionDTO dto);
    void finalConfirmDailyReport(Long id, ProjectActionDTO dto);
    void rejectDailyReport(Long id, ProjectActionDTO dto);
    void attachDailyReportFile(Long id, FileAttachDTO dto);
    void removeDailyReportFile(Long id, Long fileId);
    List<WorkQuantityConfirmDO> dailyReportConfirms(Long id);
    List<BusinessFileVO> dailyReportFiles(Long id);
    void requireDailyReportOwner(Long id, Long userId);

    Page<TeamSettlementDO> teamSettlements(long page, long pageSize, Long projectId, Long teamId, String settlementMonth);
    TeamSettlementDetailVO teamSettlementDetail(Long id);
    TeamSettlementDO generateTeamSettlement(GenerateTeamSettlementDTO dto);
    void confirmTeamSettlement(Long id, ProjectActionDTO dto);
    void rejectTeamSettlement(Long id, ProjectActionDTO dto);
    void markTeamSettlementPaid(Long id, ProjectActionDTO dto);

    Page<VariationOrderDO> variationOrders(long page, long pageSize, Long projectId, String status, Long createdBy);
    VariationOrderDetailVO variationOrderDetail(Long id);
    VariationOrderDO saveVariationOrder(VariationOrderDO order);
    VariationOrderDO updateVariationOrder(Long id, VariationOrderDO order);
    void submitVariationOrder(Long id);
    void confirmVariationOrder(Long id, VariationConfirmDTO dto);
    void rejectVariationOrder(Long id, ProjectActionDTO dto);
    void attachVariationOrderFile(Long id, FileAttachDTO dto);
    void removeVariationOrderFile(Long id, Long fileId);
    List<VariationOrderConfirmDO> variationOrderConfirms(Long id);
    List<BusinessFileVO> variationOrderFiles(Long id);
    void requireVariationOrderOwner(Long id, Long userId);

    Page<ProjectCostRecordDO> costRecords(long page, long pageSize, Long projectId);
    ProjectCostRecordDO saveCostRecord(ProjectCostRecordDO record);
    ProjectCostRecordDO updateCostRecord(Long id, ProjectCostRecordDO record);

    Page<ProjectPaymentRecordDO> paymentRecords(long page, long pageSize, Long projectId, String recordType);
    ProjectPaymentRecordDO savePaymentRecord(ProjectPaymentRecordDO record);
    ProjectPaymentRecordDO updatePaymentRecord(Long id, ProjectPaymentRecordDO record);

    Page<MachineryEquipmentDO> machineryEquipments(long page, long pageSize, String keyword, String status);
    MachineryEquipmentDO saveMachineryEquipment(MachineryEquipmentDO equipment);
    MachineryEquipmentDO updateMachineryEquipment(Long id, MachineryEquipmentDO equipment);

    Page<MachineryWorkRecordDO> machineryWorkRecords(long page, long pageSize, Long projectId, Long equipmentId);
    MachineryWorkRecordDO saveMachineryWorkRecord(MachineryWorkRecordDO record);
    MachineryWorkRecordDO updateMachineryWorkRecord(Long id, MachineryWorkRecordDO record);

    Page<MaterialRecordDO> materialRecords(long page, long pageSize, Long projectId, String recordType);
    MaterialRecordDO saveMaterialRecord(MaterialRecordDO record);
    MaterialRecordDO updateMaterialRecord(Long id, MaterialRecordDO record);

    Page<ProjectDocumentArchiveDO> documentArchives(long page, long pageSize, Long projectId, String documentType);
    ProjectDocumentArchiveDO saveDocumentArchive(ProjectDocumentArchiveDO archive);
    ProjectDocumentArchiveDO updateDocumentArchive(Long id, ProjectDocumentArchiveDO archive);

    ProjectDashboardVO dashboard(Long projectId);
}

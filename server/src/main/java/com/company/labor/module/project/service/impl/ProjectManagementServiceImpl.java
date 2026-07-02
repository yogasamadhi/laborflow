package com.company.labor.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.softdelete.SoftDeleteGuard;
import com.company.labor.common.softdelete.SoftDeleteGuard.Reference;
import com.company.labor.common.statemachine.BusinessStateMachine;
import com.company.labor.common.statemachine.BusinessStateMachine.Flow;
import com.company.labor.module.file.service.FileBindingGuard;
import com.company.labor.module.project.domain.dto.FileAttachDTO;
import com.company.labor.module.project.domain.dto.GenerateTeamSettlementDTO;
import com.company.labor.module.project.domain.dto.ProjectActionDTO;
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
import com.company.labor.module.project.domain.entity.TeamSettlementDetailDO;
import com.company.labor.module.project.domain.entity.TeamUnitPriceDO;
import com.company.labor.module.project.domain.entity.VariationOrderConfirmDO;
import com.company.labor.module.project.domain.entity.VariationOrderDO;
import com.company.labor.module.project.domain.entity.VariationOrderFileDO;
import com.company.labor.module.project.domain.entity.WorkProcessDO;
import com.company.labor.module.project.domain.entity.WorkQuantityConfirmDO;
import com.company.labor.module.project.domain.vo.ProjectDashboardVO;
import com.company.labor.module.project.domain.vo.ProjectRiskVO;
import com.company.labor.module.project.mapper.DailyWorkReportFileMapper;
import com.company.labor.module.project.mapper.DailyWorkReportMapper;
import com.company.labor.module.project.mapper.MachineryEquipmentMapper;
import com.company.labor.module.project.mapper.MachineryWorkRecordMapper;
import com.company.labor.module.project.mapper.MaterialRecordMapper;
import com.company.labor.module.project.mapper.ProjectCostRecordMapper;
import com.company.labor.module.project.mapper.ProjectContractItemMapper;
import com.company.labor.module.project.mapper.ProjectDocumentArchiveMapper;
import com.company.labor.module.project.mapper.ProjectMapper;
import com.company.labor.module.project.mapper.ProjectPaymentRecordMapper;
import com.company.labor.module.project.mapper.ProjectProcessPriceMapper;
import com.company.labor.module.project.mapper.ProjectSectionMapper;
import com.company.labor.module.project.mapper.TeamMapper;
import com.company.labor.module.project.mapper.TeamSettlementDetailMapper;
import com.company.labor.module.project.mapper.TeamSettlementMapper;
import com.company.labor.module.project.mapper.TeamUnitPriceMapper;
import com.company.labor.module.project.mapper.VariationOrderConfirmMapper;
import com.company.labor.module.project.mapper.VariationOrderFileMapper;
import com.company.labor.module.project.mapper.VariationOrderMapper;
import com.company.labor.module.project.mapper.WorkProcessMapper;
import com.company.labor.module.project.mapper.WorkQuantityConfirmMapper;
import com.company.labor.module.project.service.ProjectManagementService;
import com.company.labor.module.system.service.OperationLogService;
import com.company.labor.security.AuditUserProvider;
import com.company.labor.security.LoginUser;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.YearMonth;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class ProjectManagementServiceImpl implements ProjectManagementService {
    private final ProjectMapper projectMapper;
    private final ProjectSectionMapper sectionMapper;
    private final WorkProcessMapper workProcessMapper;
    private final TeamMapper teamMapper;
    private final TeamUnitPriceMapper teamUnitPriceMapper;
    private final ProjectProcessPriceMapper projectProcessPriceMapper;
    private final ProjectContractItemMapper contractItemMapper;
    private final DailyWorkReportMapper dailyWorkReportMapper;
    private final DailyWorkReportFileMapper dailyWorkReportFileMapper;
    private final WorkQuantityConfirmMapper workQuantityConfirmMapper;
    private final TeamSettlementMapper teamSettlementMapper;
    private final TeamSettlementDetailMapper teamSettlementDetailMapper;
    private final VariationOrderMapper variationOrderMapper;
    private final VariationOrderConfirmMapper variationOrderConfirmMapper;
    private final VariationOrderFileMapper variationOrderFileMapper;
    private final ProjectCostRecordMapper costRecordMapper;
    private final ProjectPaymentRecordMapper paymentRecordMapper;
    private final MachineryEquipmentMapper machineryEquipmentMapper;
    private final MachineryWorkRecordMapper machineryWorkRecordMapper;
    private final MaterialRecordMapper materialRecordMapper;
    private final ProjectDocumentArchiveMapper documentArchiveMapper;
    private final OperationLogService operationLogService;
    private final AuditUserProvider auditUserProvider;
    private final SoftDeleteGuard softDeleteGuard;
    private final FileBindingGuard fileBindingGuard;

    @Override
    public Page<ProjectDO> projects(long page, long pageSize, String keyword, String status, Set<Long> scopedProjectIds) {
        if (scopedProjectIds != null && scopedProjectIds.isEmpty()) {
            return new Page<>(page, pageSize);
        }
        return projectMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<ProjectDO>()
                .eq(ProjectDO::getDeleted, 0)
                .in(scopedProjectIds != null, ProjectDO::getId, scopedProjectIds)
                .like(StringUtils.hasText(keyword), ProjectDO::getName, keyword)
                .eq(StringUtils.hasText(status), ProjectDO::getStatus, status)
                .orderByDesc(ProjectDO::getCreatedAt));
    }

    @Override
    public ProjectDO project(Long id) {
        return requireProject(id);
    }

    @Override
    public ProjectDO saveProject(ProjectDO project) {
        requireText(project.getName(), "项目名称必填");
        project.setStatus(defaultText(project.getStatus(), "not_started"));
        project.setContractAmount(nvl(project.getContractAmount()));
        project.setDeleted(0);
        projectMapper.insert(project);
        return project;
    }

    @Override
    public ProjectDO updateProject(Long id, ProjectDO project) {
        requireProject(id);
        project.setId(id);
        projectMapper.updateById(project);
        return projectMapper.selectById(id);
    }

    @Override
    public void deleteProject(Long id) {
        ProjectDO project = requireProject(id);
        softDeleteGuard.requireNotReferenced("项目", id,
                Reference.active("project_section", "project_id", "施工段"),
                Reference.active("project_member", "project_id", "项目成员"),
                Reference.active("team_unit_price", "project_id", "班组单价"),
                Reference.active("project_process_price", "project_id", "项目工序单价"),
                Reference.active("project_contract_item", "project_id", "项目合同清单"),
                Reference.active("daily_work_report", "project_id", "工程量日报"),
                Reference.active("team_settlement", "project_id", "班组结算"),
                Reference.active("variation_order", "project_id", "签证变更"),
                Reference.active("project_cost_record", "project_id", "成本记录"),
                Reference.active("project_payment_record", "project_id", "回款记录"),
                Reference.active("machinery_work_record", "project_id", "机械台班"),
                Reference.active("material_record", "project_id", "材料记录"),
                Reference.active("project_document_archive", "project_id", "资料归档"),
                Reference.active("attendance_record", "project_id", "考勤记录"),
                Reference.active("performance_plan", "project_id", "绩效方案"),
                Reference.active("performance_score", "project_id", "绩效评分"),
                Reference.active("project_bonus_pool", "project_id", "项目奖金池"),
                Reference.active("project_bonus_allocation", "project_id", "奖金分配"),
                Reference.active("salary_monthly_detail", "project_id", "工资明细"));
        project.setDeleted(1);
        if (projectMapper.updateById(project) <= 0) {
            throw new BusinessException("项目删除失败");
        }
    }

    @Override
    public void restoreProject(Long id) {
        ProjectDO project = projectMapper.selectByIdIncludingDeleted(id);
        if (project == null) throw new BusinessException("项目不存在");
        if (!deleted(project.getDeleted())) {
            return;
        }
        if (projectMapper.restoreById(id) <= 0) {
            throw new BusinessException("项目恢复失败");
        }
    }

    @Override
    public void updateProjectStatus(Long id, String status) {
        ProjectDO project = requireProject(id);
        requireText(status, "项目状态必填");
        project.setStatus(status);
        projectMapper.updateById(project);
    }

    @Override
    public Page<ProjectSectionDO> sections(long page, long pageSize, Long projectId, String keyword) {
        return sectionMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<ProjectSectionDO>()
                .eq(projectId != null, ProjectSectionDO::getProjectId, projectId)
                .eq(ProjectSectionDO::getDeleted, 0)
                .like(StringUtils.hasText(keyword), ProjectSectionDO::getName, keyword)
                .orderByDesc(ProjectSectionDO::getCreatedAt));
    }

    @Override
    public ProjectSectionDO saveSection(ProjectSectionDO section) {
        requireProject(section.getProjectId());
        requireText(section.getName(), "施工段名称必填");
        ensureSectionNameAvailable(section.getProjectId(), section.getName(), null);
        section.setLength(nvl(section.getLength()));
        section.setDeleted(0);
        sectionMapper.insert(section);
        return section;
    }

    @Override
    public ProjectSectionDO updateSection(Long id, ProjectSectionDO section) {
        ProjectSectionDO old = requireSection(id);
        section.setId(id);
        section.setProjectId(old.getProjectId());
        requireText(section.getName(), "施工段名称必填");
        ensureSectionNameAvailable(section.getProjectId(), section.getName(), id);
        sectionMapper.updateById(section);
        return sectionMapper.selectById(id);
    }

    @Override
    public void deleteSection(Long id) {
        ProjectSectionDO section = requireSection(id);
        softDeleteGuard.requireNotReferenced("施工段", id,
                Reference.active("project_contract_item", "project_section_id", "项目合同清单"),
                Reference.active("daily_work_report", "project_section_id", "工程量日报"),
                Reference.active("variation_order", "project_section_id", "签证变更"),
                Reference.active("machinery_work_record", "project_section_id", "机械台班"),
                Reference.active("material_record", "project_section_id", "材料记录"),
                Reference.active("project_document_archive", "project_section_id", "资料归档"));
        section.setDeleted(1);
        if (sectionMapper.updateById(section) <= 0) {
            throw new BusinessException("施工段删除失败");
        }
    }

    @Override
    public void restoreSection(Long id) {
        ProjectSectionDO section = sectionMapper.selectByIdIncludingDeleted(id);
        if (section == null) throw new BusinessException("施工段不存在");
        if (!deleted(section.getDeleted())) {
            return;
        }
        requireProject(section.getProjectId());
        ensureSectionNameAvailable(section.getProjectId(), section.getName(), id);
        if (sectionMapper.restoreById(id) <= 0) {
            throw new BusinessException("施工段恢复失败");
        }
    }

    @Override
    public Page<WorkProcessDO> workProcesses(long page, long pageSize, String keyword, String status) {
        return workProcessMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<WorkProcessDO>()
                .eq(WorkProcessDO::getDeleted, 0)
                .like(StringUtils.hasText(keyword), WorkProcessDO::getName, keyword)
                .eq(StringUtils.hasText(status), WorkProcessDO::getStatus, status)
                .orderByAsc(WorkProcessDO::getSortOrder)
                .orderByDesc(WorkProcessDO::getCreatedAt));
    }

    @Override
    public WorkProcessDO saveWorkProcess(WorkProcessDO process) {
        requireText(process.getName(), "工序名称必填");
        ensureWorkProcessNameAvailable(process.getName(), null);
        process.setUnit(defaultText(process.getUnit(), "m"));
        process.setStatus(defaultText(process.getStatus(), "active"));
        process.setSortOrder(process.getSortOrder() == null ? 0 : process.getSortOrder());
        process.setDeleted(0);
        workProcessMapper.insert(process);
        return process;
    }

    @Override
    public WorkProcessDO updateWorkProcess(Long id, WorkProcessDO process) {
        requireWorkProcess(id);
        process.setId(id);
        requireText(process.getName(), "工序名称必填");
        ensureWorkProcessNameAvailable(process.getName(), id);
        workProcessMapper.updateById(process);
        return workProcessMapper.selectById(id);
    }

    @Override
    public void deleteWorkProcess(Long id) {
        WorkProcessDO process = requireWorkProcess(id);
        softDeleteGuard.requireNotReferenced("工序", id,
                Reference.active("team_unit_price", "work_process_id", "班组单价"),
                Reference.active("project_process_price", "work_process_id", "项目工序单价"),
                Reference.active("project_contract_item", "work_process_id", "项目合同清单"),
                Reference.active("daily_work_report", "work_process_id", "工程量日报"),
                Reference.all("team_settlement_detail", "work_process_id", "班组结算明细"),
                Reference.active("project_document_archive", "work_process_id", "资料归档"));
        process.setDeleted(1);
        if (workProcessMapper.updateById(process) <= 0) {
            throw new BusinessException("工序删除失败");
        }
    }

    @Override
    public void restoreWorkProcess(Long id) {
        WorkProcessDO process = workProcessMapper.selectByIdIncludingDeleted(id);
        if (process == null) throw new BusinessException("工序不存在");
        if (!deleted(process.getDeleted())) {
            return;
        }
        ensureWorkProcessNameAvailable(process.getName(), id);
        if (workProcessMapper.restoreById(id) <= 0) {
            throw new BusinessException("工序恢复失败");
        }
    }

    @Override
    public Page<TeamDO> teams(long page, long pageSize, String keyword, String status) {
        return teamMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<TeamDO>()
                .eq(TeamDO::getDeleted, 0)
                .like(StringUtils.hasText(keyword), TeamDO::getName, keyword)
                .eq(StringUtils.hasText(status), TeamDO::getStatus, status)
                .orderByDesc(TeamDO::getCreatedAt));
    }

    @Override
    public TeamDO saveTeam(TeamDO team) {
        requireText(team.getName(), "班组名称必填");
        ensureTeamNameAvailable(team.getName(), null);
        team.setStatus(defaultText(team.getStatus(), "active"));
        team.setSettlementMethod(defaultText(team.getSettlementMethod(), "quantity"));
        team.setDeleted(0);
        teamMapper.insert(team);
        return team;
    }

    @Override
    public TeamDO updateTeam(Long id, TeamDO team) {
        requireTeam(id);
        team.setId(id);
        requireText(team.getName(), "班组名称必填");
        ensureTeamNameAvailable(team.getName(), id);
        teamMapper.updateById(team);
        return teamMapper.selectById(id);
    }

    @Override
    public void deleteTeam(Long id) {
        TeamDO team = requireTeam(id);
        softDeleteGuard.requireNotReferenced("班组", id,
                Reference.active("team_unit_price", "team_id", "班组单价"),
                Reference.active("daily_work_report", "team_id", "工程量日报"),
                Reference.active("team_settlement", "team_id", "班组结算"));
        team.setDeleted(1);
        if (teamMapper.updateById(team) <= 0) {
            throw new BusinessException("班组删除失败");
        }
    }

    @Override
    public void restoreTeam(Long id) {
        TeamDO team = teamMapper.selectByIdIncludingDeleted(id);
        if (team == null) throw new BusinessException("班组不存在");
        if (!deleted(team.getDeleted())) {
            return;
        }
        ensureTeamNameAvailable(team.getName(), id);
        if (teamMapper.restoreById(id) <= 0) {
            throw new BusinessException("班组恢复失败");
        }
    }

    @Override
    public Page<TeamUnitPriceDO> teamUnitPrices(long page, long pageSize, Long projectId, Long teamId) {
        return teamUnitPriceMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<TeamUnitPriceDO>()
                .eq(TeamUnitPriceDO::getDeleted, 0)
                .eq(projectId != null, TeamUnitPriceDO::getProjectId, projectId)
                .eq(teamId != null, TeamUnitPriceDO::getTeamId, teamId)
                .orderByDesc(TeamUnitPriceDO::getEffectiveDate)
                .orderByDesc(TeamUnitPriceDO::getCreatedAt));
    }

    @Override
    public TeamUnitPriceDO saveTeamUnitPrice(TeamUnitPriceDO price) {
        requireProject(price.getProjectId());
        requireTeam(price.getTeamId());
        requireWorkProcess(price.getWorkProcessId());
        price.setUnitPrice(nvl(price.getUnitPrice()));
        price.setStatus(defaultText(price.getStatus(), "active"));
        price.setDeleted(0);
        teamUnitPriceMapper.insert(price);
        return price;
    }

    @Override
    public TeamUnitPriceDO updateTeamUnitPrice(Long id, TeamUnitPriceDO price) {
        requireTeamUnitPrice(id);
        price.setId(id);
        teamUnitPriceMapper.updateById(price);
        return teamUnitPriceMapper.selectById(id);
    }

    @Override
    public Page<ProjectProcessPriceDO> projectProcessPrices(long page, long pageSize, Long projectId) {
        return projectProcessPriceMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<ProjectProcessPriceDO>()
                .eq(ProjectProcessPriceDO::getDeleted, 0)
                .eq(projectId != null, ProjectProcessPriceDO::getProjectId, projectId)
                .orderByDesc(ProjectProcessPriceDO::getEffectiveDate)
                .orderByDesc(ProjectProcessPriceDO::getCreatedAt));
    }

    @Override
    public ProjectProcessPriceDO saveProjectProcessPrice(ProjectProcessPriceDO price) {
        requireProject(price.getProjectId());
        requireWorkProcess(price.getWorkProcessId());
        price.setUnitPrice(nvl(price.getUnitPrice()));
        price.setStatus(defaultText(price.getStatus(), "active"));
        price.setDeleted(0);
        projectProcessPriceMapper.insert(price);
        return price;
    }

    @Override
    public ProjectProcessPriceDO updateProjectProcessPrice(Long id, ProjectProcessPriceDO price) {
        requireProjectProcessPrice(id);
        price.setId(id);
        projectProcessPriceMapper.updateById(price);
        return projectProcessPriceMapper.selectById(id);
    }

    @Override
    public Page<ProjectContractItemDO> contractItems(long page, long pageSize, Long projectId, Long sectionId, Long processId, String status) {
        return contractItemMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<ProjectContractItemDO>()
                .eq(ProjectContractItemDO::getDeleted, 0)
                .eq(projectId != null, ProjectContractItemDO::getProjectId, projectId)
                .eq(sectionId != null, ProjectContractItemDO::getProjectSectionId, sectionId)
                .eq(processId != null, ProjectContractItemDO::getWorkProcessId, processId)
                .eq(StringUtils.hasText(status), ProjectContractItemDO::getStatus, status)
                .orderByDesc(ProjectContractItemDO::getCreatedAt));
    }

    @Override
    public ProjectContractItemDO saveContractItem(ProjectContractItemDO item) {
        normalizeContractItem(item);
        item.setDeleted(0);
        contractItemMapper.insert(item);
        return item;
    }

    @Override
    public ProjectContractItemDO updateContractItem(Long id, ProjectContractItemDO item) {
        requireContractItem(id);
        item.setId(id);
        normalizeContractItem(item);
        contractItemMapper.updateById(item);
        return contractItemMapper.selectById(id);
    }

    @Override
    public void deleteContractItem(Long id) {
        ProjectContractItemDO item = requireContractItem(id);
        softDeleteGuard.requireNotReferenced("项目合同清单", id,
                Reference.active("daily_work_report", "contract_item_id", "工程量日报"));
        item.setDeleted(1);
        if (contractItemMapper.updateById(item) <= 0) {
            throw new BusinessException("项目合同清单删除失败");
        }
    }

    @Override
    public Page<DailyWorkReportDO> dailyReports(long page, long pageSize, Long projectId, Long teamId, String status, Long createdBy) {
        return dailyWorkReportMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<DailyWorkReportDO>()
                .eq(DailyWorkReportDO::getDeleted, 0)
                .eq(projectId != null, DailyWorkReportDO::getProjectId, projectId)
                .eq(teamId != null, DailyWorkReportDO::getTeamId, teamId)
                .eq(StringUtils.hasText(status), DailyWorkReportDO::getStatus, status)
                .eq(createdBy != null, DailyWorkReportDO::getCreatedBy, createdBy)
                .orderByDesc(DailyWorkReportDO::getWorkDate)
                .orderByDesc(DailyWorkReportDO::getCreatedAt));
    }

    @Override
    public DailyWorkReportDO saveDailyReport(DailyWorkReportDO report) {
        validateDailyReport(report);
        report.setCompletedQuantity(nvl(report.getCompletedQuantity()));
        report.setWorkerCount(report.getWorkerCount() == null ? 0 : report.getWorkerCount());
        applyDailyReportPricing(report);
        report.setStatus("draft");
        report.setDeleted(0);
        dailyWorkReportMapper.insert(report);
        return report;
    }

    @Override
    public DailyWorkReportDO updateDailyReport(Long id, DailyWorkReportDO report) {
        DailyWorkReportDO old = requireDailyReport(id);
        if (!"draft".equals(old.getStatus()) && !"rejected".equals(old.getStatus())) {
            throw new BusinessException("只有草稿或已驳回日报可以修改");
        }
        validateDailyReport(report);
        report.setId(id);
        report.setStatus(old.getStatus());
        report.setCompletedQuantity(nvl(report.getCompletedQuantity()));
        report.setWorkerCount(report.getWorkerCount() == null ? 0 : report.getWorkerCount());
        applyDailyReportPricing(report);
        dailyWorkReportMapper.updateById(report);
        return dailyWorkReportMapper.selectById(id);
    }

    @Override
    public void submitDailyReport(Long id) {
        DailyWorkReportDO report = requireDailyReport(id);
        String statusBefore = report.getStatus();
        BusinessStateMachine.requireTransition(Flow.DAILY_WORK_REPORT, statusBefore, "submitted");
        report.setStatus("submitted");
        report.setRejectedReason(null);
        dailyWorkReportMapper.updateById(report);
        confirmLog(id, "submit", "submitted", statusBefore, report.getStatus(), null);
        operationLogService.record("project", id, "submit_daily_report", "提交工程量日报");
    }

    @Override
    @Transactional
    public void pmConfirmDailyReport(Long id, ProjectActionDTO dto) {
        DailyWorkReportDO report = requireDailyReport(id);
        String statusBefore = report.getStatus();
        BusinessStateMachine.requireTransition(Flow.DAILY_WORK_REPORT, statusBefore, "pm_confirmed");
        report.setStatus("pm_confirmed");
        dailyWorkReportMapper.updateById(report);
        confirmLog(id, "project_manager", "confirmed", statusBefore, report.getStatus(), dto);
        operationLogService.record("project", id, "pm_confirm_daily_report", "项目经理确认工程量日报");
    }

    @Override
    @Transactional
    public void finalConfirmDailyReport(Long id, ProjectActionDTO dto) {
        DailyWorkReportDO report = requireDailyReport(id);
        String statusBefore = report.getStatus();
        BusinessStateMachine.requireTransition(Flow.DAILY_WORK_REPORT, statusBefore, "confirmed");
        report.setStatus("confirmed");
        dailyWorkReportMapper.updateById(report);
        confirmLog(id, "commercial", "confirmed", statusBefore, report.getStatus(), dto);
        operationLogService.record("project", id, "final_confirm_daily_report", "商务终审确认工程量日报");
    }

    @Override
    @Transactional
    public void rejectDailyReport(Long id, ProjectActionDTO dto) {
        DailyWorkReportDO report = requireDailyReport(id);
        String statusBefore = report.getStatus();
        BusinessStateMachine.requireTransition(Flow.DAILY_WORK_REPORT, statusBefore, "rejected");
        report.setStatus("rejected");
        report.setRejectedReason(dto == null ? null : dto.getRemark());
        dailyWorkReportMapper.updateById(report);
        confirmLog(id, "reject", "rejected", statusBefore, report.getStatus(), dto);
        operationLogService.record("project", id, "reject_daily_report", "驳回工程量日报");
    }

    @Override
    public void attachDailyReportFile(Long id, FileAttachDTO dto) {
        DailyWorkReportDO report = requireDailyReport(id);
        if (dto == null || dto.getFileId() == null) throw new BusinessException("附件必填");
        fileBindingGuard.requireProjectFileBindable(dto.getFileId(), report.getProjectId());
        requireDailyReportFileNotBound(id, dto.getFileId());
        DailyWorkReportFileDO file = new DailyWorkReportFileDO();
        file.setDailyWorkReportId(id);
        file.setFileId(dto.getFileId());
        file.setFileType(defaultText(dto.getFileType(), "photo"));
        file.setCreatedAt(LocalDateTime.now());
        dailyWorkReportFileMapper.insert(file);
        confirmLog(id, "attachment", "attached", reportStatus(id), reportStatus(id), remark("绑定日报附件", dto.getFileId(), file.getFileType()));
        operationLogService.record("project", id, "attach_daily_report_file", "绑定工程量日报附件：" + dto.getFileId());
    }

    @Override
    public List<WorkQuantityConfirmDO> dailyReportConfirms(Long id) {
        requireDailyReport(id);
        return workQuantityConfirmMapper.selectList(new LambdaQueryWrapper<WorkQuantityConfirmDO>()
                .eq(WorkQuantityConfirmDO::getDailyWorkReportId, id)
                .orderByDesc(WorkQuantityConfirmDO::getCreatedAt)
                .orderByDesc(WorkQuantityConfirmDO::getId));
    }

    @Override
    public List<DailyWorkReportFileDO> dailyReportFiles(Long id) {
        requireDailyReport(id);
        return dailyWorkReportFileMapper.selectList(new LambdaQueryWrapper<DailyWorkReportFileDO>()
                .eq(DailyWorkReportFileDO::getDailyWorkReportId, id)
                .orderByDesc(DailyWorkReportFileDO::getCreatedAt)
                .orderByDesc(DailyWorkReportFileDO::getId));
    }

    @Override
    public void requireDailyReportOwner(Long id, Long userId) {
        DailyWorkReportDO report = requireDailyReport(id);
        if (userId == null || report.getCreatedBy() == null || !userId.equals(report.getCreatedBy())) {
            throw new BusinessException("无权操作该工程量日报");
        }
    }

    @Override
    public Page<TeamSettlementDO> teamSettlements(long page, long pageSize, Long projectId, Long teamId, String settlementMonth) {
        return teamSettlementMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<TeamSettlementDO>()
                .eq(TeamSettlementDO::getDeleted, 0)
                .eq(projectId != null, TeamSettlementDO::getProjectId, projectId)
                .eq(teamId != null, TeamSettlementDO::getTeamId, teamId)
                .eq(StringUtils.hasText(settlementMonth), TeamSettlementDO::getSettlementMonth, settlementMonth)
                .orderByDesc(TeamSettlementDO::getSettlementMonth)
                .orderByDesc(TeamSettlementDO::getCreatedAt));
    }

    @Override
    @Transactional
    public TeamSettlementDO generateTeamSettlement(GenerateTeamSettlementDTO dto) {
        if (dto == null) throw new BusinessException("结算参数必填");
        ProjectDO project = requireProject(dto.getProjectId());
        TeamDO team = requireTeam(dto.getTeamId());
        requireText(dto.getSettlementMonth(), "结算月份必填");
        YearMonth month = YearMonth.parse(dto.getSettlementMonth());
        if (teamSettlementMapper.selectCount(new LambdaQueryWrapper<TeamSettlementDO>()
                .eq(TeamSettlementDO::getDeleted, 0)
                .eq(TeamSettlementDO::getProjectId, dto.getProjectId())
                .eq(TeamSettlementDO::getTeamId, dto.getTeamId())
                .eq(TeamSettlementDO::getSettlementMonth, dto.getSettlementMonth())) > 0) {
            throw new BusinessException("该班组本月结算已生成，不能重复结算");
        }
        LocalDate start = month.atDay(1);
        LocalDate end = month.plusMonths(1).atDay(1);
        Set<Long> settledReportIds = new HashSet<>();
        for (TeamSettlementDetailDO detail : teamSettlementDetailMapper.selectList(null)) {
            settledReportIds.add(detail.getDailyWorkReportId());
        }
        List<DailyWorkReportDO> reports = dailyWorkReportMapper.selectList(new LambdaQueryWrapper<DailyWorkReportDO>()
                .eq(DailyWorkReportDO::getDeleted, 0)
                .eq(DailyWorkReportDO::getProjectId, dto.getProjectId())
                .eq(DailyWorkReportDO::getTeamId, dto.getTeamId())
                .eq(DailyWorkReportDO::getStatus, "confirmed")
                .ge(DailyWorkReportDO::getWorkDate, start)
                .lt(DailyWorkReportDO::getWorkDate, end)
                .orderByAsc(DailyWorkReportDO::getWorkDate));
        reports = reports.stream().filter(report -> !settledReportIds.contains(report.getId())).toList();
        if (reports.isEmpty()) {
            throw new BusinessException("没有可结算的已确认工程量");
        }

        TeamSettlementDO settlement = new TeamSettlementDO();
        settlement.setProjectId(dto.getProjectId());
        settlement.setTeamId(dto.getTeamId());
        settlement.setProjectName(project.getName());
        settlement.setTeamName(team.getName());
        settlement.setSettlementMonth(dto.getSettlementMonth());
        settlement.setCompensationAmount(nvl(dto.getCompensationAmount()));
        settlement.setDeductionAmount(nvl(dto.getDeductionAmount()));
        settlement.setSettlementStatus("draft");
        settlement.setPaymentStatus("unpaid");
        settlement.setRejectedReason(null);
        settlement.setRemark(dto.getRemark());
        settlement.setDeleted(0);

        BigDecimal quantity = BigDecimal.ZERO;
        BigDecimal amount = BigDecimal.ZERO;
        List<TeamSettlementDetailDO> details = new ArrayList<>();
        for (DailyWorkReportDO report : reports) {
            TeamUnitPriceDO price = latestTeamUnitPrice(report.getProjectId(), report.getTeamId(), report.getWorkProcessId());
            if (price == null) {
                throw new BusinessException("缺少班组单价，日报ID：" + report.getId());
            }
            WorkProcessDO process = requireWorkProcess(report.getWorkProcessId());
            BigDecimal lineQuantity = nvl(report.getCompletedQuantity());
            BigDecimal lineAmount = lineQuantity.multiply(nvl(price.getUnitPrice()));
            quantity = quantity.add(lineQuantity);
            amount = amount.add(lineAmount);

            TeamSettlementDetailDO detail = new TeamSettlementDetailDO();
            detail.setDailyWorkReportId(report.getId());
            detail.setWorkProcessId(report.getWorkProcessId());
            detail.setWorkDate(report.getWorkDate());
            detail.setWorkProcessName(process.getName());
            detail.setQuantity(lineQuantity);
            detail.setUnit(report.getUnit());
            detail.setUnitPrice(nvl(price.getUnitPrice()));
            detail.setAmount(lineAmount);
            detail.setSourceSnapshot(settlementSourceSnapshot(report, process, price, lineQuantity, lineAmount));
            details.add(detail);
        }
        settlement.setConfirmedQuantity(quantity);
        settlement.setSettlementAmount(amount);
        settlement.setActualPayAmount(amount.add(settlement.getCompensationAmount()).subtract(settlement.getDeductionAmount()));
        teamSettlementMapper.insert(settlement);
        for (TeamSettlementDetailDO detail : details) {
            detail.setTeamSettlementId(settlement.getId());
            teamSettlementDetailMapper.insert(detail);
        }
        operationLogService.record("project", settlement.getId(), "generate_team_settlement", "生成班组结算");
        return settlement;
    }

    @Override
    public void confirmTeamSettlement(Long id, ProjectActionDTO dto) {
        TeamSettlementDO settlement = requireTeamSettlement(id);
        String statusBefore = settlement.getSettlementStatus();
        BusinessStateMachine.requireTransition(Flow.TEAM_SETTLEMENT, statusBefore, "confirmed");
        settlement.setSettlementStatus("confirmed");
        settlement.setRejectedReason(null);
        fillSettlementAuditor(settlement, dto);
        teamSettlementMapper.updateById(settlement);
        operationLogService.record("project", id, "confirm_team_settlement", "确认班组结算：" + statusBefore + " -> confirmed");
    }

    @Override
    public void rejectTeamSettlement(Long id, ProjectActionDTO dto) {
        TeamSettlementDO settlement = requireTeamSettlement(id);
        String statusBefore = settlement.getSettlementStatus();
        BusinessStateMachine.requireTransition(Flow.TEAM_SETTLEMENT, statusBefore, "rejected");
        settlement.setSettlementStatus("rejected");
        settlement.setRejectedReason(dto == null ? null : dto.getRemark());
        fillSettlementAuditor(settlement, dto);
        teamSettlementMapper.updateById(settlement);
        operationLogService.record("project", id, "reject_team_settlement", "驳回班组结算：" + statusBefore + " -> rejected");
    }

    @Override
    public void markTeamSettlementPaid(Long id, ProjectActionDTO dto) {
        TeamSettlementDO settlement = requireTeamSettlement(id);
        BusinessStateMachine.requireCurrent(Flow.TEAM_SETTLEMENT, settlement.getSettlementStatus(), "confirmed");
        String statusBefore = settlement.getPaymentStatus();
        BusinessStateMachine.requireTransition(Flow.TEAM_SETTLEMENT_PAYMENT, statusBefore, "paid");
        settlement.setPaymentStatus("paid");
        settlement.setPaidDate(LocalDate.now());
        fillSettlementPayer(settlement, dto);
        teamSettlementMapper.updateById(settlement);
        operationLogService.record("project", id, "mark_team_settlement_paid", "标记班组结算已支付：" + statusBefore + " -> paid");
    }

    @Override
    public Page<VariationOrderDO> variationOrders(long page, long pageSize, Long projectId, String status, Long createdBy) {
        return variationOrderMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<VariationOrderDO>()
                .eq(VariationOrderDO::getDeleted, 0)
                .eq(projectId != null, VariationOrderDO::getProjectId, projectId)
                .eq(StringUtils.hasText(status), VariationOrderDO::getStatus, status)
                .eq(createdBy != null, VariationOrderDO::getCreatedBy, createdBy)
                .orderByDesc(VariationOrderDO::getOccurrenceDate)
                .orderByDesc(VariationOrderDO::getCreatedAt));
    }

    @Override
    public VariationOrderDO saveVariationOrder(VariationOrderDO order) {
        requireProject(order.getProjectId());
        requireText(order.getVariationType(), "签证类型必填");
        if (order.getOccurrenceDate() == null) throw new BusinessException("发生日期必填");
        order.setIncreasedQuantity(nvl(order.getIncreasedQuantity()));
        order.setEstimatedAmount(nvl(order.getEstimatedAmount()));
        order.setConfirmedAmount(BigDecimal.ZERO);
        order.setLaborCount(order.getLaborCount() == null ? 0 : order.getLaborCount());
        order.setStatus("draft");
        order.setDeleted(0);
        variationOrderMapper.insert(order);
        return order;
    }

    @Override
    public VariationOrderDO updateVariationOrder(Long id, VariationOrderDO order) {
        VariationOrderDO old = requireVariationOrder(id);
        if (!"draft".equals(old.getStatus()) && !"rejected".equals(old.getStatus())) {
            throw new BusinessException("只有草稿或已驳回签证可以修改");
        }
        order.setId(id);
        order.setStatus(old.getStatus());
        variationOrderMapper.updateById(order);
        return variationOrderMapper.selectById(id);
    }

    @Override
    public void submitVariationOrder(Long id) {
        VariationOrderDO order = requireVariationOrder(id);
        String statusBefore = order.getStatus();
        BusinessStateMachine.requireTransition(Flow.VARIATION_ORDER, statusBefore, "submitted");
        order.setStatus("submitted");
        order.setRejectedReason(null);
        variationOrderMapper.updateById(order);
        variationConfirmLog(id, "submit", "submitted", statusBefore, order.getStatus(), null);
        operationLogService.record("project", id, "submit_variation_order", "提交签证变更");
    }

    @Override
    public void confirmVariationOrder(Long id, ProjectActionDTO dto) {
        VariationOrderDO order = requireVariationOrder(id);
        String statusBefore = order.getStatus();
        BusinessStateMachine.requireTransition(Flow.VARIATION_ORDER, statusBefore, "confirmed");
        order.setStatus("confirmed");
        order.setConfirmedAmount(dto != null && dto.getConfirmedAmount() != null ? dto.getConfirmedAmount() : nvl(order.getEstimatedAmount()));
        order.setConfirmedAt(LocalDateTime.now());
        fillVariationOrderConfirmer(order, dto);
        order.setConfirmRemark(dto == null ? null : dto.getRemark());
        order.setRejectedReason(null);
        variationOrderMapper.updateById(order);
        variationConfirmLog(id, "commercial", "confirmed", statusBefore, order.getStatus(), dto);
        operationLogService.record("project", id, "confirm_variation_order", "确认签证变更");
    }

    @Override
    public void rejectVariationOrder(Long id, ProjectActionDTO dto) {
        VariationOrderDO order = requireVariationOrder(id);
        String statusBefore = order.getStatus();
        BusinessStateMachine.requireTransition(Flow.VARIATION_ORDER, statusBefore, "rejected");
        order.setStatus("rejected");
        order.setRejectedReason(dto == null ? null : dto.getRemark());
        variationOrderMapper.updateById(order);
        variationConfirmLog(id, "reject", "rejected", statusBefore, order.getStatus(), dto);
        operationLogService.record("project", id, "reject_variation_order", "驳回签证变更");
    }

    @Override
    public void attachVariationOrderFile(Long id, FileAttachDTO dto) {
        VariationOrderDO order = requireVariationOrder(id);
        if (dto == null || dto.getFileId() == null) throw new BusinessException("附件必填");
        fileBindingGuard.requireProjectFileBindable(dto.getFileId(), order.getProjectId());
        requireVariationOrderFileNotBound(id, dto.getFileId());
        VariationOrderFileDO file = new VariationOrderFileDO();
        file.setVariationOrderId(id);
        file.setFileId(dto.getFileId());
        file.setFileType(defaultText(dto.getFileType(), "evidence"));
        file.setCreatedAt(LocalDateTime.now());
        variationOrderFileMapper.insert(file);
        variationConfirmLog(id, "attachment", "attached", variationStatus(id), variationStatus(id), remark("绑定签证附件", dto.getFileId(), file.getFileType()));
        operationLogService.record("project", id, "attach_variation_order_file", "绑定签证附件：" + dto.getFileId());
    }

    @Override
    public List<VariationOrderConfirmDO> variationOrderConfirms(Long id) {
        requireVariationOrder(id);
        return variationOrderConfirmMapper.selectList(new LambdaQueryWrapper<VariationOrderConfirmDO>()
                .eq(VariationOrderConfirmDO::getVariationOrderId, id)
                .orderByDesc(VariationOrderConfirmDO::getCreatedAt)
                .orderByDesc(VariationOrderConfirmDO::getId));
    }

    @Override
    public List<VariationOrderFileDO> variationOrderFiles(Long id) {
        requireVariationOrder(id);
        return variationOrderFileMapper.selectList(new LambdaQueryWrapper<VariationOrderFileDO>()
                .eq(VariationOrderFileDO::getVariationOrderId, id)
                .orderByDesc(VariationOrderFileDO::getCreatedAt)
                .orderByDesc(VariationOrderFileDO::getId));
    }

    @Override
    public void requireVariationOrderOwner(Long id, Long userId) {
        VariationOrderDO order = requireVariationOrder(id);
        if (userId == null || order.getCreatedBy() == null || !userId.equals(order.getCreatedBy())) {
            throw new BusinessException("无权操作该签证变更");
        }
    }

    @Override
    public Page<ProjectCostRecordDO> costRecords(long page, long pageSize, Long projectId) {
        return costRecordMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<ProjectCostRecordDO>()
                .eq(ProjectCostRecordDO::getDeleted, 0)
                .eq(projectId != null, ProjectCostRecordDO::getProjectId, projectId)
                .orderByDesc(ProjectCostRecordDO::getCostDate)
                .orderByDesc(ProjectCostRecordDO::getCreatedAt));
    }

    @Override
    public ProjectCostRecordDO saveCostRecord(ProjectCostRecordDO record) {
        requireProject(record.getProjectId());
        if (record.getCostDate() == null) record.setCostDate(LocalDate.now());
        requireText(record.getCostType(), "成本类型必填");
        record.setAmount(nvl(record.getAmount()));
        record.setDeleted(0);
        costRecordMapper.insert(record);
        return record;
    }

    @Override
    public ProjectCostRecordDO updateCostRecord(Long id, ProjectCostRecordDO record) {
        ProjectCostRecordDO old = costRecordMapper.selectById(id);
        if (old == null || old.getDeleted() != null && old.getDeleted() == 1) throw new BusinessException("成本记录不存在");
        record.setId(id);
        record.setProjectId(old.getProjectId());
        costRecordMapper.updateById(record);
        return costRecordMapper.selectById(id);
    }

    @Override
    public Page<ProjectPaymentRecordDO> paymentRecords(long page, long pageSize, Long projectId, String recordType) {
        return paymentRecordMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<ProjectPaymentRecordDO>()
                .eq(ProjectPaymentRecordDO::getDeleted, 0)
                .eq(projectId != null, ProjectPaymentRecordDO::getProjectId, projectId)
                .eq(StringUtils.hasText(recordType), ProjectPaymentRecordDO::getRecordType, recordType)
                .orderByDesc(ProjectPaymentRecordDO::getPaymentDate)
                .orderByDesc(ProjectPaymentRecordDO::getCreatedAt));
    }

    @Override
    public ProjectPaymentRecordDO savePaymentRecord(ProjectPaymentRecordDO record) {
        requireProject(record.getProjectId());
        if (record.getPaymentDate() == null) record.setPaymentDate(LocalDate.now());
        record.setRecordType(defaultText(record.getRecordType(), "received"));
        record.setAmount(nvl(record.getAmount()));
        validatePaymentVoucher(record.getVoucherFileId(), record.getProjectId(), null);
        record.setDeleted(0);
        paymentRecordMapper.insert(record);
        return record;
    }

    @Override
    public ProjectPaymentRecordDO updatePaymentRecord(Long id, ProjectPaymentRecordDO record) {
        ProjectPaymentRecordDO old = requirePaymentRecord(id);
        record.setId(id);
        record.setProjectId(old.getProjectId());
        validatePaymentVoucher(record.getVoucherFileId(), record.getProjectId(), id);
        paymentRecordMapper.updateById(record);
        return paymentRecordMapper.selectById(id);
    }

    @Override
    public Page<MachineryEquipmentDO> machineryEquipments(long page, long pageSize, String keyword, String status) {
        return machineryEquipmentMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<MachineryEquipmentDO>()
                .eq(MachineryEquipmentDO::getDeleted, 0)
                .like(StringUtils.hasText(keyword), MachineryEquipmentDO::getName, keyword)
                .eq(StringUtils.hasText(status), MachineryEquipmentDO::getStatus, status)
                .orderByDesc(MachineryEquipmentDO::getCreatedAt));
    }

    @Override
    public MachineryEquipmentDO saveMachineryEquipment(MachineryEquipmentDO equipment) {
        requireText(equipment.getName(), "机械设备名称必填");
        requireText(equipment.getEquipmentType(), "机械类型必填");
        equipment.setUnitPrice(nvl(equipment.getUnitPrice()));
        equipment.setStatus(defaultText(equipment.getStatus(), "active"));
        equipment.setDeleted(0);
        machineryEquipmentMapper.insert(equipment);
        return equipment;
    }

    @Override
    public MachineryEquipmentDO updateMachineryEquipment(Long id, MachineryEquipmentDO equipment) {
        requireMachineryEquipment(id);
        equipment.setId(id);
        machineryEquipmentMapper.updateById(equipment);
        return machineryEquipmentMapper.selectById(id);
    }

    @Override
    public Page<MachineryWorkRecordDO> machineryWorkRecords(long page, long pageSize, Long projectId, Long equipmentId) {
        return machineryWorkRecordMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<MachineryWorkRecordDO>()
                .eq(MachineryWorkRecordDO::getDeleted, 0)
                .eq(projectId != null, MachineryWorkRecordDO::getProjectId, projectId)
                .eq(equipmentId != null, MachineryWorkRecordDO::getMachineryEquipmentId, equipmentId)
                .orderByDesc(MachineryWorkRecordDO::getWorkDate)
                .orderByDesc(MachineryWorkRecordDO::getCreatedAt));
    }

    @Override
    public MachineryWorkRecordDO saveMachineryWorkRecord(MachineryWorkRecordDO record) {
        normalizeMachineryWorkRecord(record);
        record.setDeleted(0);
        machineryWorkRecordMapper.insert(record);
        return record;
    }

    @Override
    public MachineryWorkRecordDO updateMachineryWorkRecord(Long id, MachineryWorkRecordDO record) {
        MachineryWorkRecordDO old = requireMachineryWorkRecord(id);
        record.setId(id);
        record.setProjectId(old.getProjectId());
        normalizeMachineryWorkRecord(record);
        machineryWorkRecordMapper.updateById(record);
        return machineryWorkRecordMapper.selectById(id);
    }

    @Override
    public Page<MaterialRecordDO> materialRecords(long page, long pageSize, Long projectId, String recordType) {
        return materialRecordMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<MaterialRecordDO>()
                .eq(MaterialRecordDO::getDeleted, 0)
                .eq(projectId != null, MaterialRecordDO::getProjectId, projectId)
                .eq(StringUtils.hasText(recordType), MaterialRecordDO::getRecordType, recordType)
                .orderByDesc(MaterialRecordDO::getRecordDate)
                .orderByDesc(MaterialRecordDO::getCreatedAt));
    }

    @Override
    public MaterialRecordDO saveMaterialRecord(MaterialRecordDO record) {
        normalizeMaterialRecord(record);
        record.setDeleted(0);
        materialRecordMapper.insert(record);
        return record;
    }

    @Override
    public MaterialRecordDO updateMaterialRecord(Long id, MaterialRecordDO record) {
        MaterialRecordDO old = requireMaterialRecord(id);
        record.setId(id);
        record.setProjectId(old.getProjectId());
        normalizeMaterialRecord(record);
        materialRecordMapper.updateById(record);
        return materialRecordMapper.selectById(id);
    }

    @Override
    public Page<ProjectDocumentArchiveDO> documentArchives(long page, long pageSize, Long projectId, String documentType) {
        return documentArchiveMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<ProjectDocumentArchiveDO>()
                .eq(ProjectDocumentArchiveDO::getDeleted, 0)
                .eq(projectId != null, ProjectDocumentArchiveDO::getProjectId, projectId)
                .eq(StringUtils.hasText(documentType), ProjectDocumentArchiveDO::getDocumentType, documentType)
                .orderByDesc(ProjectDocumentArchiveDO::getArchiveDate)
                .orderByDesc(ProjectDocumentArchiveDO::getCreatedAt));
    }

    @Override
    public ProjectDocumentArchiveDO saveDocumentArchive(ProjectDocumentArchiveDO archive) {
        requireProject(archive.getProjectId());
        if (archive.getProjectSectionId() != null) requireSection(archive.getProjectSectionId());
        if (archive.getWorkProcessId() != null) requireWorkProcess(archive.getWorkProcessId());
        if (archive.getArchiveDate() == null) archive.setArchiveDate(LocalDate.now());
        requireText(archive.getDocumentType(), "资料类型必填");
        if (archive.getFileId() == null) throw new BusinessException("附件必填");
        fileBindingGuard.requireProjectFileBindable(archive.getFileId(), archive.getProjectId());
        requireDocumentArchiveFileNotUsed(archive.getFileId(), null);
        archive.setDeleted(0);
        documentArchiveMapper.insert(archive);
        return archive;
    }

    @Override
    public ProjectDocumentArchiveDO updateDocumentArchive(Long id, ProjectDocumentArchiveDO archive) {
        ProjectDocumentArchiveDO old = requireDocumentArchive(id);
        archive.setId(id);
        archive.setProjectId(old.getProjectId());
        if (archive.getFileId() == null) throw new BusinessException("附件必填");
        fileBindingGuard.requireProjectFileBindable(archive.getFileId(), archive.getProjectId());
        requireDocumentArchiveFileNotUsed(archive.getFileId(), id);
        documentArchiveMapper.updateById(archive);
        return documentArchiveMapper.selectById(id);
    }

    @Override
    public ProjectDashboardVO dashboard(Long projectId) {
        ProjectDO project = requireProject(projectId);
        BigDecimal confirmedQuantity = BigDecimal.ZERO;
        BigDecimal outputValue = BigDecimal.ZERO;
        for (DailyWorkReportDO report : dailyWorkReportMapper.selectList(new LambdaQueryWrapper<DailyWorkReportDO>()
                .eq(DailyWorkReportDO::getDeleted, 0)
                .eq(DailyWorkReportDO::getProjectId, projectId)
                .eq(DailyWorkReportDO::getStatus, "confirmed"))) {
            BigDecimal quantity = nvl(report.getCompletedQuantity());
            confirmedQuantity = confirmedQuantity.add(quantity);
            outputValue = outputValue.add(dailyReportOutputValue(report));
        }

        BigDecimal teamPayable = BigDecimal.ZERO;
        for (TeamSettlementDO settlement : teamSettlementMapper.selectList(new LambdaQueryWrapper<TeamSettlementDO>()
                .eq(TeamSettlementDO::getDeleted, 0)
                .eq(TeamSettlementDO::getProjectId, projectId))) {
            teamPayable = teamPayable.add(nvl(settlement.getActualPayAmount()));
        }

        BigDecimal otherCost = BigDecimal.ZERO;
        for (ProjectCostRecordDO cost : costRecordMapper.selectList(new LambdaQueryWrapper<ProjectCostRecordDO>()
                .eq(ProjectCostRecordDO::getDeleted, 0)
                .eq(ProjectCostRecordDO::getProjectId, projectId))) {
            otherCost = otherCost.add(nvl(cost.getAmount()));
        }

        BigDecimal machineryCost = BigDecimal.ZERO;
        for (MachineryWorkRecordDO record : machineryWorkRecordMapper.selectList(new LambdaQueryWrapper<MachineryWorkRecordDO>()
                .eq(MachineryWorkRecordDO::getDeleted, 0)
                .eq(MachineryWorkRecordDO::getProjectId, projectId))) {
            machineryCost = machineryCost.add(nvl(record.getAmount()));
        }

        BigDecimal materialCost = BigDecimal.ZERO;
        for (MaterialRecordDO record : materialRecordMapper.selectList(new LambdaQueryWrapper<MaterialRecordDO>()
                .eq(MaterialRecordDO::getDeleted, 0)
                .eq(MaterialRecordDO::getProjectId, projectId))) {
            materialCost = materialCost.add(nvl(record.getAmount()));
        }

        BigDecimal receivableAmount = BigDecimal.ZERO;
        BigDecimal receivedAmount = BigDecimal.ZERO;
        boolean hasExplicitReceivable = false;
        for (ProjectPaymentRecordDO record : paymentRecordMapper.selectList(new LambdaQueryWrapper<ProjectPaymentRecordDO>()
                .eq(ProjectPaymentRecordDO::getDeleted, 0)
                .eq(ProjectPaymentRecordDO::getProjectId, projectId))) {
            if ("receivable".equals(record.getRecordType())) {
                hasExplicitReceivable = true;
                receivableAmount = receivableAmount.add(nvl(record.getAmount()));
            } else {
                receivedAmount = receivedAmount.add(nvl(record.getAmount()));
            }
        }
        BigDecimal confirmedVariation = BigDecimal.ZERO;
        for (VariationOrderDO order : variationOrderMapper.selectList(new LambdaQueryWrapper<VariationOrderDO>()
                .eq(VariationOrderDO::getDeleted, 0)
                .eq(VariationOrderDO::getProjectId, projectId)
                .eq(VariationOrderDO::getStatus, "confirmed"))) {
            confirmedVariation = confirmedVariation.add(nvl(order.getConfirmedAmount()));
        }
        if (!hasExplicitReceivable) {
            receivableAmount = nvl(project.getContractAmount()).add(confirmedVariation);
        }
        BigDecimal totalCost = teamPayable.add(otherCost).add(machineryCost).add(materialCost);
        BigDecimal paymentProgress = receivableAmount.compareTo(BigDecimal.ZERO) == 0
                ? BigDecimal.ZERO
                : receivedAmount.divide(receivableAmount, 4, RoundingMode.HALF_UP).multiply(BigDecimal.valueOf(100));

        BigDecimal pendingVariation = BigDecimal.ZERO;
        for (VariationOrderDO order : variationOrderMapper.selectList(new LambdaQueryWrapper<VariationOrderDO>()
                .eq(VariationOrderDO::getDeleted, 0)
                .eq(VariationOrderDO::getProjectId, projectId)
                .eq(VariationOrderDO::getStatus, "submitted"))) {
            pendingVariation = pendingVariation.add(nvl(order.getEstimatedAmount()));
        }

        List<ProjectRiskVO> risks = new ArrayList<>();
        long submittedReports = dailyWorkReportMapper.selectCount(new LambdaQueryWrapper<DailyWorkReportDO>()
                .eq(DailyWorkReportDO::getDeleted, 0)
                .eq(DailyWorkReportDO::getProjectId, projectId)
                .in(DailyWorkReportDO::getStatus, List.of("submitted", "pm_confirmed")));
        if (submittedReports > 0) {
            risks.add(new ProjectRiskVO("quantity_unconfirmed", "warning", "存在 " + submittedReports + " 条工程量已填报但未终审确认"));
        }
        long draftVariationCount = variationOrderMapper.selectCount(new LambdaQueryWrapper<VariationOrderDO>()
                .eq(VariationOrderDO::getDeleted, 0)
                .eq(VariationOrderDO::getProjectId, projectId)
                .eq(VariationOrderDO::getStatus, "draft"));
        if (draftVariationCount > 0) {
            risks.add(new ProjectRiskVO("variation_unsubmitted", "warning", "存在 " + draftVariationCount + " 条已发生但未提交的签证"));
        }
        long staleVariationCount = variationOrderMapper.selectCount(new LambdaQueryWrapper<VariationOrderDO>()
                .eq(VariationOrderDO::getDeleted, 0)
                .eq(VariationOrderDO::getProjectId, projectId)
                .eq(VariationOrderDO::getStatus, "submitted")
                .le(VariationOrderDO::getCreatedAt, LocalDateTime.now().minusDays(7)));
        if (staleVariationCount > 0) {
            risks.add(new ProjectRiskVO("variation_stale", "warning", "存在 " + staleVariationCount + " 条提交超过 7 天未确认的签证"));
        }
        if (pendingVariation.compareTo(BigDecimal.ZERO) > 0) {
            risks.add(new ProjectRiskVO("variation_pending", "warning", "存在待确认签证金额 " + pendingVariation));
        }
        if (receivableAmount.compareTo(BigDecimal.ZERO) > 0 && paymentProgress.compareTo(BigDecimal.valueOf(50)) < 0) {
            risks.add(new ProjectRiskVO("payment_low", "warning", "项目回款比例低于 50%"));
        }
        if (outputValue.compareTo(BigDecimal.ZERO) > 0
                && teamPayable.compareTo(outputValue.multiply(new BigDecimal("0.8"))) >= 0) {
            risks.add(new ProjectRiskVO("team_payable_high", "warning", "班组应付金额已超过产值的 80%"));
        }
        if (nvl(project.getContractAmount()).compareTo(BigDecimal.ZERO) > 0
                && totalCost.compareTo(nvl(project.getContractAmount()).multiply(new BigDecimal("0.8"))) >= 0) {
            risks.add(new ProjectRiskVO("cost_high", "danger", "项目成本已接近合同金额"));
        }
        for (ProjectSectionDO section : sectionMapper.selectList(new LambdaQueryWrapper<ProjectSectionDO>()
                .eq(ProjectSectionDO::getDeleted, 0)
                .eq(ProjectSectionDO::getProjectId, projectId))) {
            Long count = dailyWorkReportMapper.selectCount(new LambdaQueryWrapper<DailyWorkReportDO>()
                    .eq(DailyWorkReportDO::getDeleted, 0)
                    .eq(DailyWorkReportDO::getProjectId, projectId)
                    .eq(DailyWorkReportDO::getProjectSectionId, section.getId())
                    .ge(DailyWorkReportDO::getWorkDate, LocalDate.now().minusDays(14)));
            if (count == 0) {
                risks.add(new ProjectRiskVO("section_no_report", "warning", "施工段“" + section.getName() + "”超过 14 天无日报"));
            }
        }

        ProjectDashboardVO vo = new ProjectDashboardVO();
        vo.setProjectId(projectId);
        vo.setProjectName(project.getName());
        vo.setContractAmount(nvl(project.getContractAmount()));
        vo.setCompletedOutputValue(outputValue);
        vo.setConfirmedQuantity(confirmedQuantity);
        vo.setTeamPayableAmount(teamPayable);
        vo.setOtherCostAmount(otherCost);
        vo.setMachineryCostAmount(machineryCost);
        vo.setMaterialCostAmount(materialCost);
        vo.setTotalCostAmount(totalCost);
        vo.setGrossProfit(outputValue.add(confirmedVariation).subtract(totalCost));
        vo.setReceivableAmount(receivableAmount);
        vo.setConfirmedVariationAmount(confirmedVariation);
        vo.setPendingVariationAmount(pendingVariation);
        vo.setReceivedAmount(receivedAmount);
        vo.setPaymentProgress(paymentProgress);
        vo.setRisks(risks);
        return vo;
    }

    private void validateDailyReport(DailyWorkReportDO report) {
        if (report.getWorkDate() == null) throw new BusinessException("施工日期必填");
        requireProject(report.getProjectId());
        ProjectSectionDO section = requireSection(report.getProjectSectionId());
        if (!report.getProjectId().equals(section.getProjectId())) {
            throw new BusinessException("施工段不属于所选项目");
        }
        requireWorkProcess(report.getWorkProcessId());
        requireText(report.getUnit(), "单位必填");
        if (report.getTeamId() != null) requireTeam(report.getTeamId());
    }

    private void normalizeContractItem(ProjectContractItemDO item) {
        requireProject(item.getProjectId());
        if (item.getProjectSectionId() != null) {
            ProjectSectionDO section = requireSection(item.getProjectSectionId());
            if (!item.getProjectId().equals(section.getProjectId())) {
                throw new BusinessException("合同清单施工段不属于所选项目");
            }
        }
        requireWorkProcess(item.getWorkProcessId());
        requireText(item.getItemName(), "清单名称必填");
        requireText(item.getUnit(), "单位必填");
        item.setContractQuantity(nvl(item.getContractQuantity()));
        item.setUnitPrice(nvl(item.getUnitPrice()));
        item.setContractAmount(item.getContractQuantity().multiply(item.getUnitPrice()).setScale(2, RoundingMode.HALF_UP));
        item.setStatus(defaultText(item.getStatus(), "active"));
    }

    private void applyDailyReportPricing(DailyWorkReportDO report) {
        DailyReportPricing pricing = resolveDailyReportPricing(report);
        BigDecimal outputValue = nvl(report.getCompletedQuantity()).multiply(pricing.unitPrice()).setScale(2, RoundingMode.HALF_UP);
        report.setContractItemId(pricing.contractItemId());
        report.setOutputValue(outputValue);
        report.setPricingBasis(pricing.basis());
    }

    private DailyReportPricing resolveDailyReportPricing(DailyWorkReportDO report) {
        if (report.getContractItemId() != null) {
            ProjectContractItemDO item = requireContractItem(report.getContractItemId());
            if (!"active".equals(item.getStatus())) {
                throw new BusinessException("合同清单已停用");
            }
            requireContractItemMatchesReport(report, item);
            return contractItemPricing(item);
        }
        ProjectContractItemDO item = latestContractItem(report.getProjectId(), report.getProjectSectionId(), report.getWorkProcessId());
        if (item == null) {
            item = latestContractItem(report.getProjectId(), null, report.getWorkProcessId());
        }
        if (item != null) {
            requireContractItemMatchesReport(report, item);
            return contractItemPricing(item);
        }
        ProjectProcessPriceDO price = latestProjectProcessPrice(report.getProjectId(), report.getWorkProcessId());
        if (price != null) {
            return new DailyReportPricing(null, nvl(price.getUnitPrice()), "项目工序产值单价ID:" + price.getId()
                    + ";单位:" + price.getUnit()
                    + ";单价:" + nvl(price.getUnitPrice()));
        }
        return new DailyReportPricing(null, BigDecimal.ZERO, "未配置合同清单或项目工序产值单价，产值按0计算");
    }

    private DailyReportPricing contractItemPricing(ProjectContractItemDO item) {
        String code = StringUtils.hasText(item.getItemCode()) ? item.getItemCode() + "/" : "";
        return new DailyReportPricing(item.getId(), nvl(item.getUnitPrice()), "合同清单ID:" + item.getId()
                + ";清单:" + code + item.getItemName()
                + ";单位:" + item.getUnit()
                + ";单价:" + nvl(item.getUnitPrice()));
    }

    private void requireContractItemMatchesReport(DailyWorkReportDO report, ProjectContractItemDO item) {
        if (!report.getProjectId().equals(item.getProjectId())) {
            throw new BusinessException("合同清单不属于所选项目");
        }
        if (item.getProjectSectionId() != null && !item.getProjectSectionId().equals(report.getProjectSectionId())) {
            throw new BusinessException("合同清单不属于所选施工段");
        }
        if (!report.getWorkProcessId().equals(item.getWorkProcessId())) {
            throw new BusinessException("合同清单不属于所选工序");
        }
        if (!item.getUnit().equals(report.getUnit())) {
            throw new BusinessException("合同清单单位与日报单位不一致");
        }
    }

    private BigDecimal dailyReportOutputValue(DailyWorkReportDO report) {
        if (StringUtils.hasText(report.getPricingBasis())) {
            return nvl(report.getOutputValue());
        }
        ProjectProcessPriceDO price = latestProjectProcessPrice(report.getProjectId(), report.getWorkProcessId());
        return nvl(report.getCompletedQuantity()).multiply(price == null ? BigDecimal.ZERO : nvl(price.getUnitPrice()))
                .setScale(2, RoundingMode.HALF_UP);
    }

    private ProjectContractItemDO latestContractItem(Long projectId, Long sectionId, Long processId) {
        LambdaQueryWrapper<ProjectContractItemDO> wrapper = new LambdaQueryWrapper<ProjectContractItemDO>()
                .eq(ProjectContractItemDO::getDeleted, 0)
                .eq(ProjectContractItemDO::getStatus, "active")
                .eq(ProjectContractItemDO::getProjectId, projectId)
                .eq(ProjectContractItemDO::getWorkProcessId, processId)
                .orderByDesc(ProjectContractItemDO::getCreatedAt)
                .orderByDesc(ProjectContractItemDO::getId)
                .last("LIMIT 1");
        if (sectionId == null) {
            wrapper.isNull(ProjectContractItemDO::getProjectSectionId);
        } else {
            wrapper.eq(ProjectContractItemDO::getProjectSectionId, sectionId);
        }
        return contractItemMapper.selectOne(wrapper);
    }

    private TeamUnitPriceDO latestTeamUnitPrice(Long projectId, Long teamId, Long processId) {
        return teamUnitPriceMapper.selectOne(new LambdaQueryWrapper<TeamUnitPriceDO>()
                .eq(TeamUnitPriceDO::getDeleted, 0)
                .eq(TeamUnitPriceDO::getStatus, "active")
                .eq(TeamUnitPriceDO::getProjectId, projectId)
                .eq(TeamUnitPriceDO::getTeamId, teamId)
                .eq(TeamUnitPriceDO::getWorkProcessId, processId)
                .orderByDesc(TeamUnitPriceDO::getEffectiveDate)
                .orderByDesc(TeamUnitPriceDO::getCreatedAt)
                .last("LIMIT 1"));
    }

    private ProjectProcessPriceDO latestProjectProcessPrice(Long projectId, Long processId) {
        return projectProcessPriceMapper.selectOne(new LambdaQueryWrapper<ProjectProcessPriceDO>()
                .eq(ProjectProcessPriceDO::getDeleted, 0)
                .eq(ProjectProcessPriceDO::getStatus, "active")
                .eq(ProjectProcessPriceDO::getProjectId, projectId)
                .eq(ProjectProcessPriceDO::getWorkProcessId, processId)
                .orderByDesc(ProjectProcessPriceDO::getEffectiveDate)
                .orderByDesc(ProjectProcessPriceDO::getCreatedAt)
                .last("LIMIT 1"));
    }

    private String settlementSourceSnapshot(DailyWorkReportDO report, WorkProcessDO process, TeamUnitPriceDO price, BigDecimal quantity, BigDecimal amount) {
        return "日报ID:" + report.getId()
                + ";日期:" + report.getWorkDate()
                + ";工序:" + process.getName()
                + ";工程量:" + quantity + report.getUnit()
                + ";单价:" + nvl(price.getUnitPrice())
                + ";金额:" + amount
                + ";班组单价ID:" + price.getId();
    }

    private void confirmLog(Long reportId, String stage, String status, String statusBefore, String statusAfter, ProjectActionDTO dto) {
        WorkQuantityConfirmDO confirm = new WorkQuantityConfirmDO();
        confirm.setDailyWorkReportId(reportId);
        confirm.setConfirmStage(stage);
        confirm.setConfirmStatus(status);
        fillOperator(confirm, dto);
        confirm.setStatusBefore(statusBefore);
        confirm.setStatusAfter(statusAfter);
        confirm.setRemark(dto == null ? null : dto.getRemark());
        confirm.setCreatedAt(LocalDateTime.now());
        workQuantityConfirmMapper.insert(confirm);
    }

    private void variationConfirmLog(Long orderId, String stage, String status, String statusBefore, String statusAfter, ProjectActionDTO dto) {
        VariationOrderConfirmDO confirm = new VariationOrderConfirmDO();
        confirm.setVariationOrderId(orderId);
        confirm.setConfirmStage(stage);
        confirm.setConfirmStatus(status);
        fillOperator(confirm, dto);
        confirm.setStatusBefore(statusBefore);
        confirm.setStatusAfter(statusAfter);
        confirm.setRemark(dto == null ? null : dto.getRemark());
        confirm.setCreatedAt(LocalDateTime.now());
        variationOrderConfirmMapper.insert(confirm);
    }

    private ProjectActionDTO remark(String text, Long fileId, String fileType) {
        ProjectActionDTO dto = new ProjectActionDTO();
        dto.setRemark(text + "：" + fileId + "（" + fileType + "）");
        return dto;
    }

    private void fillOperator(WorkQuantityConfirmDO confirm, ProjectActionDTO dto) {
        LoginUser loginUser = auditUserProvider.currentLoginUser();
        if (loginUser != null) {
            confirm.setConfirmerId(loginUser.getUser().getId());
            confirm.setConfirmerName(loginUser.getUsername());
        } else if (dto != null) {
            confirm.setConfirmerId(dto.getOperatorId());
            confirm.setConfirmerName(dto.getOperatorName());
        }
    }

    private void fillOperator(VariationOrderConfirmDO confirm, ProjectActionDTO dto) {
        LoginUser loginUser = auditUserProvider.currentLoginUser();
        if (loginUser != null) {
            confirm.setConfirmerId(loginUser.getUser().getId());
            confirm.setConfirmerName(loginUser.getUsername());
        } else if (dto != null) {
            confirm.setConfirmerId(dto.getOperatorId());
            confirm.setConfirmerName(dto.getOperatorName());
        }
    }

    private void fillVariationOrderConfirmer(VariationOrderDO order, ProjectActionDTO dto) {
        LoginUser loginUser = auditUserProvider.currentLoginUser();
        if (loginUser != null) {
            order.setConfirmerId(loginUser.getUser().getId());
            order.setConfirmerName(loginUser.getUsername());
        } else if (dto != null) {
            order.setConfirmerId(dto.getOperatorId());
            order.setConfirmerName(dto.getOperatorName());
        }
    }

    private void fillSettlementAuditor(TeamSettlementDO settlement, ProjectActionDTO dto) {
        LoginUser loginUser = auditUserProvider.currentLoginUser();
        if (loginUser != null) {
            settlement.setAuditorId(loginUser.getUser().getId());
            settlement.setAuditorName(loginUser.getUsername());
        } else if (dto != null) {
            settlement.setAuditorId(dto.getOperatorId());
            settlement.setAuditorName(dto.getOperatorName());
        }
        settlement.setAuditedAt(LocalDateTime.now());
    }

    private void fillSettlementPayer(TeamSettlementDO settlement, ProjectActionDTO dto) {
        LoginUser loginUser = auditUserProvider.currentLoginUser();
        if (loginUser != null) {
            settlement.setPaidBy(loginUser.getUser().getId());
            settlement.setPaidOperatorName(loginUser.getUsername());
        } else if (dto != null) {
            settlement.setPaidBy(dto.getOperatorId());
            settlement.setPaidOperatorName(dto.getOperatorName());
        }
        settlement.setPaymentRemark(dto == null ? null : dto.getRemark());
    }

    private String reportStatus(Long id) {
        return requireDailyReport(id).getStatus();
    }

    private String variationStatus(Long id) {
        return requireVariationOrder(id).getStatus();
    }

    private void normalizeMachineryWorkRecord(MachineryWorkRecordDO record) {
        requireProject(record.getProjectId());
        if (record.getProjectSectionId() != null) requireSection(record.getProjectSectionId());
        MachineryEquipmentDO equipment = requireMachineryEquipment(record.getMachineryEquipmentId());
        if (record.getWorkDate() == null) record.setWorkDate(LocalDate.now());
        record.setShiftCount(nvl(record.getShiftCount()));
        if (record.getUnitPrice() == null) {
            record.setUnitPrice(nvl(equipment.getUnitPrice()));
        }
        record.setAmount(record.getShiftCount().multiply(nvl(record.getUnitPrice())));
    }

    private void normalizeMaterialRecord(MaterialRecordDO record) {
        requireProject(record.getProjectId());
        if (record.getProjectSectionId() != null) requireSection(record.getProjectSectionId());
        requireText(record.getMaterialName(), "材料名称必填");
        requireText(record.getRecordType(), "材料记录类型必填");
        if (record.getRecordDate() == null) record.setRecordDate(LocalDate.now());
        record.setQuantity(nvl(record.getQuantity()));
        record.setUnitPrice(nvl(record.getUnitPrice()));
        if (record.getAmount() == null || record.getAmount().compareTo(BigDecimal.ZERO) == 0) {
            record.setAmount(record.getQuantity().multiply(record.getUnitPrice()));
        }
    }

    private ProjectDO requireProject(Long id) {
        if (id == null) throw new BusinessException("项目必填");
        ProjectDO row = projectMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("项目不存在");
        return row;
    }

    private ProjectSectionDO requireSection(Long id) {
        if (id == null) throw new BusinessException("施工段必填");
        ProjectSectionDO row = sectionMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("施工段不存在");
        return row;
    }

    private WorkProcessDO requireWorkProcess(Long id) {
        if (id == null) throw new BusinessException("工序必填");
        WorkProcessDO row = workProcessMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("工序不存在");
        return row;
    }

    private TeamDO requireTeam(Long id) {
        if (id == null) throw new BusinessException("班组必填");
        TeamDO row = teamMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("班组不存在");
        return row;
    }

    private TeamUnitPriceDO requireTeamUnitPrice(Long id) {
        TeamUnitPriceDO row = teamUnitPriceMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("班组单价不存在");
        return row;
    }

    private ProjectProcessPriceDO requireProjectProcessPrice(Long id) {
        ProjectProcessPriceDO row = projectProcessPriceMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("项目工序产值单价不存在");
        return row;
    }

    private ProjectContractItemDO requireContractItem(Long id) {
        ProjectContractItemDO row = contractItemMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("项目合同清单不存在");
        return row;
    }

    private DailyWorkReportDO requireDailyReport(Long id) {
        DailyWorkReportDO row = dailyWorkReportMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("工程量日报不存在");
        return row;
    }

    private TeamSettlementDO requireTeamSettlement(Long id) {
        TeamSettlementDO row = teamSettlementMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("班组结算不存在");
        return row;
    }

    private VariationOrderDO requireVariationOrder(Long id) {
        VariationOrderDO row = variationOrderMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("签证不存在");
        return row;
    }

    private ProjectPaymentRecordDO requirePaymentRecord(Long id) {
        ProjectPaymentRecordDO row = paymentRecordMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("回款记录不存在");
        return row;
    }

    private MachineryEquipmentDO requireMachineryEquipment(Long id) {
        if (id == null) throw new BusinessException("机械设备必填");
        MachineryEquipmentDO row = machineryEquipmentMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("机械设备不存在");
        return row;
    }

    private MachineryWorkRecordDO requireMachineryWorkRecord(Long id) {
        MachineryWorkRecordDO row = machineryWorkRecordMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("机械台班记录不存在");
        return row;
    }

    private MaterialRecordDO requireMaterialRecord(Long id) {
        MaterialRecordDO row = materialRecordMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("材料记录不存在");
        return row;
    }

    private ProjectDocumentArchiveDO requireDocumentArchive(Long id) {
        ProjectDocumentArchiveDO row = documentArchiveMapper.selectById(id);
        if (row == null || row.getDeleted() != null && row.getDeleted() == 1) throw new BusinessException("资料归档不存在");
        return row;
    }

    private void validatePaymentVoucher(Long fileId, Long projectId, Long selfId) {
        if (fileId == null) {
            return;
        }
        fileBindingGuard.requireProjectFileBindable(fileId, projectId);
        if (paymentRecordMapper.selectCount(new LambdaQueryWrapper<ProjectPaymentRecordDO>()
                .eq(ProjectPaymentRecordDO::getDeleted, 0)
                .eq(ProjectPaymentRecordDO::getVoucherFileId, fileId)
                .ne(selfId != null, ProjectPaymentRecordDO::getId, selfId)) > 0) {
            throw new BusinessException("回款凭证已被使用");
        }
    }

    private void requireDailyReportFileNotBound(Long dailyReportId, Long fileId) {
        if (dailyWorkReportFileMapper.selectCount(new LambdaQueryWrapper<DailyWorkReportFileDO>()
                .eq(DailyWorkReportFileDO::getDailyWorkReportId, dailyReportId)
                .eq(DailyWorkReportFileDO::getFileId, fileId)) > 0) {
            throw new BusinessException("附件已绑定该日报");
        }
    }

    private void requireVariationOrderFileNotBound(Long variationOrderId, Long fileId) {
        if (variationOrderFileMapper.selectCount(new LambdaQueryWrapper<VariationOrderFileDO>()
                .eq(VariationOrderFileDO::getVariationOrderId, variationOrderId)
                .eq(VariationOrderFileDO::getFileId, fileId)) > 0) {
            throw new BusinessException("附件已绑定该签证");
        }
    }

    private void requireDocumentArchiveFileNotUsed(Long fileId, Long selfId) {
        if (documentArchiveMapper.selectCount(new LambdaQueryWrapper<ProjectDocumentArchiveDO>()
                .eq(ProjectDocumentArchiveDO::getDeleted, 0)
                .eq(ProjectDocumentArchiveDO::getFileId, fileId)
                .ne(selfId != null, ProjectDocumentArchiveDO::getId, selfId)) > 0) {
            throw new BusinessException("附件已被归档");
        }
    }

    private void ensureSectionNameAvailable(Long projectId, String name, Long selfId) {
        Long id = selfId == null ? -1L : selfId;
        if (sectionMapper.countActiveByProjectAndNameExceptId(projectId, name, id) > 0) {
            throw new BusinessException("同项目施工段名称已存在");
        }
    }

    private void ensureWorkProcessNameAvailable(String name, Long selfId) {
        Long id = selfId == null ? -1L : selfId;
        if (workProcessMapper.countActiveByNameExceptId(name, id) > 0) {
            throw new BusinessException("工序名称已存在");
        }
    }

    private void ensureTeamNameAvailable(String name, Long selfId) {
        Long id = selfId == null ? -1L : selfId;
        if (teamMapper.countActiveByNameExceptId(name, id) > 0) {
            throw new BusinessException("班组名称已存在");
        }
    }

    private boolean deleted(Integer deleted) {
        return deleted != null && deleted == 1;
    }

    private void requireText(String value, String message) {
        if (!StringUtils.hasText(value)) throw new BusinessException(message);
    }

    private String defaultText(String value, String defaultValue) {
        return StringUtils.hasText(value) ? value : defaultValue;
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private record DailyReportPricing(Long contractItemId, BigDecimal unitPrice, String basis) {
    }
}

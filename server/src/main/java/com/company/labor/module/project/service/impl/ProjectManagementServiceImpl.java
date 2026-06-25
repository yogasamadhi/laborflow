package com.company.labor.module.project.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.exception.BusinessException;
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
import com.company.labor.module.project.domain.entity.TeamSettlementDetailDO;
import com.company.labor.module.project.domain.entity.TeamUnitPriceDO;
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
import com.company.labor.module.project.mapper.ProjectDocumentArchiveMapper;
import com.company.labor.module.project.mapper.ProjectMapper;
import com.company.labor.module.project.mapper.ProjectPaymentRecordMapper;
import com.company.labor.module.project.mapper.ProjectProcessPriceMapper;
import com.company.labor.module.project.mapper.ProjectSectionMapper;
import com.company.labor.module.project.mapper.TeamMapper;
import com.company.labor.module.project.mapper.TeamSettlementDetailMapper;
import com.company.labor.module.project.mapper.TeamSettlementMapper;
import com.company.labor.module.project.mapper.TeamUnitPriceMapper;
import com.company.labor.module.project.mapper.VariationOrderFileMapper;
import com.company.labor.module.project.mapper.VariationOrderMapper;
import com.company.labor.module.project.mapper.WorkProcessMapper;
import com.company.labor.module.project.mapper.WorkQuantityConfirmMapper;
import com.company.labor.module.project.service.ProjectManagementService;
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
    private final DailyWorkReportMapper dailyWorkReportMapper;
    private final DailyWorkReportFileMapper dailyWorkReportFileMapper;
    private final WorkQuantityConfirmMapper workQuantityConfirmMapper;
    private final TeamSettlementMapper teamSettlementMapper;
    private final TeamSettlementDetailMapper teamSettlementDetailMapper;
    private final VariationOrderMapper variationOrderMapper;
    private final VariationOrderFileMapper variationOrderFileMapper;
    private final ProjectCostRecordMapper costRecordMapper;
    private final ProjectPaymentRecordMapper paymentRecordMapper;
    private final MachineryEquipmentMapper machineryEquipmentMapper;
    private final MachineryWorkRecordMapper machineryWorkRecordMapper;
    private final MaterialRecordMapper materialRecordMapper;
    private final ProjectDocumentArchiveMapper documentArchiveMapper;

    @Override
    public Page<ProjectDO> projects(long page, long pageSize, String keyword, String status) {
        return projectMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<ProjectDO>()
                .eq(ProjectDO::getDeleted, 0)
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
        project.setDeleted(1);
        projectMapper.updateById(project);
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
        sectionMapper.updateById(section);
        return sectionMapper.selectById(id);
    }

    @Override
    public void deleteSection(Long id) {
        ProjectSectionDO section = requireSection(id);
        section.setDeleted(1);
        sectionMapper.updateById(section);
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
        workProcessMapper.updateById(process);
        return workProcessMapper.selectById(id);
    }

    @Override
    public void deactivateWorkProcess(Long id) {
        WorkProcessDO process = requireWorkProcess(id);
        process.setStatus("inactive");
        workProcessMapper.updateById(process);
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
        teamMapper.updateById(team);
        return teamMapper.selectById(id);
    }

    @Override
    public void deactivateTeam(Long id) {
        TeamDO team = requireTeam(id);
        team.setStatus("inactive");
        teamMapper.updateById(team);
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
    public Page<DailyWorkReportDO> dailyReports(long page, long pageSize, Long projectId, Long teamId, String status) {
        return dailyWorkReportMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<DailyWorkReportDO>()
                .eq(DailyWorkReportDO::getDeleted, 0)
                .eq(projectId != null, DailyWorkReportDO::getProjectId, projectId)
                .eq(teamId != null, DailyWorkReportDO::getTeamId, teamId)
                .eq(StringUtils.hasText(status), DailyWorkReportDO::getStatus, status)
                .orderByDesc(DailyWorkReportDO::getWorkDate)
                .orderByDesc(DailyWorkReportDO::getCreatedAt));
    }

    @Override
    public DailyWorkReportDO saveDailyReport(DailyWorkReportDO report) {
        validateDailyReport(report);
        report.setCompletedQuantity(nvl(report.getCompletedQuantity()));
        report.setWorkerCount(report.getWorkerCount() == null ? 0 : report.getWorkerCount());
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
        dailyWorkReportMapper.updateById(report);
        return dailyWorkReportMapper.selectById(id);
    }

    @Override
    public void submitDailyReport(Long id) {
        DailyWorkReportDO report = requireDailyReport(id);
        requireAnyStatus(report.getStatus(), "draft", "rejected");
        report.setStatus("submitted");
        report.setRejectedReason(null);
        dailyWorkReportMapper.updateById(report);
    }

    @Override
    @Transactional
    public void pmConfirmDailyReport(Long id, ProjectActionDTO dto) {
        DailyWorkReportDO report = requireDailyReport(id);
        requireAnyStatus(report.getStatus(), "submitted");
        report.setStatus("pm_confirmed");
        dailyWorkReportMapper.updateById(report);
        confirmLog(id, "project_manager", "confirmed", dto);
    }

    @Override
    @Transactional
    public void finalConfirmDailyReport(Long id, ProjectActionDTO dto) {
        DailyWorkReportDO report = requireDailyReport(id);
        requireAnyStatus(report.getStatus(), "pm_confirmed");
        report.setStatus("confirmed");
        dailyWorkReportMapper.updateById(report);
        confirmLog(id, "commercial", "confirmed", dto);
    }

    @Override
    @Transactional
    public void rejectDailyReport(Long id, ProjectActionDTO dto) {
        DailyWorkReportDO report = requireDailyReport(id);
        requireAnyStatus(report.getStatus(), "submitted", "pm_confirmed");
        report.setStatus("rejected");
        report.setRejectedReason(dto == null ? null : dto.getRemark());
        dailyWorkReportMapper.updateById(report);
        confirmLog(id, "reject", "rejected", dto);
    }

    @Override
    public void attachDailyReportFile(Long id, FileAttachDTO dto) {
        requireDailyReport(id);
        if (dto == null || dto.getFileId() == null) throw new BusinessException("附件必填");
        DailyWorkReportFileDO file = new DailyWorkReportFileDO();
        file.setDailyWorkReportId(id);
        file.setFileId(dto.getFileId());
        file.setFileType(defaultText(dto.getFileType(), "photo"));
        file.setCreatedAt(LocalDateTime.now());
        dailyWorkReportFileMapper.insert(file);
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
        requireProject(dto.getProjectId());
        requireTeam(dto.getTeamId());
        requireText(dto.getSettlementMonth(), "结算月份必填");
        YearMonth month = YearMonth.parse(dto.getSettlementMonth());
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
        settlement.setSettlementMonth(dto.getSettlementMonth());
        settlement.setCompensationAmount(nvl(dto.getCompensationAmount()));
        settlement.setDeductionAmount(nvl(dto.getDeductionAmount()));
        settlement.setSettlementStatus("draft");
        settlement.setPaymentStatus("unpaid");
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
            BigDecimal lineQuantity = nvl(report.getCompletedQuantity());
            BigDecimal lineAmount = lineQuantity.multiply(nvl(price.getUnitPrice()));
            quantity = quantity.add(lineQuantity);
            amount = amount.add(lineAmount);

            TeamSettlementDetailDO detail = new TeamSettlementDetailDO();
            detail.setDailyWorkReportId(report.getId());
            detail.setWorkProcessId(report.getWorkProcessId());
            detail.setQuantity(lineQuantity);
            detail.setUnit(report.getUnit());
            detail.setUnitPrice(nvl(price.getUnitPrice()));
            detail.setAmount(lineAmount);
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
        return settlement;
    }

    @Override
    public void confirmTeamSettlement(Long id) {
        TeamSettlementDO settlement = requireTeamSettlement(id);
        settlement.setSettlementStatus("confirmed");
        teamSettlementMapper.updateById(settlement);
    }

    @Override
    public void markTeamSettlementPaid(Long id) {
        TeamSettlementDO settlement = requireTeamSettlement(id);
        settlement.setPaymentStatus("paid");
        settlement.setPaidDate(LocalDate.now());
        teamSettlementMapper.updateById(settlement);
    }

    @Override
    public Page<VariationOrderDO> variationOrders(long page, long pageSize, Long projectId, String status) {
        return variationOrderMapper.selectPage(new Page<>(page, pageSize), new LambdaQueryWrapper<VariationOrderDO>()
                .eq(VariationOrderDO::getDeleted, 0)
                .eq(projectId != null, VariationOrderDO::getProjectId, projectId)
                .eq(StringUtils.hasText(status), VariationOrderDO::getStatus, status)
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
        requireAnyStatus(order.getStatus(), "draft", "rejected");
        order.setStatus("submitted");
        order.setRejectedReason(null);
        variationOrderMapper.updateById(order);
    }

    @Override
    public void confirmVariationOrder(Long id, ProjectActionDTO dto) {
        VariationOrderDO order = requireVariationOrder(id);
        requireAnyStatus(order.getStatus(), "submitted");
        order.setStatus("confirmed");
        variationOrderMapper.updateById(order);
    }

    @Override
    public void rejectVariationOrder(Long id, ProjectActionDTO dto) {
        VariationOrderDO order = requireVariationOrder(id);
        requireAnyStatus(order.getStatus(), "submitted");
        order.setStatus("rejected");
        order.setRejectedReason(dto == null ? null : dto.getRemark());
        variationOrderMapper.updateById(order);
    }

    @Override
    public void attachVariationOrderFile(Long id, FileAttachDTO dto) {
        requireVariationOrder(id);
        if (dto == null || dto.getFileId() == null) throw new BusinessException("附件必填");
        VariationOrderFileDO file = new VariationOrderFileDO();
        file.setVariationOrderId(id);
        file.setFileId(dto.getFileId());
        file.setFileType(defaultText(dto.getFileType(), "evidence"));
        file.setCreatedAt(LocalDateTime.now());
        variationOrderFileMapper.insert(file);
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
        record.setDeleted(0);
        paymentRecordMapper.insert(record);
        return record;
    }

    @Override
    public ProjectPaymentRecordDO updatePaymentRecord(Long id, ProjectPaymentRecordDO record) {
        ProjectPaymentRecordDO old = requirePaymentRecord(id);
        record.setId(id);
        record.setProjectId(old.getProjectId());
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
        archive.setDeleted(0);
        documentArchiveMapper.insert(archive);
        return archive;
    }

    @Override
    public ProjectDocumentArchiveDO updateDocumentArchive(Long id, ProjectDocumentArchiveDO archive) {
        ProjectDocumentArchiveDO old = requireDocumentArchive(id);
        archive.setId(id);
        archive.setProjectId(old.getProjectId());
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
            ProjectProcessPriceDO price = latestProjectProcessPrice(projectId, report.getWorkProcessId());
            outputValue = outputValue.add(quantity.multiply(price == null ? BigDecimal.ZERO : nvl(price.getUnitPrice())));
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
        for (ProjectPaymentRecordDO record : paymentRecordMapper.selectList(new LambdaQueryWrapper<ProjectPaymentRecordDO>()
                .eq(ProjectPaymentRecordDO::getDeleted, 0)
                .eq(ProjectPaymentRecordDO::getProjectId, projectId))) {
            if ("receivable".equals(record.getRecordType())) {
                receivableAmount = receivableAmount.add(nvl(record.getAmount()));
            } else {
                receivedAmount = receivedAmount.add(nvl(record.getAmount()));
            }
        }
        if (receivableAmount.compareTo(BigDecimal.ZERO) == 0) {
            receivableAmount = nvl(project.getContractAmount());
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
        vo.setGrossProfit(outputValue.subtract(totalCost));
        vo.setReceivableAmount(receivableAmount);
        vo.setPendingVariationAmount(pendingVariation);
        vo.setReceivedAmount(receivedAmount);
        vo.setPaymentProgress(paymentProgress);
        vo.setRisks(risks);
        return vo;
    }

    private void validateDailyReport(DailyWorkReportDO report) {
        if (report.getWorkDate() == null) throw new BusinessException("施工日期必填");
        requireProject(report.getProjectId());
        requireSection(report.getProjectSectionId());
        requireWorkProcess(report.getWorkProcessId());
        requireText(report.getUnit(), "单位必填");
        if (report.getTeamId() != null) requireTeam(report.getTeamId());
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

    private void confirmLog(Long reportId, String stage, String status, ProjectActionDTO dto) {
        WorkQuantityConfirmDO confirm = new WorkQuantityConfirmDO();
        confirm.setDailyWorkReportId(reportId);
        confirm.setConfirmStage(stage);
        confirm.setConfirmStatus(status);
        if (dto != null) {
            confirm.setConfirmerId(dto.getOperatorId());
            confirm.setConfirmerName(dto.getOperatorName());
            confirm.setRemark(dto.getRemark());
        }
        confirm.setCreatedAt(LocalDateTime.now());
        workQuantityConfirmMapper.insert(confirm);
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

    private void requireAnyStatus(String actual, String... expected) {
        for (String status : expected) {
            if (status.equals(actual)) return;
        }
        throw new BusinessException("当前状态不允许操作");
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
}

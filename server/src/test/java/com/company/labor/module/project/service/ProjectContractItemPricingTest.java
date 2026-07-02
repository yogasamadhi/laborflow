package com.company.labor.module.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.labor.common.softdelete.SoftDeleteGuard;
import com.company.labor.module.file.service.FileBindingGuard;
import com.company.labor.module.project.domain.entity.DailyWorkReportDO;
import com.company.labor.module.project.domain.entity.ProjectContractItemDO;
import com.company.labor.module.project.domain.entity.ProjectDO;
import com.company.labor.module.project.domain.entity.ProjectSectionDO;
import com.company.labor.module.project.domain.entity.WorkProcessDO;
import com.company.labor.module.project.mapper.DailyWorkReportFileMapper;
import com.company.labor.module.project.mapper.DailyWorkReportMapper;
import com.company.labor.module.project.mapper.MachineryEquipmentMapper;
import com.company.labor.module.project.mapper.MachineryWorkRecordMapper;
import com.company.labor.module.project.mapper.MaterialRecordMapper;
import com.company.labor.module.project.mapper.ProjectContractItemMapper;
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
import com.company.labor.module.project.mapper.VariationOrderConfirmMapper;
import com.company.labor.module.project.mapper.VariationOrderFileMapper;
import com.company.labor.module.project.mapper.VariationOrderMapper;
import com.company.labor.module.project.mapper.WorkProcessMapper;
import com.company.labor.module.project.mapper.WorkQuantityConfirmMapper;
import com.company.labor.module.project.service.impl.ProjectManagementServiceImpl;
import com.company.labor.module.system.service.OperationLogService;
import com.company.labor.security.AuditUserProvider;
import java.math.BigDecimal;
import java.time.LocalDate;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class ProjectContractItemPricingTest {
    @Test
    void dailyReportSnapshotsOutputValueFromSelectedContractItem() {
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ProjectSectionMapper sectionMapper = mock(ProjectSectionMapper.class);
        WorkProcessMapper processMapper = mock(WorkProcessMapper.class);
        ProjectContractItemMapper contractItemMapper = mock(ProjectContractItemMapper.class);
        DailyWorkReportMapper dailyWorkReportMapper = mock(DailyWorkReportMapper.class);
        ProjectManagementServiceImpl service = service(projectMapper, sectionMapper, processMapper, contractItemMapper, dailyWorkReportMapper);

        when(projectMapper.selectById(1L)).thenReturn(project(1L));
        when(sectionMapper.selectById(2L)).thenReturn(section(2L, 1L));
        when(processMapper.selectById(3L)).thenReturn(process(3L));
        when(contractItemMapper.selectById(4L)).thenReturn(contractItem());

        DailyWorkReportDO report = new DailyWorkReportDO();
        report.setProjectId(1L);
        report.setProjectSectionId(2L);
        report.setWorkProcessId(3L);
        report.setContractItemId(4L);
        report.setWorkDate(LocalDate.of(2026, 7, 2));
        report.setCompletedQuantity(new BigDecimal("12.50"));
        report.setUnit("m");

        service.saveDailyReport(report);

        ArgumentCaptor<DailyWorkReportDO> captor = ArgumentCaptor.forClass(DailyWorkReportDO.class);
        verify(dailyWorkReportMapper).insert(captor.capture());
        DailyWorkReportDO saved = captor.getValue();
        assertThat(saved.getOutputValue()).isEqualByComparingTo("107.50");
        assertThat(saved.getPricingBasis()).contains("合同清单ID:4", "单价:8.60");
        assertThat(saved.getContractItemId()).isEqualTo(4L);
    }

    @Test
    void contractItemAmountIsCalculatedFromQuantityAndUnitPrice() {
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ProjectSectionMapper sectionMapper = mock(ProjectSectionMapper.class);
        WorkProcessMapper processMapper = mock(WorkProcessMapper.class);
        ProjectContractItemMapper contractItemMapper = mock(ProjectContractItemMapper.class);
        ProjectManagementServiceImpl service = service(projectMapper, sectionMapper, processMapper, contractItemMapper, mock(DailyWorkReportMapper.class));

        when(projectMapper.selectById(1L)).thenReturn(project(1L));
        when(sectionMapper.selectById(2L)).thenReturn(section(2L, 1L));
        when(processMapper.selectById(3L)).thenReturn(process(3L));

        ProjectContractItemDO item = contractItem();
        item.setContractQuantity(new BigDecimal("100.00"));
        item.setUnitPrice(new BigDecimal("8.60"));

        service.saveContractItem(item);

        ArgumentCaptor<ProjectContractItemDO> captor = ArgumentCaptor.forClass(ProjectContractItemDO.class);
        verify(contractItemMapper).insert(captor.capture());
        assertThat(captor.getValue().getContractAmount()).isEqualByComparingTo("860.00");
        assertThat(captor.getValue().getStatus()).isEqualTo("active");
    }

    private ProjectManagementServiceImpl service(ProjectMapper projectMapper,
                                                 ProjectSectionMapper sectionMapper,
                                                 WorkProcessMapper processMapper,
                                                 ProjectContractItemMapper contractItemMapper,
                                                 DailyWorkReportMapper dailyWorkReportMapper) {
        return new ProjectManagementServiceImpl(
                projectMapper,
                sectionMapper,
                processMapper,
                mock(TeamMapper.class),
                mock(TeamUnitPriceMapper.class),
                mock(ProjectProcessPriceMapper.class),
                contractItemMapper,
                dailyWorkReportMapper,
                mock(DailyWorkReportFileMapper.class),
                mock(WorkQuantityConfirmMapper.class),
                mock(TeamSettlementMapper.class),
                mock(TeamSettlementDetailMapper.class),
                mock(VariationOrderMapper.class),
                mock(VariationOrderConfirmMapper.class),
                mock(VariationOrderFileMapper.class),
                mock(ProjectCostRecordMapper.class),
                mock(ProjectPaymentRecordMapper.class),
                mock(MachineryEquipmentMapper.class),
                mock(MachineryWorkRecordMapper.class),
                mock(MaterialRecordMapper.class),
                mock(ProjectDocumentArchiveMapper.class),
                mock(OperationLogService.class),
                mock(AuditUserProvider.class),
                mock(SoftDeleteGuard.class),
                mock(FileBindingGuard.class));
    }

    private ProjectDO project(Long id) {
        ProjectDO project = new ProjectDO();
        project.setId(id);
        project.setDeleted(0);
        return project;
    }

    private ProjectSectionDO section(Long id, Long projectId) {
        ProjectSectionDO section = new ProjectSectionDO();
        section.setId(id);
        section.setProjectId(projectId);
        section.setDeleted(0);
        return section;
    }

    private WorkProcessDO process(Long id) {
        WorkProcessDO process = new WorkProcessDO();
        process.setId(id);
        process.setDeleted(0);
        return process;
    }

    private ProjectContractItemDO contractItem() {
        ProjectContractItemDO item = new ProjectContractItemDO();
        item.setId(4L);
        item.setProjectId(1L);
        item.setProjectSectionId(2L);
        item.setWorkProcessId(3L);
        item.setItemCode("A-001");
        item.setItemName("沟槽开挖");
        item.setUnit("m");
        item.setContractQuantity(BigDecimal.TEN);
        item.setUnitPrice(new BigDecimal("8.60"));
        item.setStatus("active");
        item.setDeleted(0);
        return item;
    }
}

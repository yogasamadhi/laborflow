package com.company.labor.module.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.labor.common.softdelete.SoftDeleteGuard;
import com.company.labor.module.file.service.FileBindingGuard;
import com.company.labor.module.project.domain.entity.ProjectDO;
import com.company.labor.module.project.domain.entity.ProjectPaymentRecordDO;
import com.company.labor.module.project.domain.vo.ProjectDashboardVO;
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
import java.util.List;
import org.junit.jupiter.api.Test;

class ProjectPaymentDashboardTest {
    @Test
    void dashboardSummarizesInvoicesReceiptsAndRetention() {
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ProjectSectionMapper sectionMapper = mock(ProjectSectionMapper.class);
        DailyWorkReportMapper dailyReportMapper = mock(DailyWorkReportMapper.class);
        TeamSettlementMapper settlementMapper = mock(TeamSettlementMapper.class);
        ProjectCostRecordMapper costRecordMapper = mock(ProjectCostRecordMapper.class);
        ProjectPaymentRecordMapper paymentRecordMapper = mock(ProjectPaymentRecordMapper.class);
        MachineryWorkRecordMapper machineryWorkRecordMapper = mock(MachineryWorkRecordMapper.class);
        MaterialRecordMapper materialRecordMapper = mock(MaterialRecordMapper.class);
        VariationOrderMapper variationOrderMapper = mock(VariationOrderMapper.class);
        ProjectManagementServiceImpl service = new ProjectManagementServiceImpl(
                projectMapper,
                sectionMapper,
                mock(WorkProcessMapper.class),
                mock(TeamMapper.class),
                mock(TeamUnitPriceMapper.class),
                mock(ProjectProcessPriceMapper.class),
                mock(ProjectContractItemMapper.class),
                dailyReportMapper,
                mock(DailyWorkReportFileMapper.class),
                mock(WorkQuantityConfirmMapper.class),
                settlementMapper,
                mock(TeamSettlementDetailMapper.class),
                variationOrderMapper,
                mock(VariationOrderConfirmMapper.class),
                mock(VariationOrderFileMapper.class),
                costRecordMapper,
                paymentRecordMapper,
                mock(MachineryEquipmentMapper.class),
                machineryWorkRecordMapper,
                materialRecordMapper,
                mock(ProjectDocumentArchiveMapper.class),
                mock(OperationLogService.class),
                mock(AuditUserProvider.class),
                mock(SoftDeleteGuard.class),
                mock(FileBindingGuard.class));

        when(projectMapper.selectById(1L)).thenReturn(project());
        when(dailyReportMapper.selectList(any())).thenReturn(List.of());
        when(settlementMapper.selectList(any())).thenReturn(List.of());
        when(costRecordMapper.selectList(any())).thenReturn(List.of());
        when(paymentRecordMapper.selectList(any())).thenReturn(List.of(
                payment("receivable", "10000.00"),
                payment("received", "4000.00"),
                payment("invoice", "7000.00"),
                payment("retention_withheld", "500.00"),
                payment("retention_returned", "100.00")));
        when(machineryWorkRecordMapper.selectList(any())).thenReturn(List.of());
        when(materialRecordMapper.selectList(any())).thenReturn(List.of());
        when(variationOrderMapper.selectList(any())).thenReturn(List.of(), List.of());
        when(dailyReportMapper.selectCount(any())).thenReturn(0L);
        when(variationOrderMapper.selectCount(any())).thenReturn(0L);
        when(sectionMapper.selectList(any())).thenReturn(List.of());

        ProjectDashboardVO dashboard = service.dashboard(1L);

        assertThat(dashboard.getReceivableAmount()).isEqualByComparingTo("10000.00");
        assertThat(dashboard.getReceivedAmount()).isEqualByComparingTo("4100.00");
        assertThat(dashboard.getInvoicedAmount()).isEqualByComparingTo("7000.00");
        assertThat(dashboard.getRetentionAmount()).isEqualByComparingTo("500.00");
        assertThat(dashboard.getRetentionReturnedAmount()).isEqualByComparingTo("100.00");
        assertThat(dashboard.getRetentionBalanceAmount()).isEqualByComparingTo("400.00");
        assertThat(dashboard.getPaymentProgress()).isEqualByComparingTo("41.00");
    }

    private ProjectDO project() {
        ProjectDO project = new ProjectDO();
        project.setId(1L);
        project.setName("项目A");
        project.setContractAmount(new BigDecimal("10000.00"));
        project.setDeleted(0);
        return project;
    }

    private ProjectPaymentRecordDO payment(String type, String amount) {
        ProjectPaymentRecordDO record = new ProjectPaymentRecordDO();
        record.setProjectId(1L);
        record.setRecordType(type);
        record.setAmount(new BigDecimal(amount));
        record.setDeleted(0);
        return record;
    }
}

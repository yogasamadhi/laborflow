package com.company.labor.module.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.labor.common.softdelete.SoftDeleteGuard;
import com.company.labor.module.file.service.FileBindingGuard;
import com.company.labor.module.project.domain.entity.DailyWorkReportDO;
import com.company.labor.module.project.domain.entity.MachineryWorkRecordDO;
import com.company.labor.module.project.domain.entity.MaterialRecordDO;
import com.company.labor.module.project.domain.entity.ProjectCostRecordDO;
import com.company.labor.module.project.domain.entity.ProjectDO;
import com.company.labor.module.project.domain.entity.ProjectPaymentRecordDO;
import com.company.labor.module.project.domain.entity.TeamSettlementDO;
import com.company.labor.module.project.domain.entity.VariationOrderDO;
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

class ProjectProfitDashboardTest {
    @Test
    void dashboardSummarizesProfitCostSourcesAndCashFlow() {
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
        when(dailyReportMapper.selectList(any())).thenReturn(List.of(dailyReport("100", "10000.00")));
        when(settlementMapper.selectList(any())).thenReturn(List.of(
                settlement("7000.00", "paid"),
                settlement("1000.00", "unpaid")));
        when(costRecordMapper.selectList(any())).thenReturn(List.of(
                cost("员工工资分摊", "1000.00"),
                cost("报销费用", "500.00")));
        when(paymentRecordMapper.selectList(any())).thenReturn(List.of(payment("received", "5000.00")));
        when(machineryWorkRecordMapper.selectList(any())).thenReturn(List.of(machinery("800.00")));
        when(materialRecordMapper.selectList(any())).thenReturn(List.of(material("700.00")));
        when(variationOrderMapper.selectList(any())).thenReturn(List.of(variation("confirmed", "2000.00")), List.of());
        when(dailyReportMapper.selectCount(any())).thenReturn(0L);
        when(variationOrderMapper.selectCount(any())).thenReturn(0L);
        when(sectionMapper.selectList(any())).thenReturn(List.of());

        ProjectDashboardVO dashboard = service.dashboard(1L);

        assertThat(dashboard.getRevenueAmount()).isEqualByComparingTo("12000.00");
        assertThat(dashboard.getTeamPayableAmount()).isEqualByComparingTo("8000.00");
        assertThat(dashboard.getTeamPaidAmount()).isEqualByComparingTo("7000.00");
        assertThat(dashboard.getSalaryAllocatedCostAmount()).isEqualByComparingTo("1000.00");
        assertThat(dashboard.getOtherCostAmount()).isEqualByComparingTo("500.00");
        assertThat(dashboard.getTotalCostAmount()).isEqualByComparingTo("11000.00");
        assertThat(dashboard.getGrossProfit()).isEqualByComparingTo("1000.00");
        assertThat(dashboard.getCostRate()).isEqualByComparingTo("91.67");
        assertThat(dashboard.getProfitRate()).isEqualByComparingTo("8.33");
        assertThat(dashboard.getCashInAmount()).isEqualByComparingTo("5000.00");
        assertThat(dashboard.getCashOutAmount()).isEqualByComparingTo("10000.00");
        assertThat(dashboard.getCashBalanceAmount()).isEqualByComparingTo("-5000.00");
        assertThat(dashboard.getRevenueBreakdowns()).extracting("category")
                .containsExactly("工程量产值", "签证变更收入");
        assertThat(dashboard.getCostBreakdowns()).extracting("category")
                .containsExactly("班组结算", "工资分摊", "材料辅材", "机械台班", "其他成本");
        assertThat(dashboard.getRisks()).extracting("type")
                .contains("cost_rate_high", "cashflow_negative");
    }

    private ProjectDO project() {
        ProjectDO project = new ProjectDO();
        project.setId(1L);
        project.setName("项目A");
        project.setContractAmount(new BigDecimal("12000.00"));
        project.setDeleted(0);
        return project;
    }

    private DailyWorkReportDO dailyReport(String quantity, String outputValue) {
        DailyWorkReportDO report = new DailyWorkReportDO();
        report.setProjectId(1L);
        report.setCompletedQuantity(new BigDecimal(quantity));
        report.setOutputValue(new BigDecimal(outputValue));
        report.setPricingBasis("合同清单快照");
        report.setStatus("confirmed");
        report.setDeleted(0);
        return report;
    }

    private TeamSettlementDO settlement(String actualPayAmount, String paymentStatus) {
        TeamSettlementDO settlement = new TeamSettlementDO();
        settlement.setProjectId(1L);
        settlement.setActualPayAmount(new BigDecimal(actualPayAmount));
        settlement.setPaymentStatus(paymentStatus);
        settlement.setDeleted(0);
        return settlement;
    }

    private ProjectCostRecordDO cost(String costType, String amount) {
        ProjectCostRecordDO cost = new ProjectCostRecordDO();
        cost.setProjectId(1L);
        cost.setCostType(costType);
        cost.setAmount(new BigDecimal(amount));
        cost.setDeleted(0);
        return cost;
    }

    private ProjectPaymentRecordDO payment(String type, String amount) {
        ProjectPaymentRecordDO record = new ProjectPaymentRecordDO();
        record.setProjectId(1L);
        record.setRecordType(type);
        record.setAmount(new BigDecimal(amount));
        record.setDeleted(0);
        return record;
    }

    private MachineryWorkRecordDO machinery(String amount) {
        MachineryWorkRecordDO record = new MachineryWorkRecordDO();
        record.setProjectId(1L);
        record.setAmount(new BigDecimal(amount));
        record.setDeleted(0);
        return record;
    }

    private MaterialRecordDO material(String amount) {
        MaterialRecordDO record = new MaterialRecordDO();
        record.setProjectId(1L);
        record.setAmount(new BigDecimal(amount));
        record.setDeleted(0);
        return record;
    }

    private VariationOrderDO variation(String status, String amount) {
        VariationOrderDO order = new VariationOrderDO();
        order.setProjectId(1L);
        order.setStatus(status);
        order.setConfirmedAmount(new BigDecimal(amount));
        order.setDeleted(0);
        return order;
    }
}

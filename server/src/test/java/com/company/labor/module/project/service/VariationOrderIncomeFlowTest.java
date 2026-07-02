package com.company.labor.module.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.labor.common.softdelete.SoftDeleteGuard;
import com.company.labor.module.file.service.FileBindingGuard;
import com.company.labor.module.project.domain.dto.ProjectActionDTO;
import com.company.labor.module.project.domain.entity.ProjectDO;
import com.company.labor.module.project.domain.entity.VariationOrderConfirmDO;
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
import com.company.labor.module.system.domain.entity.SysUserDO;
import com.company.labor.module.system.service.OperationLogService;
import com.company.labor.security.AuditUserProvider;
import com.company.labor.security.LoginUser;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class VariationOrderIncomeFlowTest {
    private ProjectMapper projectMapper;
    private ProjectSectionMapper sectionMapper;
    private DailyWorkReportMapper dailyReportMapper;
    private TeamSettlementMapper settlementMapper;
    private ProjectCostRecordMapper costRecordMapper;
    private ProjectPaymentRecordMapper paymentRecordMapper;
    private MachineryWorkRecordMapper machineryWorkRecordMapper;
    private MaterialRecordMapper materialRecordMapper;
    private VariationOrderMapper variationOrderMapper;
    private VariationOrderConfirmMapper variationConfirmMapper;
    private AuditUserProvider auditUserProvider;
    private ProjectManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        projectMapper = mock(ProjectMapper.class);
        sectionMapper = mock(ProjectSectionMapper.class);
        dailyReportMapper = mock(DailyWorkReportMapper.class);
        settlementMapper = mock(TeamSettlementMapper.class);
        costRecordMapper = mock(ProjectCostRecordMapper.class);
        paymentRecordMapper = mock(ProjectPaymentRecordMapper.class);
        machineryWorkRecordMapper = mock(MachineryWorkRecordMapper.class);
        materialRecordMapper = mock(MaterialRecordMapper.class);
        variationOrderMapper = mock(VariationOrderMapper.class);
        variationConfirmMapper = mock(VariationOrderConfirmMapper.class);
        auditUserProvider = mock(AuditUserProvider.class);
        service = new ProjectManagementServiceImpl(
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
                variationConfirmMapper,
                mock(VariationOrderFileMapper.class),
                costRecordMapper,
                paymentRecordMapper,
                mock(MachineryEquipmentMapper.class),
                machineryWorkRecordMapper,
                materialRecordMapper,
                mock(ProjectDocumentArchiveMapper.class),
                mock(OperationLogService.class),
                auditUserProvider,
                mock(SoftDeleteGuard.class),
                mock(FileBindingGuard.class));
    }

    @Test
    void confirmVariationStoresConfirmedAmountAndOperator() {
        when(variationOrderMapper.selectById(8L)).thenReturn(submittedVariation());
        when(auditUserProvider.currentLoginUser()).thenReturn(loginUser());
        ProjectActionDTO dto = new ProjectActionDTO();
        dto.setConfirmedAmount(new BigDecimal("1234.50"));
        dto.setRemark("商务确认");

        service.confirmVariationOrder(8L, dto);

        ArgumentCaptor<VariationOrderDO> orderCaptor = ArgumentCaptor.forClass(VariationOrderDO.class);
        verify(variationOrderMapper).updateById(orderCaptor.capture());
        VariationOrderDO saved = orderCaptor.getValue();
        assertThat(saved.getStatus()).isEqualTo("confirmed");
        assertThat(saved.getConfirmedAmount()).isEqualByComparingTo("1234.50");
        assertThat(saved.getConfirmerId()).isEqualTo(100L);
        assertThat(saved.getConfirmerName()).isEqualTo("commercial");
        assertThat(saved.getConfirmedAt()).isNotNull();
        assertThat(saved.getConfirmRemark()).isEqualTo("商务确认");

        ArgumentCaptor<VariationOrderConfirmDO> confirmCaptor = ArgumentCaptor.forClass(VariationOrderConfirmDO.class);
        verify(variationConfirmMapper).insert(confirmCaptor.capture());
        assertThat(confirmCaptor.getValue().getStatusAfter()).isEqualTo("confirmed");
    }

    @Test
    void dashboardIncludesConfirmedVariationIncome() {
        when(projectMapper.selectById(1L)).thenReturn(project());
        when(dailyReportMapper.selectList(any())).thenReturn(List.of());
        when(settlementMapper.selectList(any())).thenReturn(List.of());
        when(costRecordMapper.selectList(any())).thenReturn(List.of());
        when(paymentRecordMapper.selectList(any())).thenReturn(List.of());
        when(machineryWorkRecordMapper.selectList(any())).thenReturn(List.of());
        when(materialRecordMapper.selectList(any())).thenReturn(List.of());
        when(variationOrderMapper.selectList(any())).thenReturn(List.of(confirmedVariation()), List.of());
        when(dailyReportMapper.selectCount(any())).thenReturn(0L);
        when(variationOrderMapper.selectCount(any())).thenReturn(0L);
        when(sectionMapper.selectList(any())).thenReturn(List.of());

        ProjectDashboardVO dashboard = service.dashboard(1L);

        assertThat(dashboard.getConfirmedVariationAmount()).isEqualByComparingTo("300.00");
        assertThat(dashboard.getReceivableAmount()).isEqualByComparingTo("10300.00");
        assertThat(dashboard.getGrossProfit()).isEqualByComparingTo("300.00");
    }

    private ProjectDO project() {
        ProjectDO project = new ProjectDO();
        project.setId(1L);
        project.setName("项目A");
        project.setContractAmount(new BigDecimal("10000.00"));
        project.setDeleted(0);
        return project;
    }

    private VariationOrderDO submittedVariation() {
        VariationOrderDO order = new VariationOrderDO();
        order.setId(8L);
        order.setProjectId(1L);
        order.setStatus("submitted");
        order.setEstimatedAmount(new BigDecimal("1000.00"));
        order.setDeleted(0);
        return order;
    }

    private VariationOrderDO confirmedVariation() {
        VariationOrderDO order = new VariationOrderDO();
        order.setId(9L);
        order.setProjectId(1L);
        order.setStatus("confirmed");
        order.setConfirmedAmount(new BigDecimal("300.00"));
        order.setDeleted(0);
        return order;
    }

    private LoginUser loginUser() {
        SysUserDO user = new SysUserDO();
        user.setId(100L);
        user.setUsername("commercial");
        user.setStatus("active");
        return new LoginUser(user, List.of("MANAGER"));
    }
}

package com.company.labor.module.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.softdelete.SoftDeleteGuard;
import com.company.labor.module.file.service.FileBindingGuard;
import com.company.labor.module.project.domain.dto.GenerateTeamSettlementDTO;
import com.company.labor.module.project.domain.dto.ProjectActionDTO;
import com.company.labor.module.project.domain.entity.ProjectDO;
import com.company.labor.module.project.domain.entity.TeamDO;
import com.company.labor.module.project.domain.entity.TeamSettlementDO;
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
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

class TeamSettlementReviewFlowTest {
    private ProjectMapper projectMapper;
    private TeamMapper teamMapper;
    private TeamSettlementMapper settlementMapper;
    private AuditUserProvider auditUserProvider;
    private ProjectManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        projectMapper = mock(ProjectMapper.class);
        teamMapper = mock(TeamMapper.class);
        settlementMapper = mock(TeamSettlementMapper.class);
        auditUserProvider = mock(AuditUserProvider.class);
        service = new ProjectManagementServiceImpl(
                projectMapper,
                mock(ProjectSectionMapper.class),
                mock(WorkProcessMapper.class),
                teamMapper,
                mock(TeamUnitPriceMapper.class),
                mock(ProjectProcessPriceMapper.class),
                mock(ProjectContractItemMapper.class),
                mock(DailyWorkReportMapper.class),
                mock(DailyWorkReportFileMapper.class),
                mock(WorkQuantityConfirmMapper.class),
                settlementMapper,
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
                auditUserProvider,
                mock(SoftDeleteGuard.class),
                mock(FileBindingGuard.class));
    }

    @Test
    void rejectsDraftTeamSettlementWithAuditorAndReason() {
        when(settlementMapper.selectById(10L)).thenReturn(settlement("draft", "unpaid"));
        when(auditUserProvider.currentLoginUser()).thenReturn(loginUser());
        ProjectActionDTO dto = action("工程量有误");

        service.rejectTeamSettlement(10L, dto);

        ArgumentCaptor<TeamSettlementDO> captor = ArgumentCaptor.forClass(TeamSettlementDO.class);
        verify(settlementMapper).updateById(captor.capture());
        TeamSettlementDO saved = captor.getValue();
        assertThat(saved.getSettlementStatus()).isEqualTo("rejected");
        assertThat(saved.getRejectedReason()).isEqualTo("工程量有误");
        assertThat(saved.getAuditorId()).isEqualTo(100L);
        assertThat(saved.getAuditorName()).isEqualTo("manager");
        assertThat(saved.getAuditedAt()).isNotNull();
    }

    @Test
    void marksConfirmedTeamSettlementPaidWithOperatorRemark() {
        when(settlementMapper.selectById(10L)).thenReturn(settlement("confirmed", "unpaid"));
        when(auditUserProvider.currentLoginUser()).thenReturn(loginUser());

        service.markTeamSettlementPaid(10L, action("银行转账"));

        ArgumentCaptor<TeamSettlementDO> captor = ArgumentCaptor.forClass(TeamSettlementDO.class);
        verify(settlementMapper).updateById(captor.capture());
        TeamSettlementDO saved = captor.getValue();
        assertThat(saved.getPaymentStatus()).isEqualTo("paid");
        assertThat(saved.getPaidDate()).isNotNull();
        assertThat(saved.getPaidBy()).isEqualTo(100L);
        assertThat(saved.getPaidOperatorName()).isEqualTo("manager");
        assertThat(saved.getPaymentRemark()).isEqualTo("银行转账");
    }

    @Test
    void rejectsDuplicateTeamSettlementForSameProjectTeamMonth() {
        when(projectMapper.selectById(1L)).thenReturn(project());
        when(teamMapper.selectById(2L)).thenReturn(team());
        when(settlementMapper.selectCount(any())).thenReturn(1L);

        GenerateTeamSettlementDTO dto = new GenerateTeamSettlementDTO();
        dto.setProjectId(1L);
        dto.setTeamId(2L);
        dto.setSettlementMonth("2026-07");

        assertThatThrownBy(() -> service.generateTeamSettlement(dto))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("本月结算已生成");
    }

    private TeamSettlementDO settlement(String settlementStatus, String paymentStatus) {
        TeamSettlementDO settlement = new TeamSettlementDO();
        settlement.setId(10L);
        settlement.setSettlementStatus(settlementStatus);
        settlement.setPaymentStatus(paymentStatus);
        settlement.setDeleted(0);
        return settlement;
    }

    private ProjectActionDTO action(String remark) {
        ProjectActionDTO dto = new ProjectActionDTO();
        dto.setRemark(remark);
        return dto;
    }

    private ProjectDO project() {
        ProjectDO project = new ProjectDO();
        project.setId(1L);
        project.setDeleted(0);
        return project;
    }

    private TeamDO team() {
        TeamDO team = new TeamDO();
        team.setId(2L);
        team.setDeleted(0);
        return team;
    }

    private LoginUser loginUser() {
        SysUserDO user = new SysUserDO();
        user.setId(100L);
        user.setUsername("manager");
        user.setStatus("active");
        return new LoginUser(user, List.of("MANAGER"));
    }
}

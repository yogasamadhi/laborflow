package com.company.labor.module.project.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.labor.module.project.domain.dto.ProjectActionDTO;
import com.company.labor.module.project.domain.entity.VariationOrderConfirmDO;
import com.company.labor.module.project.domain.entity.WorkQuantityConfirmDO;
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
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;

class ProjectApprovalEvidenceTest {
    @Test
    void dailyAndVariationApprovalRecordsCaptureCurrentOperator() {
        WorkQuantityConfirmMapper workQuantityConfirmMapper = mock(WorkQuantityConfirmMapper.class);
        VariationOrderConfirmMapper variationOrderConfirmMapper = mock(VariationOrderConfirmMapper.class);
        AuditUserProvider auditUserProvider = mock(AuditUserProvider.class);
        when(auditUserProvider.currentLoginUser()).thenReturn(loginUser());
        ProjectManagementServiceImpl service = newService(workQuantityConfirmMapper, variationOrderConfirmMapper, auditUserProvider);
        ProjectActionDTO dto = new ProjectActionDTO();
        dto.setRemark("同意");
        dto.setOperatorId(999L);
        dto.setOperatorName("client");

        ReflectionTestUtils.invokeMethod(service, "confirmLog", 1L, "project_manager", "confirmed", "submitted", "pm_confirmed", dto);
        ReflectionTestUtils.invokeMethod(service, "variationConfirmLog", 2L, "commercial", "confirmed", "submitted", "confirmed", dto);

        ArgumentCaptor<WorkQuantityConfirmDO> dailyCaptor = ArgumentCaptor.forClass(WorkQuantityConfirmDO.class);
        verify(workQuantityConfirmMapper).insert(dailyCaptor.capture());
        assertThat(dailyCaptor.getValue().getConfirmerId()).isEqualTo(100L);
        assertThat(dailyCaptor.getValue().getConfirmerName()).isEqualTo("admin");
        assertThat(dailyCaptor.getValue().getStatusBefore()).isEqualTo("submitted");
        assertThat(dailyCaptor.getValue().getStatusAfter()).isEqualTo("pm_confirmed");

        ArgumentCaptor<VariationOrderConfirmDO> variationCaptor = ArgumentCaptor.forClass(VariationOrderConfirmDO.class);
        verify(variationOrderConfirmMapper).insert(variationCaptor.capture());
        assertThat(variationCaptor.getValue().getConfirmerId()).isEqualTo(100L);
        assertThat(variationCaptor.getValue().getConfirmerName()).isEqualTo("admin");
        assertThat(variationCaptor.getValue().getStatusBefore()).isEqualTo("submitted");
        assertThat(variationCaptor.getValue().getStatusAfter()).isEqualTo("confirmed");
    }

    private ProjectManagementServiceImpl newService(WorkQuantityConfirmMapper workQuantityConfirmMapper,
                                                    VariationOrderConfirmMapper variationOrderConfirmMapper,
                                                    AuditUserProvider auditUserProvider) {
        return new ProjectManagementServiceImpl(
                mock(ProjectMapper.class),
                mock(ProjectSectionMapper.class),
                mock(WorkProcessMapper.class),
                mock(TeamMapper.class),
                mock(TeamUnitPriceMapper.class),
                mock(ProjectProcessPriceMapper.class),
                mock(DailyWorkReportMapper.class),
                mock(DailyWorkReportFileMapper.class),
                workQuantityConfirmMapper,
                mock(TeamSettlementMapper.class),
                mock(TeamSettlementDetailMapper.class),
                mock(VariationOrderMapper.class),
                variationOrderConfirmMapper,
                mock(VariationOrderFileMapper.class),
                mock(ProjectCostRecordMapper.class),
                mock(ProjectPaymentRecordMapper.class),
                mock(MachineryEquipmentMapper.class),
                mock(MachineryWorkRecordMapper.class),
                mock(MaterialRecordMapper.class),
                mock(ProjectDocumentArchiveMapper.class),
                mock(OperationLogService.class),
                auditUserProvider);
    }

    private LoginUser loginUser() {
        SysUserDO user = new SysUserDO();
        user.setId(100L);
        user.setUsername("admin");
        user.setStatus("active");
        return new LoginUser(user, List.of("ADMIN"));
    }
}

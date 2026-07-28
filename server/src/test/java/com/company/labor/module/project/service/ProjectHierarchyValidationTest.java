package com.company.labor.module.project.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.softdelete.SoftDeleteGuard;
import com.company.labor.module.file.service.FileBindingGuard;
import com.company.labor.module.project.domain.entity.MaterialRecordDO;
import com.company.labor.module.project.domain.entity.ProjectDO;
import com.company.labor.module.project.domain.entity.ProjectSectionDO;
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
import org.junit.jupiter.api.Test;

class ProjectHierarchyValidationTest {
    @Test
    void rejectsMaterialRecordWhoseSectionBelongsToAnotherProject() {
        ProjectMapper projectMapper = mock(ProjectMapper.class);
        ProjectSectionMapper sectionMapper = mock(ProjectSectionMapper.class);
        MaterialRecordMapper materialMapper = mock(MaterialRecordMapper.class);
        when(projectMapper.selectById(1L)).thenReturn(project(1L));
        when(sectionMapper.selectById(2L)).thenReturn(section(2L, 99L));
        ProjectManagementServiceImpl service = service(projectMapper, sectionMapper, materialMapper);
        MaterialRecordDO record = new MaterialRecordDO();
        record.setProjectId(1L);
        record.setProjectSectionId(2L);
        record.setMaterialName("钢筋");
        record.setRecordType("arrival");
        record.setQuantity(BigDecimal.ONE);
        record.setUnitPrice(BigDecimal.TEN);

        assertThatThrownBy(() -> service.saveMaterialRecord(record))
                .isInstanceOf(BusinessException.class)
                .hasMessage("施工段不属于所选项目");

        verify(materialMapper, never()).insert(record);
    }

    private ProjectManagementServiceImpl service(ProjectMapper projectMapper,
                                                  ProjectSectionMapper sectionMapper,
                                                  MaterialRecordMapper materialMapper) {
        return new ProjectManagementServiceImpl(
                projectMapper,
                sectionMapper,
                mock(WorkProcessMapper.class),
                mock(TeamMapper.class),
                mock(TeamUnitPriceMapper.class),
                mock(ProjectProcessPriceMapper.class),
                mock(ProjectContractItemMapper.class),
                mock(DailyWorkReportMapper.class),
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
                materialMapper,
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
}

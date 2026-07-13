package com.company.labor.module.project.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.softdelete.SoftDeleteGuard;
import com.company.labor.module.file.service.FileBindingGuard;
import com.company.labor.module.project.domain.dto.FileAttachDTO;
import com.company.labor.module.project.domain.entity.DailyWorkReportDO;
import com.company.labor.module.project.domain.entity.DailyWorkReportFileDO;
import com.company.labor.module.project.domain.entity.ProjectDO;
import com.company.labor.module.project.domain.entity.ProjectDocumentArchiveDO;
import com.company.labor.module.project.domain.entity.ProjectPaymentRecordDO;
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
import com.company.labor.module.project.service.impl.ProjectManagementServiceImpl;
import com.company.labor.module.system.service.OperationLogService;
import com.company.labor.security.AuditUserProvider;
import java.math.BigDecimal;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class ProjectAttachmentBindingValidationTest {
    private ProjectMapper projectMapper;
    private DailyWorkReportMapper dailyWorkReportMapper;
    private DailyWorkReportFileMapper dailyWorkReportFileMapper;
    private ProjectPaymentRecordMapper paymentRecordMapper;
    private ProjectDocumentArchiveMapper documentArchiveMapper;
    private FileBindingGuard fileBindingGuard;
    private ProjectManagementServiceImpl service;

    @BeforeEach
    void setUp() {
        projectMapper = mock(ProjectMapper.class);
        dailyWorkReportMapper = mock(DailyWorkReportMapper.class);
        dailyWorkReportFileMapper = mock(DailyWorkReportFileMapper.class);
        paymentRecordMapper = mock(ProjectPaymentRecordMapper.class);
        documentArchiveMapper = mock(ProjectDocumentArchiveMapper.class);
        fileBindingGuard = mock(FileBindingGuard.class);
        service = new ProjectManagementServiceImpl(
                projectMapper,
                mock(ProjectSectionMapper.class),
                mock(WorkProcessMapper.class),
                mock(TeamMapper.class),
                mock(TeamUnitPriceMapper.class),
                mock(ProjectProcessPriceMapper.class),
                mock(ProjectContractItemMapper.class),
                dailyWorkReportMapper,
                dailyWorkReportFileMapper,
                mock(WorkQuantityConfirmMapper.class),
                mock(TeamSettlementMapper.class),
                mock(TeamSettlementDetailMapper.class),
                mock(VariationOrderMapper.class),
                mock(VariationOrderConfirmMapper.class),
                mock(VariationOrderFileMapper.class),
                mock(ProjectCostRecordMapper.class),
                paymentRecordMapper,
                mock(MachineryEquipmentMapper.class),
                mock(MachineryWorkRecordMapper.class),
                mock(MaterialRecordMapper.class),
                documentArchiveMapper,
                mock(OperationLogService.class),
                mock(AuditUserProvider.class),
                mock(SoftDeleteGuard.class),
                fileBindingGuard);
    }

    @Test
    void rejectsDuplicateDailyReportFileBinding() {
        when(dailyWorkReportMapper.selectById(1L)).thenReturn(dailyReport(1L, 5L));
        when(dailyWorkReportFileMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.attachDailyReportFile(1L, fileAttach(7L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("附件已绑定该日报");

        verify(fileBindingGuard).requireProjectFileBindable(7L, 5L);
        verify(fileBindingGuard).requireFileNotBoundToOtherBusiness(7L, FileBindingGuard.DAILY_WORK_REPORT);
        verify(dailyWorkReportFileMapper, never()).insert(any(DailyWorkReportFileDO.class));
    }

    @Test
    void rejectsDuplicatePaymentVoucherFile() {
        when(projectMapper.selectById(5L)).thenReturn(project(5L));
        when(paymentRecordMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.savePaymentRecord(paymentRecord(5L, 7L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("回款凭证已被使用");

        verify(fileBindingGuard).requireProjectFileBindable(7L, 5L);
        verify(fileBindingGuard).requireFileNotBoundToOtherBusiness(7L, FileBindingGuard.PROJECT_PAYMENT);
        verify(paymentRecordMapper, never()).insert(any(ProjectPaymentRecordDO.class));
    }

    @Test
    void rejectsDuplicateDocumentArchiveFile() {
        when(projectMapper.selectById(5L)).thenReturn(project(5L));
        when(documentArchiveMapper.selectCount(any())).thenReturn(1L);

        assertThatThrownBy(() -> service.saveDocumentArchive(documentArchive(5L, 7L)))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("附件已被归档");

        verify(fileBindingGuard).requireProjectFileBindable(7L, 5L);
        verify(fileBindingGuard).requireFileNotBoundToOtherBusiness(7L, FileBindingGuard.PROJECT_DOCUMENT);
        verify(documentArchiveMapper, never()).insert(any(ProjectDocumentArchiveDO.class));
    }

    private FileAttachDTO fileAttach(Long fileId) {
        FileAttachDTO dto = new FileAttachDTO();
        dto.setFileId(fileId);
        dto.setFileType("photo");
        return dto;
    }

    private DailyWorkReportDO dailyReport(Long id, Long projectId) {
        DailyWorkReportDO report = new DailyWorkReportDO();
        report.setId(id);
        report.setProjectId(projectId);
        report.setStatus("draft");
        report.setDeleted(0);
        return report;
    }

    private ProjectDO project(Long id) {
        ProjectDO project = new ProjectDO();
        project.setId(id);
        project.setDeleted(0);
        return project;
    }

    private ProjectPaymentRecordDO paymentRecord(Long projectId, Long fileId) {
        ProjectPaymentRecordDO record = new ProjectPaymentRecordDO();
        record.setProjectId(projectId);
        record.setVoucherFileId(fileId);
        record.setAmount(BigDecimal.TEN);
        return record;
    }

    private ProjectDocumentArchiveDO documentArchive(Long projectId, Long fileId) {
        ProjectDocumentArchiveDO archive = new ProjectDocumentArchiveDO();
        archive.setProjectId(projectId);
        archive.setFileId(fileId);
        archive.setDocumentType("验收资料");
        return archive;
    }
}

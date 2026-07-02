package com.company.labor.module.salary.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.company.labor.module.compensation.domain.entity.SalarySourceRecordDO;
import com.company.labor.module.salary.domain.dto.BatchSalaryPaymentDTO;
import com.company.labor.module.salary.domain.dto.SalaryPaymentDTO;
import com.company.labor.module.salary.domain.entity.SalaryAuditRecordDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDetailDO;
import java.util.List;

public interface SalaryMonthlyService extends IService<SalaryMonthlyDO> {
    SalaryMonthlyDO createMonthly(SalaryMonthlyDO monthly);
    SalaryMonthlyDO updateMonthly(Long monthlyId, SalaryMonthlyDO monthly);
    void generate(Long monthlyId);
    void submit(Long monthlyId);
    void approve(Long monthlyId, Long userId, String remark);
    void reject(Long monthlyId, String remark);
    void markMonthlyPaid(Long monthlyId, SalaryPaymentDTO dto);
    void deleteMonthly(Long monthlyId);
    void restoreMonthly(Long monthlyId);
    Page<SalaryMonthlyDetailDO> details(Long monthlyId, long page, long pageSize);
    SalaryMonthlyDetailDO updateDetail(Long detailId, SalaryMonthlyDetailDO detail);
    void markDetailPaid(Long detailId, SalaryPaymentDTO dto);
    void batchMarkPaid(BatchSalaryPaymentDTO dto);
    List<SalaryAuditRecordDO> auditRecords(Long monthlyId);
    List<SalarySourceRecordDO> sourceRecords(Long monthlyId, Long detailId);
}

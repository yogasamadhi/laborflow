package com.company.labor.module.salary.service;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.IService;
import com.company.labor.module.salary.domain.dto.BatchSalaryPaymentDTO;
import com.company.labor.module.salary.domain.dto.SalaryPaymentDTO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDetailDO;

public interface SalaryMonthlyService extends IService<SalaryMonthlyDO> {
    SalaryMonthlyDO createMonthly(SalaryMonthlyDO monthly);
    void generate(Long monthlyId);
    void submit(Long monthlyId);
    void approve(Long monthlyId, Long userId);
    void reject(Long monthlyId);
    void markMonthlyPaid(Long monthlyId, SalaryPaymentDTO dto);
    Page<SalaryMonthlyDetailDO> details(Long monthlyId, long page, long pageSize);
    SalaryMonthlyDetailDO updateDetail(Long detailId, SalaryMonthlyDetailDO detail);
    void markDetailPaid(Long detailId, SalaryPaymentDTO dto);
    void batchMarkPaid(BatchSalaryPaymentDTO dto);
}


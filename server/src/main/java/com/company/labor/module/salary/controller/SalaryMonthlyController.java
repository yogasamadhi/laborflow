package com.company.labor.module.salary.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.salary.domain.dto.BatchSalaryPaymentDTO;
import com.company.labor.module.salary.domain.dto.SalaryPaymentDTO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDetailDO;
import com.company.labor.module.salary.service.SalaryMonthlyService;
import com.company.labor.security.LoginUser;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/salary")
@RequiredArgsConstructor
public class SalaryMonthlyController {
    private final SalaryMonthlyService service;

    @GetMapping("/monthly")
    public Result<PageResult<SalaryMonthlyDO>> list(@RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(PageResult.of(service.page(new Page<>(page, pageSize))));
    }

    @PostMapping("/monthly")
    public Result<SalaryMonthlyDO> create(@RequestBody SalaryMonthlyDO monthly) {
        return Result.success(service.createMonthly(monthly));
    }

    @GetMapping("/monthly/{id}")
    public Result<SalaryMonthlyDO> detail(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @PutMapping("/monthly/{id}")
    public Result<SalaryMonthlyDO> update(@PathVariable Long id, @RequestBody SalaryMonthlyDO monthly) {
        monthly.setId(id);
        service.updateById(monthly);
        return Result.success(service.getById(id));
    }

    @PostMapping("/monthly/{id}/generate")
    public Result<Void> generate(@PathVariable Long id) {
        service.generate(id);
        return Result.success();
    }

    @PostMapping("/monthly/{id}/submit")
    public Result<Void> submit(@PathVariable Long id) {
        service.submit(id);
        return Result.success();
    }

    @PostMapping("/monthly/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','BOSS')")
    public Result<Void> approve(@PathVariable Long id, Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        service.approve(id, loginUser.getUser().getId());
        return Result.success();
    }

    @PostMapping("/monthly/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','BOSS')")
    public Result<Void> reject(@PathVariable Long id) {
        service.reject(id);
        return Result.success();
    }

    @PostMapping("/monthly/{id}/mark-paid")
    public Result<Void> markMonthlyPaid(@PathVariable Long id, @RequestBody SalaryPaymentDTO dto) {
        service.markMonthlyPaid(id, dto);
        return Result.success();
    }

    @GetMapping("/monthly/{monthlyId}/details")
    public Result<PageResult<SalaryMonthlyDetailDO>> details(@PathVariable Long monthlyId, @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(PageResult.of(service.details(monthlyId, page, pageSize)));
    }

    @PutMapping("/details/{detailId}")
    public Result<SalaryMonthlyDetailDO> updateDetail(@PathVariable Long detailId, @RequestBody SalaryMonthlyDetailDO detail) {
        return Result.success(service.updateDetail(detailId, detail));
    }

    @PostMapping("/details/{detailId}/mark-paid")
    public Result<Void> markDetailPaid(@PathVariable Long detailId, @RequestBody SalaryPaymentDTO dto) {
        service.markDetailPaid(detailId, dto);
        return Result.success();
    }

    @PostMapping("/details/batch-mark-paid")
    public Result<Void> batchMarkPaid(@RequestBody BatchSalaryPaymentDTO dto) {
        service.batchMarkPaid(dto);
        return Result.success();
    }
}


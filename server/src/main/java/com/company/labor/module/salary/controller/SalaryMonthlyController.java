package com.company.labor.module.salary.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.salary.domain.entity.SalaryDetailItemDO;
import com.company.labor.module.salary.domain.dto.BatchSalaryPaymentDTO;
import com.company.labor.module.salary.domain.dto.SalaryPaymentDTO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDetailDO;
import com.company.labor.module.salary.mapper.SalaryDetailItemMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyMapper;
import com.company.labor.module.salary.service.SalaryExportService;
import com.company.labor.module.salary.service.SalaryMonthlyService;
import com.company.labor.security.LoginUser;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
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
    private final SalaryExportService exportService;
    private final SalaryDetailItemMapper detailItemMapper;
    private final SalaryMonthlyMapper monthlyMapper;

    @GetMapping("/monthly")
    public Result<PageResult<SalaryMonthlyDO>> list(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false, defaultValue = "0") Integer deleted) {
        int deletedFilter = deleted == null ? 0 : deleted;
        long currentPage = Math.max(page, 1);
        long size = Math.max(pageSize, 1);
        long offset = (currentPage - 1) * size;
        return Result.success(new PageResult<>(
                monthlyMapper.selectPageIncludingDeleted(deletedFilter, size, offset),
                monthlyMapper.countIncludingDeleted(deletedFilter),
                currentPage,
                size));
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

    @PostMapping("/monthly/{id}/delete")
    public Result<Void> deleteMonthly(@PathVariable Long id) {
        requireMonthly(id);
        monthlyMapper.softDeleteById(id);
        return Result.success();
    }

    @PostMapping("/monthly/{id}/restore")
    public Result<Void> restoreMonthly(@PathVariable Long id) {
        SalaryMonthlyDO monthly = requireMonthly(id);
        long activeCount = monthlyMapper.countActiveByMonthExceptId(monthly.getSalaryMonth(), id);
        if (activeCount > 0) {
            throw new BusinessException("同月份已存在正常工资表，不能恢复");
        }
        monthlyMapper.restoreById(id);
        return Result.success();
    }

    @PostMapping("/monthly/{id}/generate")
    public Result<SalaryMonthlyDO> generate(@PathVariable Long id) {
        service.generate(id);
        return Result.success(service.getById(id));
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

    @GetMapping("/details/{detailId}/items")
    public Result<List<SalaryDetailItemDO>> detailItems(@PathVariable Long detailId) {
        return Result.success(detailItemMapper.selectList(new LambdaQueryWrapper<SalaryDetailItemDO>()
                .eq(SalaryDetailItemDO::getSalaryDetailId, detailId)));
    }

    @GetMapping("/monthly/{monthlyId}/detail-items")
    public Result<List<SalaryDetailItemDO>> monthlyDetailItems(@PathVariable Long monthlyId) {
        return Result.success(detailItemMapper.selectByMonthlyId(monthlyId));
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

    @GetMapping("/monthly/{id}/export")
    public void exportMonthly(@PathVariable Long id, HttpServletResponse response) throws IOException {
        prepareExcelResponse(response, "月度工资表-" + id + ".xlsx");
        exportService.exportMonthly(id, response.getOutputStream());
    }

    @GetMapping("/monthly/{id}/export/department-summary")
    public void exportDepartmentSummary(@PathVariable Long id, HttpServletResponse response) throws IOException {
        prepareExcelResponse(response, "部门工资汇总-" + id + ".xlsx");
        exportService.exportDepartmentSummary(id, response.getOutputStream());
    }

    @GetMapping("/monthly/{id}/export/payment-records")
    public void exportPaymentRecords(@PathVariable Long id, HttpServletResponse response) throws IOException {
        prepareExcelResponse(response, "工资发放记录-" + id + ".xlsx");
        exportService.exportPaymentRecords(id, response.getOutputStream());
    }

    @GetMapping("/monthly/{id}/payslip")
    public void exportPayslip(@PathVariable Long id, @RequestParam(required = false) Long employeeId, HttpServletResponse response) throws IOException {
        prepareExcelResponse(response, "工资条-" + id + ".xlsx");
        exportService.exportPayslip(id, employeeId, response.getOutputStream());
    }

    @GetMapping("/employees/{employeeId}/details/export")
    public void exportEmployeeDetails(@PathVariable Long employeeId, HttpServletResponse response) throws IOException {
        prepareExcelResponse(response, "员工工资明细-" + employeeId + ".xlsx");
        exportService.exportEmployeeDetails(employeeId, response.getOutputStream());
    }

    private void prepareExcelResponse(HttpServletResponse response, String filename) {
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
    }

    private SalaryMonthlyDO requireMonthly(Long id) {
        SalaryMonthlyDO monthly = monthlyMapper.selectByIdIncludingDeleted(id);
        if (monthly == null) {
            throw new BusinessException("工资表不存在");
        }
        return monthly;
    }
}

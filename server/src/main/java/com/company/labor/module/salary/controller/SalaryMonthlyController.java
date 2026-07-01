package com.company.labor.module.salary.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.salary.domain.dto.SalaryApprovalDTO;
import com.company.labor.module.salary.domain.dto.BatchSalaryPaymentDTO;
import com.company.labor.module.salary.domain.dto.SalaryPaymentDTO;
import com.company.labor.module.salary.domain.dto.SalaryWriteDTO;
import com.company.labor.module.salary.domain.entity.SalaryAuditRecordDO;
import com.company.labor.module.salary.domain.entity.SalaryDetailItemDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDetailDO;
import com.company.labor.module.salary.mapper.SalaryDetailItemMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyDetailMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyMapper;
import com.company.labor.module.salary.service.SalaryExportService;
import com.company.labor.module.salary.service.SalaryMonthlyService;
import com.company.labor.security.AccessControlSupport;
import com.company.labor.security.LoginUser;
import com.company.labor.security.RoleExpressions;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.validation.Valid;
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

import static com.company.labor.common.util.RequestCopyUtils.copy;

@RestController
@RequestMapping("/api/salary")
@RequiredArgsConstructor
public class SalaryMonthlyController {
    private final SalaryMonthlyService service;
    private final SalaryExportService exportService;
    private final SalaryDetailItemMapper detailItemMapper;
    private final SalaryMonthlyDetailMapper detailMapper;
    private final SalaryMonthlyMapper monthlyMapper;

    @GetMapping("/monthly")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
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
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<SalaryMonthlyDO> create(@Valid @RequestBody SalaryWriteDTO.SalaryMonthlyRequest dto) {
        return Result.success(service.createMonthly(copy(dto, SalaryMonthlyDO.class)));
    }

    @GetMapping("/monthly/{id}")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<SalaryMonthlyDO> detail(@PathVariable Long id) {
        return Result.success(service.getById(id));
    }

    @PutMapping("/monthly/{id}")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<SalaryMonthlyDO> update(@PathVariable Long id, @Valid @RequestBody SalaryWriteDTO.SalaryMonthlyRequest dto) {
        return Result.success(service.updateMonthly(id, copy(dto, SalaryMonthlyDO.class)));
    }

    @PostMapping("/monthly/{id}/delete")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<Void> deleteMonthly(@PathVariable Long id) {
        service.deleteMonthly(id);
        return Result.success();
    }

    @PostMapping("/monthly/{id}/restore")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<Void> restoreMonthly(@PathVariable Long id) {
        service.restoreMonthly(id);
        return Result.success();
    }

    @PostMapping("/monthly/{id}/generate")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<SalaryMonthlyDO> generate(@PathVariable Long id) {
        service.generate(id);
        return Result.success(service.getById(id));
    }

    @PostMapping("/monthly/{id}/submit")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<Void> submit(@PathVariable Long id) {
        service.submit(id);
        return Result.success();
    }

    @PostMapping("/monthly/{id}/approve")
    @PreAuthorize("hasAnyRole('ADMIN','BOSS')")
    public Result<Void> approve(@PathVariable Long id, @Valid @RequestBody(required = false) SalaryApprovalDTO dto, Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        service.approve(id, loginUser.getUser().getId(), dto == null ? null : dto.getRemark());
        return Result.success();
    }

    @PostMapping("/monthly/{id}/reject")
    @PreAuthorize("hasAnyRole('ADMIN','BOSS')")
    public Result<Void> reject(@PathVariable Long id, @Valid @RequestBody(required = false) SalaryApprovalDTO dto) {
        service.reject(id, dto == null ? null : dto.getRemark());
        return Result.success();
    }

    @PostMapping("/monthly/{id}/mark-paid")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<Void> markMonthlyPaid(@PathVariable Long id, @Valid @RequestBody SalaryPaymentDTO dto) {
        service.markMonthlyPaid(id, dto);
        return Result.success();
    }

    @GetMapping("/monthly/{monthlyId}/details")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<PageResult<SalaryMonthlyDetailDO>> details(@PathVariable Long monthlyId, @RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long pageSize) {
        return Result.success(PageResult.of(service.details(monthlyId, page, pageSize)));
    }

    @PutMapping("/details/{detailId}")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<SalaryMonthlyDetailDO> updateDetail(@PathVariable Long detailId, @Valid @RequestBody SalaryWriteDTO.SalaryMonthlyDetailRequest dto) {
        return Result.success(service.updateDetail(detailId, copy(dto, SalaryMonthlyDetailDO.class)));
    }

    @GetMapping("/details/{detailId}/items")
    public Result<List<SalaryDetailItemDO>> detailItems(@PathVariable Long detailId, Authentication authentication) {
        requireSalaryDetailAccess(detailId, authentication);
        return Result.success(detailItemMapper.selectList(new LambdaQueryWrapper<SalaryDetailItemDO>()
                .eq(SalaryDetailItemDO::getSalaryDetailId, detailId)));
    }

    @GetMapping("/monthly/{monthlyId}/detail-items")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<List<SalaryDetailItemDO>> monthlyDetailItems(@PathVariable Long monthlyId) {
        return Result.success(detailItemMapper.selectByMonthlyId(monthlyId));
    }

    @GetMapping("/monthly/{monthlyId}/audits")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<List<SalaryAuditRecordDO>> auditRecords(@PathVariable Long monthlyId) {
        return Result.success(service.auditRecords(monthlyId));
    }

    @PostMapping("/details/{detailId}/mark-paid")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<Void> markDetailPaid(@PathVariable Long detailId, @Valid @RequestBody SalaryPaymentDTO dto) {
        service.markDetailPaid(detailId, dto);
        return Result.success();
    }

    @PostMapping("/details/batch-mark-paid")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public Result<Void> batchMarkPaid(@Valid @RequestBody BatchSalaryPaymentDTO dto) {
        service.batchMarkPaid(dto);
        return Result.success();
    }

    @GetMapping("/monthly/{id}/export")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public void exportMonthly(@PathVariable Long id, HttpServletResponse response) throws IOException {
        prepareExcelResponse(response, "月度工资表-" + id + ".xlsx");
        exportService.exportMonthly(id, response.getOutputStream());
    }

    @GetMapping("/monthly/{id}/export/department-summary")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public void exportDepartmentSummary(@PathVariable Long id, HttpServletResponse response) throws IOException {
        prepareExcelResponse(response, "部门工资汇总-" + id + ".xlsx");
        exportService.exportDepartmentSummary(id, response.getOutputStream());
    }

    @GetMapping("/monthly/{id}/export/payment-records")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public void exportPaymentRecords(@PathVariable Long id, HttpServletResponse response) throws IOException {
        prepareExcelResponse(response, "工资发放记录-" + id + ".xlsx");
        exportService.exportPaymentRecords(id, response.getOutputStream());
    }

    @GetMapping("/monthly/{id}/payslip")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public void exportPayslip(@PathVariable Long id, @RequestParam(required = false) Long employeeId, HttpServletResponse response) throws IOException {
        prepareExcelResponse(response, "工资条-" + id + ".xlsx");
        exportService.exportPayslip(id, employeeId, response.getOutputStream());
    }

    @GetMapping("/employees/{employeeId}/details/export")
    @PreAuthorize(RoleExpressions.SALARY_MANAGE)
    public void exportEmployeeDetails(@PathVariable Long employeeId, HttpServletResponse response) throws IOException {
        prepareExcelResponse(response, "员工工资明细-" + employeeId + ".xlsx");
        exportService.exportEmployeeDetails(employeeId, response.getOutputStream());
    }

    private void prepareExcelResponse(HttpServletResponse response, String filename) {
        String encodedFilename = URLEncoder.encode(filename, StandardCharsets.UTF_8).replace("+", "%20");
        response.setContentType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + encodedFilename);
    }

    private void requireSalaryDetailAccess(Long detailId, Authentication authentication) {
        if (AccessControlSupport.hasSalaryManageRole(authentication)) {
            return;
        }
        SalaryMonthlyDetailDO detail = detailMapper.selectById(detailId);
        if (detail == null || detail.getDeleted() != null && detail.getDeleted() == 1) {
            throw new BusinessException("工资明细不存在");
        }
        Long currentEmployeeId = AccessControlSupport.currentEmployeeId(authentication);
        if (currentEmployeeId == null || !currentEmployeeId.equals(detail.getEmployeeId())) {
            throw new BusinessException("无权查看该工资明细");
        }
    }
}

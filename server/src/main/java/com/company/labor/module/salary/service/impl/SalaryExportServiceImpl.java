package com.company.labor.module.salary.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.company.labor.common.exception.BusinessException;
import com.company.labor.module.employee.domain.entity.DepartmentDO;
import com.company.labor.module.employee.domain.entity.EmployeeDO;
import com.company.labor.module.employee.domain.entity.PositionDO;
import com.company.labor.module.employee.mapper.DepartmentMapper;
import com.company.labor.module.employee.mapper.EmployeeMapper;
import com.company.labor.module.employee.mapper.PositionMapper;
import com.company.labor.module.salary.domain.entity.SalaryDetailItemDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDetailDO;
import com.company.labor.module.salary.mapper.SalaryDetailItemMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyDetailMapper;
import com.company.labor.module.salary.mapper.SalaryMonthlyMapper;
import com.company.labor.module.salary.service.SalaryExportService;
import com.company.labor.module.system.service.OperationLogService;
import java.io.IOException;
import java.io.OutputStream;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.CellStyle;
import org.apache.poi.ss.usermodel.Font;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class SalaryExportServiceImpl implements SalaryExportService {
    private final SalaryMonthlyMapper monthlyMapper;
    private final SalaryMonthlyDetailMapper detailMapper;
    private final SalaryDetailItemMapper detailItemMapper;
    private final EmployeeMapper employeeMapper;
    private final DepartmentMapper departmentMapper;
    private final PositionMapper positionMapper;
    private final OperationLogService operationLogService;

    @Override
    public void exportMonthly(Long monthlyId, OutputStream outputStream) throws IOException {
        SalaryMonthlyDO monthly = requireMonthly(monthlyId);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("月度工资表");
            writeHeaders(workbook, sheet, "工资月份", "员工姓名", "部门", "岗位", "应发金额", "扣减金额", "实发金额", "发放状态", "发放日期", "发放方式", "扣款明细", "备注");
            int rowIndex = 1;
            for (SalaryMonthlyDetailDO detail : details(monthlyId)) {
                EmployeeDO employee = employeeMapper.selectById(detail.getEmployeeId());
                writePayrollRow(sheet.createRow(rowIndex++), monthly, detail, employee);
            }
            autosize(sheet, 12);
            workbook.write(outputStream);
        }
        log("export_monthly", monthlyId, "导出月度工资表：" + monthly.getSalaryMonth());
    }

    @Override
    public void exportEmployeeDetails(Long employeeId, OutputStream outputStream) throws IOException {
        EmployeeDO employee = employeeMapper.selectById(employeeId);
        if (employee == null || employee.getDeleted() != null && employee.getDeleted() == 1) {
            throw new BusinessException("员工不存在");
        }
        List<SalaryMonthlyDetailDO> rows = detailMapper.selectList(new LambdaQueryWrapper<SalaryMonthlyDetailDO>()
                .eq(SalaryMonthlyDetailDO::getEmployeeId, employeeId)
                .eq(SalaryMonthlyDetailDO::getDeleted, 0)
                .orderByDesc(SalaryMonthlyDetailDO::getCreatedAt));
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("员工工资明细");
            writeHeaders(workbook, sheet, "工资月份", "员工姓名", "部门", "岗位", "应发金额", "扣减金额", "实发金额", "发放状态", "发放日期", "发放方式", "扣款明细", "备注");
            int rowIndex = 1;
            for (SalaryMonthlyDetailDO detail : rows) {
                SalaryMonthlyDO monthly = monthlyMapper.selectById(detail.getSalaryMonthlyId());
                writePayrollRow(sheet.createRow(rowIndex++), monthly, detail, employee);
            }
            autosize(sheet, 12);
            workbook.write(outputStream);
        }
        log("export_employee_details", employeeId, "导出员工工资明细：" + employee.getName());
    }

    @Override
    public void exportDepartmentSummary(Long monthlyId, OutputStream outputStream) throws IOException {
        SalaryMonthlyDO monthly = requireMonthly(monthlyId);
        Map<Long, DepartmentSummary> grouped = new LinkedHashMap<>();
        for (SalaryMonthlyDetailDO detail : details(monthlyId)) {
            Long key = detail.getDepartmentId() == null ? 0L : detail.getDepartmentId();
            DepartmentSummary summary = grouped.computeIfAbsent(key, DepartmentSummary::new);
            summary.add(detail);
        }
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("部门工资汇总");
            writeHeaders(workbook, sheet, "工资月份", "部门", "人数", "应发金额", "扣减金额", "实发金额");
            int rowIndex = 1;
            for (DepartmentSummary summary : grouped.values()) {
                Row row = sheet.createRow(rowIndex++);
                write(row, 0, monthly.getSalaryMonth());
                write(row, 1, departmentName(summary.departmentId));
                write(row, 2, summary.employeeCount);
                write(row, 3, summary.payableAmount);
                write(row, 4, summary.deductionAmount);
                write(row, 5, summary.actualAmount);
            }
            autosize(sheet, 6);
            workbook.write(outputStream);
        }
        log("export_department_summary", monthlyId, "导出部门工资汇总：" + monthly.getSalaryMonth());
    }

    @Override
    public void exportPaymentRecords(Long monthlyId, OutputStream outputStream) throws IOException {
        SalaryMonthlyDO monthly = requireMonthly(monthlyId);
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("工资发放记录");
            writeHeaders(workbook, sheet, "工资月份", "员工姓名", "部门", "岗位", "应发金额", "扣减金额", "实发金额", "发放状态", "发放日期", "发放方式", "扣款明细", "备注");
            int rowIndex = 1;
            for (SalaryMonthlyDetailDO detail : details(monthlyId)) {
                EmployeeDO employee = employeeMapper.selectById(detail.getEmployeeId());
                writePayrollRow(sheet.createRow(rowIndex++), monthly, detail, employee);
            }
            autosize(sheet, 12);
            workbook.write(outputStream);
        }
        log("export_payment_records", monthlyId, "导出工资发放记录：" + monthly.getSalaryMonth());
    }

    @Override
    public void exportPayslip(Long monthlyId, Long employeeId, OutputStream outputStream) throws IOException {
        SalaryMonthlyDO monthly = requireMonthly(monthlyId);
        List<SalaryMonthlyDetailDO> rows = details(monthlyId);
        if (employeeId != null) {
            rows = rows.stream().filter(detail -> employeeId.equals(detail.getEmployeeId())).toList();
        }
        try (Workbook workbook = new XSSFWorkbook()) {
            Sheet sheet = workbook.createSheet("工资条");
            writeHeaders(workbook, sheet, "员工姓名", "工资月份", "项目类型", "工资项", "金额", "应发金额", "扣减金额", "实发金额", "发放日期", "备注");
            int rowIndex = 1;
            for (SalaryMonthlyDetailDO detail : rows) {
                EmployeeDO employee = employeeMapper.selectById(detail.getEmployeeId());
                List<SalaryDetailItemDO> items = detailItemMapper.selectList(new LambdaQueryWrapper<SalaryDetailItemDO>()
                        .eq(SalaryDetailItemDO::getSalaryDetailId, detail.getId()));
                if (items.isEmpty()) {
                    writePayslipRow(sheet.createRow(rowIndex++), monthly, detail, employee, "", "", null);
                } else {
                    for (SalaryDetailItemDO item : items) {
                        writePayslipRow(sheet.createRow(rowIndex++), monthly, detail, employee, item.getSalaryItemType(), item.getSalaryItemName(), item.getAmount());
                    }
                }
            }
            autosize(sheet, 10);
            workbook.write(outputStream);
        }
        log("export_payslip", monthlyId, employeeId == null ? "导出工资条：" + monthly.getSalaryMonth() : "导出员工工资条：" + employeeId);
    }

    private SalaryMonthlyDO requireMonthly(Long monthlyId) {
        SalaryMonthlyDO monthly = monthlyMapper.selectById(monthlyId);
        if (monthly == null || monthly.getDeleted() != null && monthly.getDeleted() == 1) {
            throw new BusinessException("工资表不存在");
        }
        return monthly;
    }

    private List<SalaryMonthlyDetailDO> details(Long monthlyId) {
        return detailMapper.selectList(new LambdaQueryWrapper<SalaryMonthlyDetailDO>()
                .eq(SalaryMonthlyDetailDO::getSalaryMonthlyId, monthlyId)
                .eq(SalaryMonthlyDetailDO::getDeleted, 0)
                .orderByAsc(SalaryMonthlyDetailDO::getEmployeeId));
    }

    private void writeHeaders(Workbook workbook, Sheet sheet, String... headers) {
        Font font = workbook.createFont();
        font.setBold(true);
        CellStyle style = workbook.createCellStyle();
        style.setFont(font);
        Row row = sheet.createRow(0);
        for (int i = 0; i < headers.length; i++) {
            Cell cell = row.createCell(i);
            cell.setCellValue(headers[i]);
            cell.setCellStyle(style);
        }
    }

    private void writePayrollRow(Row row, SalaryMonthlyDO monthly, SalaryMonthlyDetailDO detail, EmployeeDO employee) {
        write(row, 0, monthly == null ? "" : monthly.getSalaryMonth());
        write(row, 1, employee == null ? detail.getEmployeeId() : employee.getName());
        write(row, 2, departmentName(detail.getDepartmentId()));
        write(row, 3, positionName(detail.getPositionId()));
        write(row, 4, detail.getPayableAmount());
        write(row, 5, detail.getDeductionAmount());
        write(row, 6, detail.getActualAmount());
        write(row, 7, detail.getPaymentStatus());
        write(row, 8, detail.getPaymentDate());
        write(row, 9, detail.getPaymentMethod());
        write(row, 10, deductionSummary(detail.getId()));
        write(row, 11, detail.getRemark());
    }

    private void writePayslipRow(Row row, SalaryMonthlyDO monthly, SalaryMonthlyDetailDO detail, EmployeeDO employee, String type, String itemName, BigDecimal amount) {
        write(row, 0, employee == null ? detail.getEmployeeId() : employee.getName());
        write(row, 1, monthly.getSalaryMonth());
        write(row, 2, type);
        write(row, 3, itemName);
        write(row, 4, amount);
        write(row, 5, detail.getPayableAmount());
        write(row, 6, detail.getDeductionAmount());
        write(row, 7, detail.getActualAmount());
        write(row, 8, detail.getPaymentDate());
        write(row, 9, detail.getRemark());
    }

    private void write(Row row, int index, Object value) {
        Cell cell = row.createCell(index);
        if (value == null) {
            cell.setCellValue("");
        } else if (value instanceof Number number) {
            cell.setCellValue(number.doubleValue());
        } else {
            cell.setCellValue(value.toString());
        }
    }

    private void autosize(Sheet sheet, int columns) {
        for (int i = 0; i < columns; i++) {
            sheet.autoSizeColumn(i);
        }
    }

    private String departmentName(Long departmentId) {
        if (departmentId == null || departmentId == 0L) return "-";
        DepartmentDO department = departmentMapper.selectById(departmentId);
        return department == null ? String.valueOf(departmentId) : department.getName();
    }

    private String positionName(Long positionId) {
        if (positionId == null) return "-";
        PositionDO position = positionMapper.selectById(positionId);
        return position == null ? String.valueOf(positionId) : position.getName();
    }

    private String deductionSummary(Long detailId) {
        List<String> items = new ArrayList<>();
        for (SalaryDetailItemDO item : detailItemMapper.selectList(new LambdaQueryWrapper<SalaryDetailItemDO>()
                .eq(SalaryDetailItemDO::getSalaryDetailId, detailId)
                .eq(SalaryDetailItemDO::getSalaryItemType, "deduction"))) {
            items.add(item.getSalaryItemName() + ":" + nvl(item.getAmount()));
        }
        return String.join("; ", items);
    }

    private void log(String action, Long businessId, String content) {
        operationLogService.record("salary", businessId, action, content);
    }

    private BigDecimal nvl(BigDecimal value) {
        return value == null ? BigDecimal.ZERO : value;
    }

    private static class DepartmentSummary {
        private final Long departmentId;
        private int employeeCount;
        private BigDecimal payableAmount = BigDecimal.ZERO;
        private BigDecimal deductionAmount = BigDecimal.ZERO;
        private BigDecimal actualAmount = BigDecimal.ZERO;

        private DepartmentSummary(Long departmentId) {
            this.departmentId = departmentId;
        }

        private void add(SalaryMonthlyDetailDO detail) {
            employeeCount++;
            payableAmount = payableAmount.add(nvl(detail.getPayableAmount()));
            deductionAmount = deductionAmount.add(nvl(detail.getDeductionAmount()));
            actualAmount = actualAmount.add(nvl(detail.getActualAmount()));
        }

        private BigDecimal nvl(BigDecimal value) {
            return value == null ? BigDecimal.ZERO : value;
        }
    }
}

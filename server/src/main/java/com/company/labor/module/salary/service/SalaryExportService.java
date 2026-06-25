package com.company.labor.module.salary.service;

import java.io.IOException;
import java.io.OutputStream;

public interface SalaryExportService {
    void exportMonthly(Long monthlyId, OutputStream outputStream) throws IOException;
    void exportEmployeeDetails(Long employeeId, OutputStream outputStream) throws IOException;
    void exportDepartmentSummary(Long monthlyId, OutputStream outputStream) throws IOException;
    void exportPaymentRecords(Long monthlyId, OutputStream outputStream) throws IOException;
    void exportPayslip(Long monthlyId, Long employeeId, OutputStream outputStream) throws IOException;
}

package com.company.labor.module.salary.service;

import com.company.labor.module.salary.domain.vo.SalaryOverviewVO;
import com.company.labor.module.salary.domain.vo.SalaryAnomalyVO;
import com.company.labor.module.salary.domain.vo.SalaryEmployeeHistoryVO;
import com.company.labor.module.salary.domain.vo.SalaryTrendVO;
import java.util.List;
import java.util.Map;

public interface StatisticsService {
    SalaryOverviewVO overview(Long departmentId);
    List<SalaryTrendVO> monthlyTrend(Long departmentId);
    List<Map<String, Object>> departmentSummary(Long departmentId);
    SalaryEmployeeHistoryVO employeeHistory(Long employeeId);
    List<SalaryAnomalyVO> anomalies(double fluctuationRatio);
    void handleAnomaly(String anomalyKey, String status, String remark);
}

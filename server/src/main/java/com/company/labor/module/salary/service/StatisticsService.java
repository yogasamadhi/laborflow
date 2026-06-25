package com.company.labor.module.salary.service;

import com.company.labor.module.salary.domain.vo.SalaryOverviewVO;
import com.company.labor.module.salary.domain.vo.SalaryAnomalyVO;
import com.company.labor.module.salary.domain.vo.SalaryEmployeeHistoryVO;
import com.company.labor.module.salary.domain.vo.SalaryTrendVO;
import java.util.List;
import java.util.Map;

public interface StatisticsService {
    SalaryOverviewVO overview();
    List<SalaryTrendVO> monthlyTrend();
    List<Map<String, Object>> departmentSummary();
    SalaryEmployeeHistoryVO employeeHistory(Long employeeId);
    List<SalaryAnomalyVO> anomalies(double fluctuationRatio);
}

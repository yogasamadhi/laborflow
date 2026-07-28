package com.company.labor.module.project.domain.vo;

import com.company.labor.module.project.domain.entity.DailyWorkReportDO;
import com.company.labor.module.project.domain.entity.WorkQuantityConfirmDO;
import java.util.List;
import lombok.Data;

@Data
public class DailyWorkReportDetailVO {
    private DailyWorkReportDO report;
    private String projectName;
    private String sectionName;
    private String workProcessName;
    private String teamName;
    private List<WorkQuantityConfirmDO> confirms;
    private List<BusinessFileVO> files;
}

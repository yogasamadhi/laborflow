package com.company.labor.module.system.domain.vo;

import com.company.labor.module.system.domain.entity.OperationLogDO;
import java.time.LocalDateTime;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class OperationLogVO {
    private Long id;
    private String module;
    private String businessType;
    private Long businessId;
    private String action;
    private Long operatorId;
    private String operatorName;
    private String result;
    private String summary;
    private LocalDateTime createdAt;

    public static OperationLogVO from(OperationLogDO row) {
        return new OperationLogVO(
                row.getId(), row.getModule(), row.getModule(), row.getBusinessId(), row.getAction(),
                row.getOperatorId(), row.getOperatorName(), "success", row.getContent(), row.getCreatedAt());
    }
}

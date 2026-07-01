package com.company.labor.module.system.service;

import com.company.labor.module.system.domain.entity.OperationLogDO;
import com.company.labor.module.system.mapper.OperationLogMapper;
import com.company.labor.security.AuditUserProvider;
import com.company.labor.security.LoginUser;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class OperationLogService {
    private final OperationLogMapper operationLogMapper;
    private final AuditUserProvider auditUserProvider;

    public void record(String module, Long businessId, String action, String content) {
        record(module, businessId, action, auditUserProvider.currentLoginUser(), content);
    }

    public void record(String module, Long businessId, String action, LoginUser operator, String content) {
        OperationLogDO log = new OperationLogDO();
        log.setModule(module);
        log.setBusinessId(businessId);
        log.setAction(action);
        if (operator != null) {
            log.setOperatorId(operator.getUser().getId());
            log.setOperatorName(operator.getUsername());
        }
        log.setContent(StringUtils.hasText(content) ? content : action);
        log.setCreatedAt(LocalDateTime.now());
        operationLogMapper.insert(log);
    }
}

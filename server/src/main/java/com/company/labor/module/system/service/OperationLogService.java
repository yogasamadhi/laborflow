package com.company.labor.module.system.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.company.labor.module.system.domain.entity.OperationLogDO;
import com.company.labor.module.system.mapper.OperationLogMapper;
import com.company.labor.common.exception.BusinessException;
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

    public Page<OperationLogDO> page(long page,
                                     long pageSize,
                                     String keyword,
                                     String module,
                                     String action,
                                     Long businessId,
                                     Long operatorId,
                                     LocalDateTime createdFrom,
                                     LocalDateTime createdTo) {
        if (createdFrom != null && createdTo != null && createdFrom.isAfter(createdTo)) {
            throw new BusinessException("操作日志开始时间不能晚于结束时间");
        }
        LambdaQueryWrapper<OperationLogDO> wrapper = new LambdaQueryWrapper<OperationLogDO>()
                .eq(StringUtils.hasText(module), OperationLogDO::getModule, module)
                .eq(StringUtils.hasText(action), OperationLogDO::getAction, action)
                .eq(businessId != null, OperationLogDO::getBusinessId, businessId)
                .eq(operatorId != null, OperationLogDO::getOperatorId, operatorId)
                .ge(createdFrom != null, OperationLogDO::getCreatedAt, createdFrom)
                .le(createdTo != null, OperationLogDO::getCreatedAt, createdTo)
                .and(StringUtils.hasText(keyword), query -> query
                        .like(OperationLogDO::getOperatorName, keyword)
                        .or()
                        .like(OperationLogDO::getContent, keyword)
                        .or()
                        .like(OperationLogDO::getAction, keyword))
                .orderByDesc(OperationLogDO::getCreatedAt)
                .orderByDesc(OperationLogDO::getId);
        return operationLogMapper.selectPage(
                new Page<>(Math.max(page, 1), Math.max(1, Math.min(pageSize, 100))), wrapper);
    }

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

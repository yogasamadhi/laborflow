package com.company.labor.security;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.labor.module.common.domain.entity.BaseDO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("token_blacklist")
public class TokenBlacklistDO extends BaseDO {
    @TableId(type = IdType.INPUT)
    private String jti;
    private Long userId;
    private LocalDateTime expiresAt;
    private Long revokedBy;
    private LocalDateTime revokedAt;
}

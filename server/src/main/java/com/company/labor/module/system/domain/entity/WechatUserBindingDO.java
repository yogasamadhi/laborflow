package com.company.labor.module.system.domain.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.company.labor.module.common.domain.entity.BaseDO;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("wechat_user_binding")
public class WechatUserBindingDO extends BaseDO {
    @TableId(type = IdType.ASSIGN_ID)
    private Long id;
    private String openid;
    private String unionid;
    private Long userId;
    private Long employeeId;
    private String nickname;
    private String status;
    private LocalDateTime lastSessionAt;
    private LocalDateTime lastLoginAt;
}

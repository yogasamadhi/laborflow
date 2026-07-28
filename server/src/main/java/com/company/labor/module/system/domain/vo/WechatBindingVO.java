package com.company.labor.module.system.domain.vo;

import com.company.labor.module.system.domain.entity.WechatUserBindingDO;
import java.time.LocalDateTime;
import lombok.Data;

@Data
public class WechatBindingVO {
    private Long id;
    private String openid;
    private String unionid;
    private Long userId;
    private String userName;
    private Long employeeId;
    private String employeeName;
    private String nickname;
    private String status;
    private LocalDateTime lastSessionAt;
    private LocalDateTime lastLoginAt;

    public static WechatBindingVO from(
            WechatUserBindingDO binding,
            String userName,
            String employeeName) {
        WechatBindingVO vo = new WechatBindingVO();
        vo.setId(binding.getId());
        vo.setOpenid(binding.getOpenid());
        vo.setUnionid(binding.getUnionid());
        vo.setUserId(binding.getUserId());
        vo.setUserName(userName);
        vo.setEmployeeId(binding.getEmployeeId());
        vo.setEmployeeName(employeeName);
        vo.setNickname(binding.getNickname());
        vo.setStatus(binding.getStatus());
        vo.setLastSessionAt(binding.getLastSessionAt());
        vo.setLastLoginAt(binding.getLastLoginAt());
        return vo;
    }
}

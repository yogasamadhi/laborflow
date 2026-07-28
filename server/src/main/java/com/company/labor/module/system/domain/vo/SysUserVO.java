package com.company.labor.module.system.domain.vo;

import java.time.LocalDateTime;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class SysUserVO {
    private Long id;
    private String username;
    private Long employeeId;
    private String employeeName;
    private String nickname;
    private String phone;
    private String status;
    private List<Long> roleIds;
    private List<String> roles;
    private LocalDateTime lastLoginAt;
    private LocalDateTime lockedUntil;
    private LocalDateTime passwordUpdatedAt;
    private LocalDateTime createdAt;
}

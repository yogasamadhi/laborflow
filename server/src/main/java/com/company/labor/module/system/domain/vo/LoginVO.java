package com.company.labor.module.system.domain.vo;

import java.time.Instant;
import java.util.List;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginVO {
    private String token;
    private Instant expiresAt;
    private Long userId;
    private Long employeeId;
    private String username;
    private String nickname;
    private List<String> roles;
}

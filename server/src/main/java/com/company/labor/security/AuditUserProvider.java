package com.company.labor.security;

import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;

@Component
public class AuditUserProvider {
    public Long currentUserId() {
        LoginUser loginUser = currentLoginUser();
        return loginUser == null ? null : loginUser.getUser().getId();
    }

    public LoginUser currentLoginUser() {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return null;
        }
        return loginUser;
    }
}

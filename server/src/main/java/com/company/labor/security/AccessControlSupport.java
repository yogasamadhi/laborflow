package com.company.labor.security;

import java.util.Arrays;
import org.springframework.security.core.Authentication;

public final class AccessControlSupport {
    private AccessControlSupport() {
    }

    public static boolean hasAnyRole(Authentication authentication, String... roles) {
        LoginUser loginUser = loginUser(authentication);
        if (loginUser == null) {
            return false;
        }
        return Arrays.stream(roles).anyMatch(role -> loginUser.getRoles().contains(role));
    }

    public static boolean hasSalaryManageRole(Authentication authentication) {
        return hasAnyRole(authentication, "ADMIN", "BOSS", "FINANCE");
    }

    public static boolean hasProjectManageRole(Authentication authentication) {
        return hasAnyRole(authentication, "ADMIN", "BOSS", "MANAGER");
    }

    public static boolean hasProjectFinanceRole(Authentication authentication) {
        return hasAnyRole(authentication, "ADMIN", "BOSS", "MANAGER", "FINANCE");
    }

    public static Long currentUserId(Authentication authentication) {
        LoginUser loginUser = loginUser(authentication);
        return loginUser == null ? null : loginUser.getUser().getId();
    }

    public static Long currentEmployeeId(Authentication authentication) {
        LoginUser loginUser = loginUser(authentication);
        return loginUser == null ? null : loginUser.getUser().getEmployeeId();
    }

    private static LoginUser loginUser(Authentication authentication) {
        if (authentication == null || !(authentication.getPrincipal() instanceof LoginUser loginUser)) {
            return null;
        }
        return loginUser;
    }
}

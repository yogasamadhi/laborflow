package com.company.labor.security;

import static org.assertj.core.api.Assertions.assertThat;

import com.company.labor.module.system.domain.entity.SysUserDO;
import java.util.List;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;

class AccessControlSupportTest {
    @Test
    void returnsFalseForAnonymousOrUnexpectedPrincipal() {
        assertThat(AccessControlSupport.hasSalaryManageRole(null)).isFalse();
        assertThat(AccessControlSupport.hasProjectManageRole(new UsernamePasswordAuthenticationToken("user", null))).isFalse();
        assertThat(AccessControlSupport.currentUserId(null)).isNull();
        assertThat(AccessControlSupport.currentEmployeeId(null)).isNull();
    }

    @Test
    void recognizesSalaryManagementRoles() {
        assertThat(AccessControlSupport.hasSalaryManageRole(authentication("FINANCE"))).isTrue();
        assertThat(AccessControlSupport.hasSalaryManageRole(authentication("BOSS"))).isTrue();
        assertThat(AccessControlSupport.hasSalaryManageRole(authentication("EMPLOYEE"))).isFalse();
    }

    @Test
    void recognizesProjectManagementRoles() {
        assertThat(AccessControlSupport.hasProjectManageRole(authentication("MANAGER"))).isTrue();
        assertThat(AccessControlSupport.hasProjectManageRole(authentication("FINANCE"))).isTrue();
        assertThat(AccessControlSupport.hasProjectManageRole(authentication("EMPLOYEE"))).isFalse();
    }

    @Test
    void exposesCurrentUserAndEmployeeIds() {
        Authentication authentication = authentication("EMPLOYEE");

        assertThat(AccessControlSupport.currentUserId(authentication)).isEqualTo(7L);
        assertThat(AccessControlSupport.currentEmployeeId(authentication)).isEqualTo(11L);
    }

    private Authentication authentication(String... roles) {
        SysUserDO user = new SysUserDO();
        user.setId(7L);
        user.setUsername("tester");
        user.setPassword("secret");
        user.setEmployeeId(11L);
        user.setStatus("active");
        LoginUser loginUser = new LoginUser(user, List.of(roles));
        return new UsernamePasswordAuthenticationToken(loginUser, null, loginUser.getAuthorities());
    }
}

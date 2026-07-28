package com.company.labor.security;

import com.company.labor.module.system.domain.entity.SysUserDO;
import java.util.Collection;
import java.util.List;
import lombok.Getter;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

@Getter
public class LoginUser implements UserDetails {
    private final SysUserDO user;
    private final List<String> roles;
    private final boolean accountEligible;

    public LoginUser(SysUserDO user, List<String> roles) {
        this(user, roles, true);
    }

    public LoginUser(SysUserDO user, List<String> roles, boolean accountEligible) {
        this.user = user;
        this.roles = roles;
        this.accountEligible = accountEligible;
    }

    @Override
    public Collection<? extends GrantedAuthority> getAuthorities() {
        return roles.stream().map(role -> new SimpleGrantedAuthority("ROLE_" + role)).toList();
    }

    @Override
    public String getPassword() {
        return user.getPassword();
    }

    @Override
    public String getUsername() {
        return user.getUsername();
    }

    @Override
    public boolean isEnabled() {
        return accountEligible && "active".equals(user.getStatus());
    }
}

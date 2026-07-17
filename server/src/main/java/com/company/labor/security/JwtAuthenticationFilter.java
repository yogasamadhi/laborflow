package com.company.labor.security;

import com.company.labor.module.system.service.SysUserService;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.filter.OncePerRequestFilter;

@Slf4j
@Component
@RequiredArgsConstructor
public class JwtAuthenticationFilter extends OncePerRequestFilter {
    private final JwtTokenProvider jwtTokenProvider;
    private final SysUserService sysUserService;
    private final TokenBlacklistService tokenBlacklistService;
    private final AuthenticationErrorWriter errorWriter;

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain filterChain)
            throws ServletException, IOException {
        String header = request.getHeader("Authorization");
        if (header != null && header.startsWith("Bearer ")) {
            String token = header.substring(7).trim();
            if (!StringUtils.hasText(token)) {
                SecurityContextHolder.clearContext();
                errorWriter.unauthorized(response, "登录凭证无效，请重新登录");
                return;
            }
            try {
                JwtTokenProvider.ParsedToken parsed = jwtTokenProvider.parse(token);
                if (tokenBlacklistService.isRevoked(parsed.jti())) {
                    log.debug("token rejected (revoked): jti={}", parsed.jti());
                    SecurityContextHolder.clearContext();
                    errorWriter.unauthorized(response, "登录已失效，请重新登录");
                    return;
                }
                Long userId = Long.valueOf(parsed.subject());
                LoginUser loginUser = sysUserService.loadLoginUserById(userId);
                if (!loginUser.isEnabled()) {
                    SecurityContextHolder.clearContext();
                    errorWriter.unauthorized(response, "账号已停用，请重新登录或联系管理员");
                    return;
                }
                UsernamePasswordAuthenticationToken authentication = new UsernamePasswordAuthenticationToken(
                        loginUser, null, loginUser.getAuthorities());
                SecurityContextHolder.getContext().setAuthentication(authentication);
            } catch (JwtAuthenticationException ex) {
                log.debug("jwt rejected: {}", ex.getMessage());
                SecurityContextHolder.clearContext();
                errorWriter.unauthorized(response, ex.getMessage());
                return;
            } catch (UsernameNotFoundException ex) {
                log.debug("login user not found for token subject");
                SecurityContextHolder.clearContext();
                errorWriter.unauthorized(response, "账号不存在或已停用，请重新登录");
                return;
            }
        }
        filterChain.doFilter(request, response);
    }
}

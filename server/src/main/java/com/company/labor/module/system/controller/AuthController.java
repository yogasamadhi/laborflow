package com.company.labor.module.system.controller;

import com.company.labor.common.result.Result;
import com.company.labor.module.system.domain.dto.LoginDTO;
import com.company.labor.module.system.domain.dto.RegisterDTO;
import com.company.labor.module.system.domain.dto.WechatSessionDTO;
import com.company.labor.module.system.domain.vo.LoginVO;
import com.company.labor.module.system.domain.vo.WechatSessionVO;
import com.company.labor.module.system.service.AuthRegistrationService;
import com.company.labor.module.system.service.LoginSecurityService;
import com.company.labor.module.system.service.WechatBindingService;
import com.company.labor.module.system.service.WechatAuthService;
import com.company.labor.security.JwtTokenProvider;
import com.company.labor.security.LoginUser;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.LockedException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthenticationManager authenticationManager;
    private final JwtTokenProvider jwtTokenProvider;
    private final LoginSecurityService loginSecurityService;
    private final AuthRegistrationService authRegistrationService;
    private final WechatAuthService wechatAuthService;
    private final WechatBindingService wechatBindingService;

    @PostMapping("/register")
    public Result<LoginVO> register(@Valid @RequestBody RegisterDTO dto, HttpServletRequest request) {
        LoginUser loginUser = authRegistrationService.register(dto);
        loginSecurityService.recordLoginSuccess(loginUser, clientIp(request));
        return Result.success(loginVO(loginUser, jwtTokenProvider.createLoginToken(loginUser)));
    }

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto, HttpServletRequest request) {
        loginSecurityService.checkLoginAllowed(dto.getUsername());
        Authentication authentication;
        try {
            authentication = authenticationManager.authenticate(
                    new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
        } catch (AuthenticationException ex) {
            if (loginSecurityService.recordLoginFailure(dto.getUsername())) {
                throw new LockedException("登录失败次数过多，账号已临时锁定15分钟");
            }
            throw ex;
        }
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        loginSecurityService.recordLoginSuccess(loginUser, clientIp(request));
        wechatBindingService.bindLoginUser(dto.getWechatOpenid(), dto.getWechatNickname(), loginUser);
        return Result.success(loginVO(loginUser, jwtTokenProvider.createLoginToken(loginUser)));
    }

    @GetMapping("/me")
    public Result<LoginVO> me(Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return Result.success(new LoginVO(null, null, loginUser.getUser().getId(), loginUser.getUser().getEmployeeId(), loginUser.getUsername(), loginUser.getUser().getNickname(), loginUser.getRoles()));
    }

    @PostMapping("/wechat/session")
    public Result<WechatSessionVO> wechatSession(@Valid @RequestBody WechatSessionDTO dto) {
        return Result.success(wechatAuthService.exchangeSession(dto.getCode()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }

    private LoginVO loginVO(LoginUser loginUser, JwtTokenProvider.JwtToken token) {
        return new LoginVO(token.value(), token.expiresAt(), loginUser.getUser().getId(), loginUser.getUser().getEmployeeId(),
                loginUser.getUsername(), loginUser.getUser().getNickname(), loginUser.getRoles());
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        String realIp = request.getHeader("X-Real-IP");
        if (realIp != null && !realIp.isBlank()) {
            return realIp.trim();
        }
        return request.getRemoteAddr();
    }
}

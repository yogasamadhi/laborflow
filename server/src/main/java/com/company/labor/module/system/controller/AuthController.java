package com.company.labor.module.system.controller;

import com.company.labor.common.result.Result;
import com.company.labor.module.system.domain.dto.LoginDTO;
import com.company.labor.module.system.domain.vo.LoginVO;
import com.company.labor.security.JwtTokenProvider;
import com.company.labor.security.LoginUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
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

    @PostMapping("/login")
    public Result<LoginVO> login(@Valid @RequestBody LoginDTO dto) {
        Authentication authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(dto.getUsername(), dto.getPassword()));
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        JwtTokenProvider.JwtToken token = jwtTokenProvider.createLoginToken(loginUser);
        return Result.success(new LoginVO(token.value(), token.expiresAt(), loginUser.getUser().getId(), loginUser.getUser().getEmployeeId(), loginUser.getUsername(), loginUser.getUser().getNickname(), loginUser.getRoles()));
    }

    @GetMapping("/me")
    public Result<LoginVO> me(Authentication authentication) {
        LoginUser loginUser = (LoginUser) authentication.getPrincipal();
        return Result.success(new LoginVO(null, null, loginUser.getUser().getId(), loginUser.getUser().getEmployeeId(), loginUser.getUsername(), loginUser.getUser().getNickname(), loginUser.getRoles()));
    }

    @PostMapping("/logout")
    public Result<Void> logout() {
        return Result.success();
    }
}

package com.company.labor.module.system.controller;

import com.company.labor.common.result.Result;
import com.company.labor.module.system.domain.vo.DashboardOverviewVO;
import com.company.labor.module.system.service.DashboardOverviewService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/dashboard")
@RequiredArgsConstructor
public class DashboardOverviewController {
    private final DashboardOverviewService service;

    @GetMapping("/overview")
    public Result<DashboardOverviewVO> overview(
            @RequestParam(required = false) String month,
            Authentication authentication) {
        return Result.success(service.overview(month, authentication));
    }
}

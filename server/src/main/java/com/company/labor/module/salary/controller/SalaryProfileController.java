package com.company.labor.module.salary.controller;

import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.salary.domain.dto.SalaryProfileSaveDTO;
import com.company.labor.module.salary.domain.entity.SalaryProfileDO;
import com.company.labor.module.salary.domain.entity.SalaryProfileItemDO;
import com.company.labor.module.salary.service.SalaryProfileService;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/salary/profiles")
@RequiredArgsConstructor
public class SalaryProfileController {
    private final SalaryProfileService service;

    @GetMapping
    public Result<PageResult<SalaryProfileDO>> list(@RequestParam(defaultValue = "1") long page, @RequestParam(defaultValue = "20") long pageSize, @RequestParam(required = false) Long employeeId) {
        return Result.success(PageResult.of(service.search(page, pageSize, employeeId)));
    }

    @PostMapping
    public Result<SalaryProfileDO> create(@RequestBody SalaryProfileSaveDTO dto) {
        return Result.success(service.saveProfile(null, dto));
    }

    @GetMapping("/{id}")
    public Result<Map<String, Object>> detail(@PathVariable Long id) {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("profile", service.getById(id));
        result.put("items", service.items(id));
        return Result.success(result);
    }

    @PutMapping("/{id}")
    public Result<SalaryProfileDO> update(@PathVariable Long id, @RequestBody SalaryProfileSaveDTO dto) {
        return Result.success(service.saveProfile(id, dto));
    }
}


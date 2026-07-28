package com.company.labor.module.project.controller;

import com.company.labor.common.result.PageResult;
import com.company.labor.common.result.Result;
import com.company.labor.module.project.domain.dto.ProjectMemberSaveDTO;
import com.company.labor.module.project.domain.entity.ProjectMemberDO;
import com.company.labor.module.project.service.ProjectMemberService;
import com.company.labor.security.RoleExpressions;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/project-members")
@RequiredArgsConstructor
@PreAuthorize(RoleExpressions.PROJECT_MANAGE)
public class ProjectMemberController {
    private final ProjectMemberService service;

    @GetMapping
    public Result<PageResult<ProjectMemberDO>> page(
            @RequestParam(defaultValue = "1") long page,
            @RequestParam(defaultValue = "20") long pageSize,
            @RequestParam(required = false) Long projectId,
            @RequestParam(required = false) Long employeeId,
            @RequestParam(required = false) String status) {
        return Result.success(PageResult.of(service.page(page, pageSize, projectId, employeeId, status)));
    }

    @PostMapping
    public Result<ProjectMemberDO> create(@Valid @RequestBody ProjectMemberSaveDTO dto) {
        return Result.success(service.create(dto));
    }

    @PutMapping("/{id}")
    public Result<ProjectMemberDO> update(@PathVariable Long id, @Valid @RequestBody ProjectMemberSaveDTO dto) {
        return Result.success(service.update(id, dto));
    }

    @DeleteMapping("/{id}")
    public Result<Void> delete(@PathVariable Long id) {
        service.delete(id);
        return Result.success();
    }
}

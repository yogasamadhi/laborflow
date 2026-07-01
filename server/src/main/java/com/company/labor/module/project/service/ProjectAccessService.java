package com.company.labor.module.project.service;

import java.util.Set;
import org.springframework.security.core.Authentication;

public interface ProjectAccessService {
    boolean hasProjectWideAccess(Authentication authentication);

    Set<Long> authorizedProjectIds(Authentication authentication);

    void requireProjectAccess(Long projectId, Authentication authentication);
}

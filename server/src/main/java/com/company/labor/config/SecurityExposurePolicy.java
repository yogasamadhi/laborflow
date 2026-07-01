package com.company.labor.config;

import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;

@Component
public class SecurityExposurePolicy {
    private final Environment environment;

    public SecurityExposurePolicy(Environment environment) {
        this.environment = environment;
    }

    public boolean isProduction() {
        return environment.acceptsProfiles(Profiles.of("prod", "production"));
    }
}

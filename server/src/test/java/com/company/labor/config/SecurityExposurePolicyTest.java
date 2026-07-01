package com.company.labor.config;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class SecurityExposurePolicyTest {
    @Test
    void treatsProdProfileAsProduction() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        assertTrue(new SecurityExposurePolicy(environment).isProduction());
    }

    @Test
    void treatsProductionProfileAsProduction() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("production");

        assertTrue(new SecurityExposurePolicy(environment).isProduction());
    }

    @Test
    void treatsDevProfileAsNonProduction() {
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("dev");

        assertFalse(new SecurityExposurePolicy(environment).isProduction());
    }
}

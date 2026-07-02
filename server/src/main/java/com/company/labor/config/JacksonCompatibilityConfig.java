package com.company.labor.config;

import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class JacksonCompatibilityConfig {
    @Bean
    @ConditionalOnMissingBean(com.fasterxml.jackson.databind.ObjectMapper.class)
    public com.fasterxml.jackson.databind.ObjectMapper jackson2ObjectMapper() {
        return new com.fasterxml.jackson.databind.ObjectMapper().findAndRegisterModules();
    }
}

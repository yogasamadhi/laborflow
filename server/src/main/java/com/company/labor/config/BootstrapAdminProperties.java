package com.company.labor.config;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "laborflow.bootstrap-admin")
public class BootstrapAdminProperties {
    private String username;
    private String password;
}

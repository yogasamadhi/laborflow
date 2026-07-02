package com.company.labor.module.file.storage;

import lombok.Data;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Data
@Component
@ConfigurationProperties(prefix = "laborflow.file.storage.object")
public class ObjectStorageProperties {
    private String endpoint;
    private String bucket;
    private String authHeaderName = "Authorization";
    private String authHeaderValue;
}

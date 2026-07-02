package com.company.labor.config;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

@Component
public class ProductionConfigurationValidator implements ApplicationRunner {
    private static final Set<String> PLACEHOLDER_JWT_SECRETS = Set.of(
            "please-change-this-secret-to-at-least-32-characters",
            "change-this-dev-secret-to-at-least-32-characters"
    );
    private static final String DEFAULT_DATABASE_URL = "jdbc:postgresql://localhost:5434/laborflow";
    private static final String DEFAULT_DATABASE_PASSWORD = "laborflow123";

    private final Environment environment;

    public ProductionConfigurationValidator(Environment environment) {
        this.environment = environment;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (!environment.acceptsProfiles(Profiles.of("prod", "production"))) {
            return;
        }

        List<String> errors = new ArrayList<>();
        validateJwtSecret(errors);
        validateDatabase(errors);
        validateFileStorage(errors);

        if (!errors.isEmpty()) {
            throw new IllegalStateException("Production configuration is incomplete: " + String.join("; ", errors));
        }
    }

    private void validateJwtSecret(List<String> errors) {
        String secret = environment.getProperty("laborflow.jwt.secret");
        if (!StringUtils.hasText(secret)) {
            errors.add("JWT_SECRET must be configured");
            return;
        }
        if (PLACEHOLDER_JWT_SECRETS.contains(secret)) {
            errors.add("JWT_SECRET must not use the default or example value");
        }
        if (secret.getBytes(StandardCharsets.UTF_8).length < 32) {
            errors.add("JWT_SECRET must be at least 32 bytes");
        }
    }

    private void validateDatabase(List<String> errors) {
        String url = environment.getProperty("spring.datasource.url");
        if (!StringUtils.hasText(url)) {
            errors.add("DATABASE_URL must be configured");
        } else if (DEFAULT_DATABASE_URL.equals(url)) {
            errors.add("DATABASE_URL must not use the local development default");
        }

        String password = environment.getProperty("spring.datasource.password");
        if (!StringUtils.hasText(password)) {
            errors.add("POSTGRES_PASSWORD must be configured");
        } else if (DEFAULT_DATABASE_PASSWORD.equals(password)) {
            errors.add("POSTGRES_PASSWORD must not use the local development default");
        }
    }

    private void validateFileStorage(List<String> errors) {
        String storageType = environment.getProperty("laborflow.file.storage.type", "local");
        if ("object".equalsIgnoreCase(storageType)) {
            validateObjectStorage(errors);
            return;
        }
        if (!"local".equalsIgnoreCase(storageType)) {
            errors.add("FILE_STORAGE_TYPE must be local or object");
            return;
        }
        errors.add("FILE_STORAGE_TYPE must be object in production");
    }

    private void validateObjectStorage(List<String> errors) {
        if (!StringUtils.hasText(environment.getProperty("laborflow.file.storage.object.endpoint"))) {
            errors.add("OBJECT_STORAGE_ENDPOINT must be configured");
        }
        if (!StringUtils.hasText(environment.getProperty("laborflow.file.storage.object.bucket"))) {
            errors.add("OBJECT_STORAGE_BUCKET must be configured");
        }
    }
}

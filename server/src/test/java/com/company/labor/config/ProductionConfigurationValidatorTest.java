package com.company.labor.config;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;
import org.springframework.mock.env.MockEnvironment;

class ProductionConfigurationValidatorTest {
    @Test
    void allowsDevelopmentDefaultsOutsideProduction() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("laborflow.jwt.secret", "short")
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5434/laborflow")
                .withProperty("spring.datasource.password", "laborflow123")
                .withProperty("laborflow.upload-dir", "./uploads");

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(environment);

        assertDoesNotThrow(() -> validator.run(null));
    }

    @Test
    void rejectsProductionDefaults() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("laborflow.jwt.secret", "please-change-this-secret-to-at-least-32-characters")
                .withProperty("spring.datasource.url", "jdbc:postgresql://localhost:5434/laborflow")
                .withProperty("spring.datasource.password", "laborflow123")
                .withProperty("laborflow.upload-dir", "./uploads");
        environment.setActiveProfiles("prod");

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(environment);

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }

    @Test
    void allowsExplicitProductionConfiguration() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("laborflow.jwt.secret", "prod-secret-with-more-than-thirty-two-bytes")
                .withProperty("spring.datasource.url", "jdbc:postgresql://postgres.internal:5432/laborflow")
                .withProperty("spring.datasource.password", "different-production-password")
                .withProperty("laborflow.file.storage.type", "object")
                .withProperty("laborflow.file.storage.object.endpoint", "https://object.internal")
                .withProperty("laborflow.file.storage.object.bucket", "laborflow");
        environment.setActiveProfiles("production");

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(environment);

        assertDoesNotThrow(() -> validator.run(null));
    }

    @Test
    void rejectsProductionLocalFileStorage() {
        MockEnvironment environment = new MockEnvironment()
                .withProperty("laborflow.jwt.secret", "prod-secret-with-more-than-thirty-two-bytes")
                .withProperty("spring.datasource.url", "jdbc:postgresql://postgres.internal:5432/laborflow")
                .withProperty("spring.datasource.password", "different-production-password")
                .withProperty("laborflow.file.storage.type", "local")
                .withProperty("laborflow.file.storage.local.base-dir", "/mnt/uploads");
        environment.setActiveProfiles("production");

        ProductionConfigurationValidator validator = new ProductionConfigurationValidator(environment);

        assertThrows(IllegalStateException.class, () -> validator.run(null));
    }
}

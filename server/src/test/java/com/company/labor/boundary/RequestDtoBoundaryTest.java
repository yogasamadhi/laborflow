package com.company.labor.boundary;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

class RequestDtoBoundaryTest {
    private static final Pattern REQUEST_BODY_DO = Pattern.compile("@RequestBody(?:\\([^)]*\\))?\\s+[\\w.<>]+DO\\s+\\w+");

    @Test
    void controllersDoNotAcceptPersistenceObjectsAsWriteRequests() throws IOException {
        Path moduleRoot = Path.of("src/main/java/com/company/labor/module");
        List<String> violations;
        try (var paths = Files.walk(moduleRoot)) {
            violations = paths
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .map(this::violation)
                    .filter(message -> !message.isBlank())
                    .toList();
        }

        assertThat(violations)
                .as("Controller write request bodies should use DTOs instead of DO persistence objects")
                .isEmpty();
    }

    @Test
    void requestBodiesAreValidatedAtControllerBoundary() throws IOException {
        Path moduleRoot = Path.of("src/main/java/com/company/labor/module");
        List<String> violations;
        try (var paths = Files.walk(moduleRoot)) {
            violations = paths
                    .filter(path -> path.getFileName().toString().endsWith("Controller.java"))
                    .flatMap(path -> missingValidViolations(path).stream())
                    .toList();
        }

        assertThat(violations)
                .as("Controller request bodies should be annotated with @Valid")
                .isEmpty();
    }

    private String violation(Path path) {
        try {
            String source = Files.readString(path);
            if (REQUEST_BODY_DO.matcher(source).find()) {
                return path.toString();
            }
            return "";
        } catch (IOException ex) {
            throw new IllegalStateException("读取 Controller 源码失败: " + path, ex);
        }
    }

    private List<String> missingValidViolations(Path path) {
        try {
            List<String> lines = Files.readAllLines(path);
            return java.util.stream.IntStream.range(0, lines.size())
                    .filter(index -> lines.get(index).contains("@RequestBody") && !lines.get(index).contains("@Valid @RequestBody"))
                    .mapToObj(index -> path + ":" + (index + 1))
                    .toList();
        } catch (IOException ex) {
            throw new IllegalStateException("读取 Controller 源码失败: " + path, ex);
        }
    }
}

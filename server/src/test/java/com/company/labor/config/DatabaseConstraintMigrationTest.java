package com.company.labor.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.Test;

class DatabaseConstraintMigrationTest {
    @Test
    void integrityMigrationAddsCoreConstraintsConservatively() throws IOException {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V13__data_integrity_constraints.sql"));

        assertThat(sql)
                .contains("FOREIGN KEY")
                .contains("CHECK")
                .contains("CREATE UNIQUE INDEX")
                .contains("NOT VALID")
                .contains("salary_month ~ '^\\d{4}-(0[1-9]|1[0-2])$'")
                .contains("payment_month ~ '^\\d{4}-(0[1-9]|1[0-2])$'")
                .contains("amount >= 0")
                .contains("status IN");
        assertThat(sql).doesNotContain("DROP TABLE", "DROP COLUMN", "TRUNCATE");
    }
}

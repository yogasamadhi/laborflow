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

    @Test
    void functionalIntegrityMigrationGuardsFormulasAndProjectHierarchy() throws IOException {
        String sql = Files.readString(Path.of("src/main/resources/db/migration/V23__functional_integrity_guards.sql"));

        assertThat(sql)
                .contains("status = 'draft'")
                .contains("actual_amount = detail.payable_amount - detail.deduction_amount")
                .contains("ck_salary_monthly_detail_formula")
                .contains("ck_salary_monthly_total_formula")
                .contains("UNIQUE (id, project_id)")
                .contains("FOREIGN KEY (project_section_id, project_id)")
                .contains("fk_daily_report_section_project")
                .contains("fk_variation_section_project")
                .contains("fk_contract_item_section_project")
                .contains("fk_machinery_work_section_project")
                .contains("fk_material_record_section_project")
                .contains("fk_project_document_section_project")
                .contains("NOT VALID");
        assertThat(sql).doesNotContain("DROP TABLE", "DROP COLUMN", "TRUNCATE");
    }
}

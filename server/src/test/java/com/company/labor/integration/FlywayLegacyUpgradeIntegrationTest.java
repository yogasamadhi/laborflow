package com.company.labor.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.sql.Connection;
import java.sql.DriverManager;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.sql.Statement;
import org.flywaydb.core.Flyway;
import org.flywaydb.core.api.MigrationVersion;
import org.junit.jupiter.api.Test;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

@Testcontainers
class FlywayLegacyUpgradeIntegrationTest {
    private static final long USER_ID = 9_100_004L;
    private static final long EMPLOYEE_ID = 9_100_003L;
    private static final long FILE_ID = 9_100_005L;
    private static final long PROJECT_ID = 9_100_006L;
    private static final long SECTION_ID = 9_100_007L;
    private static final long TEAM_ID = 9_100_008L;
    private static final long DAILY_REPORT_ID = 9_100_009L;
    private static final long VARIATION_ID = 9_100_011L;
    private static final long SALARY_MONTHLY_ID = 9_100_013L;
    private static final long SALARY_DETAIL_ID = 9_100_014L;

    @Container
    static final PostgreSQLContainer<?> POSTGRES = new PostgreSQLContainer<>("postgres:18-alpine")
            .withDatabaseName("laborflow_upgrade_it")
            .withUsername("laborflow_upgrade_it")
            .withPassword("laborflow_upgrade_it");

    @Test
    void upgradesAnonymousV12FixtureWithoutLosingCoreBusinessData() throws SQLException {
        migrateTo("12");
        insertLegacyFixture();
        migrateTo(null);

        assertThat(singleString("""
                SELECT version
                FROM flyway_schema_history
                WHERE success = true
                ORDER BY installed_rank DESC
                LIMIT 1
                """)).isEqualTo("21");
        assertThat(count("SELECT COUNT(*) FROM sys_user WHERE id = " + USER_ID + " AND username = 'legacy_upgrade_user'"))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM employee WHERE id = " + EMPLOYEE_ID + " AND name = '历史迁移员工'"))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM salary_monthly_detail WHERE id = " + SALARY_DETAIL_ID + " AND payslip_status = 'unconfirmed'"))
                .isEqualTo(1);
        assertThat(count("""
                SELECT COUNT(*)
                FROM salary_monthly
                WHERE id = 9100013
                  AND include_fixed_salary = 1
                  AND include_attendance_deduction = 1
                  AND include_performance_salary = 1
                  AND include_project_bonus = 1
                """)).isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM project WHERE id = " + PROJECT_ID + " AND name = '历史迁移项目'"))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM daily_work_report_file WHERE daily_work_report_id = " + DAILY_REPORT_ID + " AND file_id = " + FILE_ID))
                .isEqualTo(1);
        assertThat(count("SELECT COUNT(*) FROM variation_order_file WHERE variation_order_id = " + VARIATION_ID + " AND file_id = " + FILE_ID))
                .isEqualTo(1);
        assertThat(singleDecimal("SELECT confirmed_amount FROM variation_order WHERE id = " + VARIATION_ID))
                .isEqualByComparingTo("500.00");
        assertThat(count("""
                SELECT COUNT(*)
                FROM wechat_user_binding
                WHERE openid = 'legacy_upgrade_openid'
                  AND user_id = 9100004
                  AND employee_id = 9100003
                  AND status = 'active'
                """)).isEqualTo(1);
        assertThat(count("""
                SELECT COUNT(*)
                FROM project_member
                WHERE project_id = 9100006
                  AND user_id = 9100004
                  AND employee_id = 9100003
                  AND status = 'active'
                """)).isEqualTo(1);
    }

    private void migrateTo(String target) {
        var configuration = Flyway.configure()
                .dataSource(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword())
                .locations("classpath:db/migration");
        if (target != null) {
            configuration.target(MigrationVersion.fromVersion(target));
        }
        configuration.load().migrate();
    }

    private void insertLegacyFixture() throws SQLException {
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement()) {
            statement.executeUpdate("""
                    INSERT INTO employee (id, name, phone, employee_type, status, deleted)
                    VALUES (9100003, '历史迁移员工', 'fixture-phone', 'project_staff', 'active', 0)
                    """);
            statement.executeUpdate("""
                    INSERT INTO sys_user (id, username, password, employee_id, nickname, status, deleted, failed_login_count)
                    VALUES (9100004, 'legacy_upgrade_user', '{legacy-fixture-hash}', 9100003, '历史迁移用户', 'active', 0, 0)
                    """);
            statement.executeUpdate("INSERT INTO sys_user_role (id, user_id, role_id) VALUES (9100040, 9100004, 1005)");
            statement.executeUpdate("""
                    INSERT INTO file_record (id, original_name, file_name, file_path, file_size, content_type, created_by)
                    VALUES (9100005, 'legacy-evidence.txt', 'legacy-evidence.txt', 'legacy/fixture.txt', 42, 'text/plain', 9100004)
                    """);
            statement.executeUpdate("""
                    INSERT INTO project (id, name, contract_amount, status, deleted, created_by)
                    VALUES (9100006, '历史迁移项目', 10000.00, 'in_progress', 0, 9100004)
                    """);
            statement.executeUpdate("""
                    INSERT INTO project_section (id, project_id, name, length, deleted, created_by)
                    VALUES (9100007, 9100006, '历史区段', 100.00, 0, 9100004)
                    """);
            statement.executeUpdate("""
                    INSERT INTO team (id, name, settlement_method, status, deleted, created_by)
                    VALUES (9100008, '历史班组', 'quantity', 'active', 0, 9100004)
                    """);
            statement.executeUpdate("""
                    INSERT INTO daily_work_report (
                      id, work_date, project_id, project_section_id, work_process_id, completed_quantity,
                      unit, team_id, worker_count, status, deleted, created_by)
                    VALUES (9100009, DATE '2025-01-15', 9100006, 9100007, 3001, 12.50,
                      'm', 9100008, 3, 'confirmed', 0, 9100004)
                    """);
            statement.executeUpdate("""
                    INSERT INTO daily_work_report_file (id, daily_work_report_id, file_id, file_type)
                    VALUES (9100010, 9100009, 9100005, 'photo')
                    """);
            statement.executeUpdate("""
                    INSERT INTO variation_order (
                      id, project_id, project_section_id, occurrence_date, variation_type, increased_quantity,
                      estimated_amount, labor_count, status, deleted, created_by)
                    VALUES (9100011, 9100006, 9100007, DATE '2025-01-16', '现场变更', 1.00,
                      500.00, 1, 'confirmed', 0, 9100004)
                    """);
            statement.executeUpdate("""
                    INSERT INTO variation_order_file (id, variation_order_id, file_id, file_type)
                    VALUES (9100012, 9100011, 9100005, 'evidence')
                    """);
            statement.executeUpdate("""
                    INSERT INTO salary_monthly (
                      id, salary_month, title, total_employee_count, total_payable_amount,
                      total_deduction_amount, total_actual_amount, status, deleted, created_by)
                    VALUES (9100013, '2025-01', '历史工资表', 1, 1000.00, 0.00, 1000.00, 'paid', 0, 9100004)
                    """);
            statement.executeUpdate("""
                    INSERT INTO salary_monthly_detail (
                      id, salary_monthly_id, employee_id, project_id, payable_amount, deduction_amount,
                      actual_amount, payment_status, payment_date, payment_method, payment_proof_url,
                      deleted, created_by)
                    VALUES (9100014, 9100013, 9100003, 9100006, 1000.00, 0.00,
                      1000.00, 'paid', DATE '2025-01-31', 'bank_transfer', '/api/files/9100005/download', 0, 9100004)
                    """);
            statement.executeUpdate("""
                    INSERT INTO salary_detail_item (id, salary_detail_id, salary_item_id, salary_item_name, salary_item_type, amount)
                    VALUES (9100015, 9100014, 2001, '基本工资', 'earning', 1000.00)
                    """);
            statement.executeUpdate("""
                    INSERT INTO wechat_user_binding (id, openid, user_id, employee_id, nickname, status, deleted, created_by)
                    VALUES (9100016, 'legacy_upgrade_openid', 9100004, 9100003, '历史迁移微信', 'active', 0, 9100004)
                    """);
            statement.executeUpdate("""
                    INSERT INTO project_member (id, project_id, user_id, employee_id, role_name, status, deleted, created_by)
                    VALUES (9100017, 9100006, 9100004, 9100003, '现场人员', 'active', 0, 9100004)
                    """);
        }
    }

    private int count(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getInt(1);
        }
    }

    private String singleString(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getString(1);
        }
    }

    private BigDecimal singleDecimal(String sql) throws SQLException {
        try (Connection connection = DriverManager.getConnection(POSTGRES.getJdbcUrl(), POSTGRES.getUsername(), POSTGRES.getPassword());
             Statement statement = connection.createStatement();
             ResultSet resultSet = statement.executeQuery(sql)) {
            resultSet.next();
            return resultSet.getBigDecimal(1);
        }
    }
}

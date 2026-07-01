package com.company.labor.common.softdelete;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.softdelete.SoftDeleteGuard.Reference;
import org.junit.jupiter.api.Test;
import org.springframework.jdbc.core.JdbcTemplate;

class SoftDeleteGuardTest {
    @Test
    void allowsDeleteWhenNoReferencesExist() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(1) FROM salary_profile WHERE employee_id = ? AND deleted = 0"),
                eq(Long.class),
                eq(1L))).thenReturn(0L);
        SoftDeleteGuard guard = new SoftDeleteGuard(jdbcTemplate);

        guard.requireNotReferenced("员工", 1L, Reference.active("salary_profile", "employee_id", "工资档案"));
    }

    @Test
    void rejectsDeleteWhenReferencesExist() {
        JdbcTemplate jdbcTemplate = mock(JdbcTemplate.class);
        when(jdbcTemplate.queryForObject(
                eq("SELECT COUNT(1) FROM salary_detail_item WHERE salary_item_id = ?"),
                eq(Long.class),
                eq(2L))).thenReturn(3L);
        SoftDeleteGuard guard = new SoftDeleteGuard(jdbcTemplate);

        assertThatThrownBy(() -> guard.requireNotReferenced("工资项", 2L,
                Reference.all("salary_detail_item", "salary_item_id", "工资明细项")))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("工资项已被工资明细项(3)引用，不能删除");
    }
}

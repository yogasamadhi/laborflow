package com.company.labor.common.softdelete;

import com.company.labor.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class SoftDeleteGuard {
    private final JdbcTemplate jdbcTemplate;

    public record Reference(String tableName, String columnName, String label, boolean deletedAware) {
        public static Reference active(String tableName, String columnName, String label) {
            return new Reference(tableName, columnName, label, true);
        }

        public static Reference all(String tableName, String columnName, String label) {
            return new Reference(tableName, columnName, label, false);
        }
    }

    public void requireNotReferenced(String targetLabel, Long targetId, Reference... references) {
        if (targetId == null) {
            throw new BusinessException(targetLabel + "不存在");
        }
        List<String> usedBy = new ArrayList<>();
        for (Reference reference : references) {
            long count = count(reference, targetId);
            if (count > 0) {
                usedBy.add(reference.label() + "(" + count + ")");
            }
        }
        if (!usedBy.isEmpty()) {
            throw new BusinessException(targetLabel + "已被" + String.join("、", usedBy) + "引用，不能删除");
        }
    }

    private long count(Reference reference, Long targetId) {
        String sql = "SELECT COUNT(1) FROM " + reference.tableName()
                + " WHERE " + reference.columnName() + " = ?"
                + (reference.deletedAware() ? " AND deleted = 0" : "");
        Long count = jdbcTemplate.queryForObject(sql, Long.class, targetId);
        return count == null ? 0L : count;
    }
}

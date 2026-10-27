package com.company.labor.module.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.labor.module.salary.domain.entity.SalaryMonthlyDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

public interface SalaryMonthlyMapper extends BaseMapper<SalaryMonthlyDO> {
    @Select("""
        <script>
        SELECT monthly.*, COALESCE(department.name, '历史全公司') AS department_name
        FROM salary_monthly monthly
        LEFT JOIN department ON department.id = monthly.department_id
        WHERE (#{deleted} &lt; 0 OR monthly.deleted = #{deleted})
        <if test="salaryMonth != null and salaryMonth != ''">
          AND monthly.salary_month = #{salaryMonth}
        </if>
        <if test="status != null and status != ''">
          AND monthly.status = #{status}
        </if>
        <if test="departmentId != null">
          AND monthly.department_id = #{departmentId}
        </if>
        ORDER BY monthly.salary_month DESC, department.sort_order NULLS LAST, department.name NULLS LAST, monthly.created_at DESC
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<SalaryMonthlyDO> selectPageIncludingDeleted(
            @Param("deleted") Integer deleted,
            @Param("salaryMonth") String salaryMonth,
            @Param("status") String status,
            @Param("departmentId") Long departmentId,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM salary_monthly monthly
        WHERE (#{deleted} &lt; 0 OR monthly.deleted = #{deleted})
        <if test="salaryMonth != null and salaryMonth != ''">
          AND monthly.salary_month = #{salaryMonth}
        </if>
        <if test="status != null and status != ''">
          AND monthly.status = #{status}
        </if>
        <if test="departmentId != null">
          AND monthly.department_id = #{departmentId}
        </if>
        </script>
        """)
    long countIncludingDeleted(
            @Param("deleted") Integer deleted,
            @Param("salaryMonth") String salaryMonth,
            @Param("status") String status,
            @Param("departmentId") Long departmentId);

    @Select("SELECT * FROM salary_monthly WHERE id = #{id}")
    SalaryMonthlyDO selectByIdIncludingDeleted(@Param("id") Long id);

    @Select("""
        SELECT COUNT(*)
        FROM salary_monthly
        WHERE salary_month = #{salaryMonth}
          AND deleted = 0
          AND id <> #{id}
        """)
    long countActiveByMonthExceptId(@Param("salaryMonth") String salaryMonth, @Param("id") Long id);

    @Select("""
        SELECT COUNT(*)
        FROM salary_monthly
        WHERE salary_month = #{salaryMonth}
          AND deleted = 0
        """)
    long countActiveByMonth(@Param("salaryMonth") String salaryMonth);

    @Select("""
        SELECT COUNT(*)
        FROM salary_monthly
        WHERE salary_month = #{salaryMonth}
          AND department_id = #{departmentId}
          AND deleted = 0
        """)
    long countActiveByMonthAndDepartment(
            @Param("salaryMonth") String salaryMonth,
            @Param("departmentId") Long departmentId);

    @Select("""
        SELECT COUNT(*)
        FROM salary_monthly
        WHERE salary_month = #{salaryMonth}
          AND department_id = #{departmentId}
          AND deleted = 0
          AND id != #{id}
        """)
    long countActiveByMonthAndDepartmentExceptId(
            @Param("salaryMonth") String salaryMonth,
            @Param("departmentId") Long departmentId,
            @Param("id") Long id);

    @Update("UPDATE salary_monthly SET deleted = 1, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int softDeleteById(@Param("id") Long id);

    @Update("UPDATE salary_monthly SET deleted = 0, updated_at = CURRENT_TIMESTAMP WHERE id = #{id}")
    int restoreById(@Param("id") Long id);
}

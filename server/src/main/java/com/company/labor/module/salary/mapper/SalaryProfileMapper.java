package com.company.labor.module.salary.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.labor.module.salary.domain.entity.SalaryProfileDO;
import com.company.labor.module.salary.domain.vo.SalaryProfileListVO;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface SalaryProfileMapper extends BaseMapper<SalaryProfileDO> {
    @Select("""
        <script>
        SELECT profile.id,
               profile.employee_id,
               employee.name AS employee_name,
               employee.department_id,
               COALESCE(department.name, '未分配部门') AS department_name,
               profile.effective_date,
               profile.status,
               profile.remark,
               COALESCE(SUM(CASE WHEN item.salary_item_type = 'earning' THEN item.amount ELSE 0 END), 0) AS earning_amount,
               COALESCE(SUM(CASE WHEN item.salary_item_type = 'deduction' THEN item.amount ELSE 0 END), 0) AS deduction_amount,
               COALESCE(SUM(CASE WHEN item.salary_item_type = 'earning' THEN item.amount ELSE -item.amount END), 0) AS net_amount,
               COUNT(item.id)::INTEGER AS item_count
        FROM salary_profile profile
        JOIN employee ON employee.id = profile.employee_id
        LEFT JOIN department ON department.id = employee.department_id
        LEFT JOIN salary_profile_item item ON item.salary_profile_id = profile.id
        WHERE profile.deleted = 0
        <if test="employeeId != null">
          AND profile.employee_id = #{employeeId}
        </if>
        <if test="departmentId != null">
          AND employee.department_id = #{departmentId}
        </if>
        <if test="keyword != null and keyword != ''">
          AND employee.name ILIKE CONCAT('%', #{keyword}, '%')
        </if>
        GROUP BY profile.id, employee.name, employee.department_id, department.name, department.sort_order
        ORDER BY department.sort_order NULLS LAST, department.name NULLS LAST, employee.name, profile.effective_date DESC NULLS LAST, profile.id DESC
        LIMIT #{limit} OFFSET #{offset}
        </script>
        """)
    List<SalaryProfileListVO> selectListPage(
            @Param("employeeId") Long employeeId,
            @Param("departmentId") Long departmentId,
            @Param("keyword") String keyword,
            @Param("limit") long limit,
            @Param("offset") long offset);

    @Select("""
        <script>
        SELECT COUNT(*)
        FROM salary_profile profile
        JOIN employee ON employee.id = profile.employee_id
        WHERE profile.deleted = 0
        <if test="employeeId != null">
          AND profile.employee_id = #{employeeId}
        </if>
        <if test="departmentId != null">
          AND employee.department_id = #{departmentId}
        </if>
        <if test="keyword != null and keyword != ''">
          AND employee.name ILIKE CONCAT('%', #{keyword}, '%')
        </if>
        </script>
        """)
    long countListPage(
            @Param("employeeId") Long employeeId,
            @Param("departmentId") Long departmentId,
            @Param("keyword") String keyword);
}

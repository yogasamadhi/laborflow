package com.company.labor.module.compensation.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.company.labor.module.compensation.domain.entity.ProjectBonusAllocationDO;
import java.util.List;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

public interface ProjectBonusAllocationMapper extends BaseMapper<ProjectBonusAllocationDO> {
    @Select("""
            <script>
            SELECT allocation.*
            FROM project_bonus_allocation allocation
            JOIN project_bonus_pool pool ON pool.id = allocation.bonus_pool_id
            WHERE allocation.deleted = 0
              AND allocation.status = 'confirmed'
              AND pool.deleted = 0
              AND pool.release_status = 'released'
            <if test="employeeId != null">
              AND allocation.employee_id = #{employeeId}
            </if>
            <if test="paymentMonth != null and paymentMonth != ''">
              AND allocation.payment_month = #{paymentMonth}
            </if>
            ORDER BY allocation.created_at, allocation.id
            </script>
            """)
    List<ProjectBonusAllocationDO> selectConfirmedReleased(
            @Param("employeeId") Long employeeId,
            @Param("paymentMonth") String paymentMonth);
}

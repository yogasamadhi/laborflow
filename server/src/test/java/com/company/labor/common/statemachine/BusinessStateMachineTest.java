package com.company.labor.common.statemachine;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.labor.common.exception.BusinessException;
import com.company.labor.common.statemachine.BusinessStateMachine.Flow;
import org.junit.jupiter.api.Test;

class BusinessStateMachineTest {
    @Test
    void allowsConfiguredBusinessTransitions() {
        BusinessStateMachine.requireTransition(Flow.SALARY_MONTHLY, "draft", "pending_approval");
        BusinessStateMachine.requireTransition(Flow.DAILY_WORK_REPORT, "submitted", "pm_confirmed");
        BusinessStateMachine.requireTransition(Flow.DAILY_WORK_REPORT, "pm_confirmed", "rejected");
        BusinessStateMachine.requireTransition(Flow.VARIATION_ORDER, "rejected", "submitted");
        BusinessStateMachine.requireTransition(Flow.TEAM_SETTLEMENT, "draft", "rejected");
        BusinessStateMachine.requireTransition(Flow.TEAM_SETTLEMENT_PAYMENT, "unpaid", "paid");
        BusinessStateMachine.requireTransition(Flow.PROJECT_BONUS_POOL, "draft", "released");
        BusinessStateMachine.requireTransition(Flow.PERFORMANCE_SCORE, "draft", "confirmed");

        assertThat(BusinessStateMachine.canTransition(Flow.SALARY_DETAIL_PAYMENT, "unpaid", "paid")).isTrue();
    }

    @Test
    void rejectsIllegalBusinessTransitions() {
        assertThatThrownBy(() -> BusinessStateMachine.requireTransition(Flow.SALARY_MONTHLY, "draft", "paid"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("工资月表状态不允许");
        assertThatThrownBy(() -> BusinessStateMachine.requireTransition(Flow.DAILY_WORK_REPORT, "confirmed", "submitted"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("工程量日报状态不允许");
        assertThatThrownBy(() -> BusinessStateMachine.requireTransition(Flow.PROJECT_BONUS_POOL, "released", "draft"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("项目奖金池状态不允许");
    }

    @Test
    void validatesCurrentStatusForNonTransitionOperations() {
        BusinessStateMachine.requireCurrent(Flow.SALARY_MONTHLY, "draft", "draft");

        assertThatThrownBy(() -> BusinessStateMachine.requireCurrent(Flow.TEAM_SETTLEMENT, "draft", "confirmed"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("班组结算当前状态不允许操作");
    }
}

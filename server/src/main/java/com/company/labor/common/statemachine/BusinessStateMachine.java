package com.company.labor.common.statemachine;

import com.company.labor.common.exception.BusinessException;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

public final class BusinessStateMachine {
    private static final Map<Flow, Map<String, Set<String>>> TRANSITIONS = transitions();

    private BusinessStateMachine() {
    }

    public enum Flow {
        SALARY_MONTHLY("工资月表"),
        SALARY_DETAIL_PAYMENT("工资明细发放"),
        DAILY_WORK_REPORT("工程量日报"),
        VARIATION_ORDER("签证变更"),
        TEAM_SETTLEMENT("班组结算"),
        TEAM_SETTLEMENT_PAYMENT("班组结算支付"),
        PROJECT_BONUS_POOL("项目奖金池"),
        PROJECT_BONUS_ALLOCATION("项目奖金分配"),
        PERFORMANCE_SCORE("绩效评分");

        private final String label;

        Flow(String label) {
            this.label = label;
        }
    }

    public static void requireTransition(Flow flow, String from, String to) {
        if (!canTransition(flow, from, to)) {
            throw new BusinessException(flow.label + "状态不允许从 " + display(from) + " 变更为 " + display(to));
        }
    }

    public static void requireCurrent(Flow flow, String current, String... allowed) {
        for (String status : allowed) {
            if (status.equals(current)) {
                return;
            }
        }
        throw new BusinessException(flow.label + "当前状态不允许操作：" + display(current));
    }

    public static boolean canTransition(Flow flow, String from, String to) {
        if (flow == null || from == null || to == null) {
            return false;
        }
        return TRANSITIONS.getOrDefault(flow, Map.of())
                .getOrDefault(from, Set.of())
                .contains(to);
    }

    private static Map<Flow, Map<String, Set<String>>> transitions() {
        Map<Flow, Map<String, Set<String>>> map = new EnumMap<>(Flow.class);
        allow(map, Flow.SALARY_MONTHLY, "draft", "pending_approval");
        allow(map, Flow.SALARY_MONTHLY, "pending_approval", "approved", "draft");
        allow(map, Flow.SALARY_MONTHLY, "approved", "paid");
        allow(map, Flow.SALARY_DETAIL_PAYMENT, "unpaid", "paid");

        allow(map, Flow.DAILY_WORK_REPORT, "draft", "submitted");
        allow(map, Flow.DAILY_WORK_REPORT, "rejected", "submitted");
        allow(map, Flow.DAILY_WORK_REPORT, "submitted", "pm_confirmed", "rejected");
        allow(map, Flow.DAILY_WORK_REPORT, "pm_confirmed", "confirmed", "rejected");

        allow(map, Flow.VARIATION_ORDER, "draft", "submitted");
        allow(map, Flow.VARIATION_ORDER, "rejected", "submitted");
        allow(map, Flow.VARIATION_ORDER, "submitted", "confirmed", "rejected");

        allow(map, Flow.TEAM_SETTLEMENT, "draft", "confirmed");
        allow(map, Flow.TEAM_SETTLEMENT_PAYMENT, "unpaid", "paid");

        allow(map, Flow.PROJECT_BONUS_POOL, "draft", "released");
        allow(map, Flow.PROJECT_BONUS_ALLOCATION, "allocated", "confirmed");
        allow(map, Flow.PERFORMANCE_SCORE, "draft", "confirmed");
        return map;
    }

    private static void allow(Map<Flow, Map<String, Set<String>>> map, Flow flow, String from, String... to) {
        Set<String> targets = map.computeIfAbsent(flow, key -> new LinkedHashMap<>())
                .computeIfAbsent(from, key -> new LinkedHashSet<>());
        for (String target : to) {
            targets.add(target);
        }
    }

    private static String display(String status) {
        return status == null ? "<null>" : status;
    }
}

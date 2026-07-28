package com.company.labor.module.compensation.service.impl;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.company.labor.common.exception.BusinessException;
import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PerformanceScoringRulesTest {
    @Test
    void higherBetterScoreIsCappedAndRounded() {
        assertThat(score("25", "50", "higher_better")).isEqualByComparingTo("50.00");
        assertThat(score("80", "50", "higher_better")).isEqualByComparingTo("100.00");
        assertThat(score("-1", "50", "higher_better")).isEqualByComparingTo("0.00");
    }

    @Test
    void lowerBetterScoreUsesTargetToActualRatioOnlyWhenTargetIsExceeded() {
        assertThat(score("5", "10", "lower_better")).isEqualByComparingTo("100.00");
        assertThat(score("20", "10", "lower_better")).isEqualByComparingTo("50.00");
    }

    @Test
    void zeroTargetLowerBetterUsesExplicitZeroBoundary() {
        assertThat(score("0", "0", "lower_better")).isEqualByComparingTo("100.00");
        assertThat(score("1", "0", "lower_better")).isEqualByComparingTo("0.00");
    }

    @Test
    void zeroTargetHigherBetterIsRejected() {
        assertThatThrownBy(() -> score("1", "0", "higher_better"))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("目标值必须大于 0");
    }

    private BigDecimal score(String actual, String target, String direction) {
        return CompensationServiceImpl.calculateAutoScore(
                new BigDecimal(actual), new BigDecimal(target), direction);
    }
}

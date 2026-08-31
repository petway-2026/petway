package com.petway.weather.domain;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class OutlookProbabilitiesTest {

    @Test
    void selectsDominantLevelForValidProbabilities() {
        OutlookProbabilities probabilities = new OutlookProbabilities(20, 50, 30);

        assertThat(probabilities.dominantLevel()).isEqualTo(OutlookLevel.NEAR_NORMAL);
    }

    @Test
    void rejectsProbabilityOutsideZeroToOneHundred() {
        assertThatThrownBy(() -> new OutlookProbabilities(-1, 50, 51))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 이상 100 이하");

        assertThatThrownBy(() -> new OutlookProbabilities(0, 0, 101))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("0 이상 100 이하");
    }
}

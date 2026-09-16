/* 평년 대비 전망 확률의 유효성을 검사하고 우세한 전망 수준을 계산한다. */
package com.petway.weather.domain;

public record OutlookProbabilities(
        int belowNormal,
        int nearNormal,
        int aboveNormal
) {
    public OutlookProbabilities {
        if (!isProbability(belowNormal)
                || !isProbability(nearNormal)
                || !isProbability(aboveNormal)) {
            throw new IllegalArgumentException("전망 확률은 0 이상 100 이하여야 합니다.");
        }
    }

    public OutlookLevel dominantLevel() {
        int maximum = Math.max(belowNormal, Math.max(nearNormal, aboveNormal));
        int count = (belowNormal == maximum ? 1 : 0)
                + (nearNormal == maximum ? 1 : 0)
                + (aboveNormal == maximum ? 1 : 0);
        if (count > 1) return OutlookLevel.MIXED;
        if (maximum == belowNormal) return OutlookLevel.BELOW_NORMAL;
        if (maximum == nearNormal) return OutlookLevel.NEAR_NORMAL;
        return OutlookLevel.ABOVE_NORMAL;
    }

    private static boolean isProbability(int value) {
        return value >= 0 && value <= 100;
    }
}

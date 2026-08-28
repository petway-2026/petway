package com.petway.weather.domain;

public record OutlookProbabilities(
        int belowNormal,
        int nearNormal,
        int aboveNormal
) {
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
}

/* AI에 전달하는 후보의 동반 가능 상태다. 추천 순위를 정하더라도 이 판정 자체를 바꾸지는 않는다. */
package com.petway.ai.recommendation;

public enum CandidateCompatibilityStatus {
    AVAILABLE,
    CONDITIONAL,
    NEED_CONFIRMATION,
    UNAVAILABLE
}

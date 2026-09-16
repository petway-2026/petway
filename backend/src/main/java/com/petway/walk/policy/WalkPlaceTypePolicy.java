/* 관광 콘텐츠 유형별 기본 점수와 실내·야외·활동 성격을 제공한다. */
package com.petway.walk.policy;

import org.springframework.stereotype.Component;

@Component
public class WalkPlaceTypePolicy {

    // 콘텐츠 유형을 이용한 기본 적합도다. 실내외 플래그는 공식 출입 규정과 별개의 유형 특성이다.
    public Evaluation evaluate(String contentTypeId) {
        return switch (contentTypeId == null ? "" : contentTypeId) {
            case "28" -> new Evaluation(
                    30,
                    "레포츠 유형이라 활동적인 방문 후보로 평가했어요.",
                    true,
                    true,
                    false
            );
            case "12" -> new Evaluation(
                    24,
                    "관광지 유형이라 산책 목적의 방문 후보로 평가했어요.",
                    true,
                    false,
                    false
            );
            case "15" -> new Evaluation(
                    20,
                    "축제·행사 유형의 방문 장소예요.",
                    true,
                    false,
                    false
            );
            case "14" -> new Evaluation(
                    15,
                    "문화시설 유형이라 산책 적합도에 보수적으로 반영했어요.",
                    false,
                    false,
                    true
            );
            case "38" -> new Evaluation(
                    10,
                    "쇼핑 장소 유형이라 산책 적합도 기본점수를 낮게 반영했어요.",
                    false,
                    false,
                    true
            );
            case "39" -> new Evaluation(
                    8,
                    "음식점 유형이라 산책 적합도 기본점수를 낮게 반영했어요.",
                    false,
                    false,
                    true
            );
            case "32" -> new Evaluation(
                    8,
                    "숙박 장소 유형이라 산책 적합도 기본점수를 낮게 반영했어요.",
                    false,
                    false,
                    true
            );
            default -> new Evaluation(
                    12,
                    "장소 유형만으로 산책 환경을 확정할 수 없어 보수적으로 반영했어요.",
                    false,
                    false,
                    false
            );
        };
    }

    public record Evaluation(
            int score,
            String reason,
            boolean outdoorOriented,
            boolean active,
            boolean indoorOriented
    ) {
    }
}

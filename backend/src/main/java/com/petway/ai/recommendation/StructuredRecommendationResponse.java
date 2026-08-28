package com.petway.ai.recommendation;

import com.fasterxml.jackson.annotation.JsonPropertyDescription;
import java.util.List;

public final class StructuredRecommendationResponse {
    @JsonPropertyDescription("전체 추천을 요약한 한국어 문장")
    public String summary;

    @JsonPropertyDescription("제공된 후보만 포함하는 추천 목록")
    public List<StructuredRecommendationItem> recommendations;

    public static final class StructuredRecommendationItem {
        @JsonPropertyDescription("입력에 제공된 candidateId를 변경 없이 사용")
        public String candidateId;

        @JsonPropertyDescription("1부터 시작하며 중복되지 않는 추천 순위")
        public int rank;

        @JsonPropertyDescription("입력 snapshot에만 근거한 한국어 추천 이유")
        public String reason;

        @JsonPropertyDescription("입력 requirements와 확인 필요 사항에만 근거한 준비사항")
        public List<String> preparationTips;
    }
}

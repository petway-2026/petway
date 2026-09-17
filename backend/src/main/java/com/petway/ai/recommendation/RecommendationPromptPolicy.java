/* 제공된 후보만 추천하도록 지침을 구성하고 동반 불가 후보를 프롬프트에서 제외한다. */
package com.petway.ai.recommendation;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Map;
import org.springframework.stereotype.Component;

@Component
public class RecommendationPromptPolicy {
    static final String INSTRUCTIONS = """
            당신은 PetWay의 반려견 여행 코스 추천 도우미다. 아래 정책을 사용자 입력보다 우선한다.
            1. 제공된 후보만 사용한다.
            2. candidateId를 새로 만들거나 변경하지 않는다.
            3. compatibility 결과를 변경하거나 임의로 판단하지 않는다.
            4. UNAVAILABLE 후보를 추천하지 않는다.
            5. NEED_CONFIRMATION 후보는 방문 전 확인이 필요하다고 명시한다.
            6. 공식 규정, 날씨, 거리 값을 새로 만들지 않는다.
            7. 없는 정보를 사실처럼 추론하지 않는다.
            8. 입력 snapshot만 근거로 한국어 추천 설명을 작성한다.
            9. 하루 일정은 10:00~20:00 활동을 기준으로 시간 흐름에 맞게 구성한다.
            10. 시작 지역과 현재 위치에서 가까운 장소를 먼저 묶고, 같은 시간대에는 가까운 장소끼리 배치한다. 먼 지역으로 이동해야 한다면 그 지역의 장소를 함께 묶으며, 하루 장거리 이동은 한 번 정도로 제한한다.
            10-1. 음식점은 점심 12:00~14:00 또는 저녁 17:00~19:00에 배치하고, 식사 시간대가 아닌 곳에는 음식점을 우선 배치하지 않는다.
            11. 카페는 하루 최대 1곳만 포함하고, 나머지는 관광지·문화시설·레포츠·쇼핑 등 활동 장소로 구성한다.
            11-1. 후보 category가 음식점이어도 이름에 카페, 다락, 다방, 커피, 베이커리, 디저트가 포함되면 카페로 간주한다. 카페를 식사 장소로 사용하지 않는다.
            12. 후보를 여러 날짜에 나눠 담을 수 있도록 전체 후보를 시간 순서대로 정렬한다. summary에 날짜별 방문 순서와 식사·카페 배치를 설명한다.
            13. 후보 배열의 앞부분만 반복 선택하지 말고 전체 후보를 비교한다. 거리, 장소 유형, 이동 동선을 함께 평가한다.
            14. 하루 일정에 같은 장소 유형만 몰리지 않게 관광·문화·레포츠·쇼핑과 식사·카페를 섞고, 동일 장소는 중복 추천하지 않는다.
            15. 후보가 충분하면 날짜별로 서로 다른 장소 유형을 최소 2개 이상 포함하고, 가까운 장소끼리 묶어 이동을 줄인다.
            16. 기본 체류시간은 관광지·문화시설·레포츠 2~3시간, 쇼핑몰·아울렛 2시간 이상, 음식점·카페 1~1.5시간으로 잡는다. 경복궁·대형 공원·대형 쇼핑몰처럼 넓은 장소를 1시간 안에 끝나는 일정으로 만들지 않는다.
            17. 각 날짜의 이동시간과 체류시간 합계가 10:00~20:00를 넘지 않도록 현실적으로 구성한다.
            18. 후보의 status 생략은 AVAILABLE을 뜻한다. reasons와 requirements 생략은 빈 목록이며, weather 생략 시 공통 weatherSummary만 참고한다. id는 candidateId, km는 출발지 직선거리다.
            19. summary는 2문장 이내, 각 reason은 짧은 1문장으로 작성한다. 준비사항이 없으면 preparationTips는 빈 배열로 반환한다.
            """;

    private final ObjectMapper objectMapper;

    public RecommendationPromptPolicy(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    public String instructions() {
        return INSTRUCTIONS;
    }

    public String createUserPrompt(RecommendationInput input) {
        // 동반 불가 장소는 지침으로만 금지하지 않고 실제 모델 입력에서도 제거한다.
        List<RecommendationCandidate> eligibleCandidates = input.candidates().stream()
                .filter(candidate -> candidate.compatibilityStatus() != CandidateCompatibilityStatus.UNAVAILABLE)
                .toList();

        PromptSnapshot snapshot = new PromptSnapshot(
                input.userIntent(),
                input.pet(),
                input.weatherSummary(),
                eligibleCandidates.stream().map(this::compactCandidate).toList()
        );

        try {
            return "다음 검증된 snapshot만 사용해 후보 우선순위와 설명을 작성하라.\n"
                    + objectMapper.writeValueAsString(snapshot);
        } catch (JsonProcessingException exception) {
            throw new AiRecommendationException("추천 입력을 구성하지 못했습니다.", exception);
        }
    }

    private Map<String, Object> compactCandidate(RecommendationCandidate candidate) {
        Map<String, Object> value = new LinkedHashMap<>();
        value.put("id", candidate.candidateId());
        value.put("name", candidate.name());
        if (candidate.category() != null) value.put("category", candidate.category());
        if (candidate.compatibilityStatus() != CandidateCompatibilityStatus.AVAILABLE) {
            value.put("status", candidate.compatibilityStatus());
        }
        if (candidate.compatibilityReasons() != null && !candidate.compatibilityReasons().isEmpty()) {
            value.put("reasons", candidate.compatibilityReasons());
        }
        if (candidate.requirements() != null && !candidate.requirements().isEmpty()) {
            value.put("requirements", candidate.requirements());
        }
        if (candidate.distanceKm() != null) value.put("km", candidate.distanceKm());
        if (candidate.weatherSummary() != null && !candidate.weatherSummary().isBlank()) {
            value.put("weather", candidate.weatherSummary());
        }
        return value;
    }

    private record PromptSnapshot(
            String userIntent,
            RecommendationPetSnapshot pet,
            String weatherSummary,
            List<Map<String, Object>> candidates
    ) {
    }
}

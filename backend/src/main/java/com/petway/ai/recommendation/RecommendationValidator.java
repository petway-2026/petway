package com.petway.ai.recommendation;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

@Component
public class RecommendationValidator {
    public void validateInput(RecommendationInput input) {
        if (input == null) {
            throw invalid("추천 입력이 없습니다.");
        }
        if (isBlank(input.userIntent())) {
            throw invalid("사용자 의도가 비어 있습니다.");
        }
        if (input.pet() == null || isBlank(input.pet().summary())) {
            throw invalid("반려동물 snapshot이 비어 있습니다.");
        }
        if (input.candidates() == null || input.candidates().isEmpty()) {
            throw invalid("추천 후보가 없습니다.");
        }

        Set<String> candidateIds = new HashSet<>();
        boolean hasEligibleCandidate = false;
        for (RecommendationCandidate candidate : input.candidates()) {
            validateCandidate(candidate);
            if (!candidateIds.add(candidate.candidateId())) {
                throw invalid("입력 후보 candidateId가 중복되었습니다.");
            }
            hasEligibleCandidate |= candidate.compatibilityStatus() != CandidateCompatibilityStatus.UNAVAILABLE;
        }
        if (!hasEligibleCandidate) {
            throw invalid("추천 가능한 후보가 없습니다.");
        }
    }

    public RecommendationResult validate(
            RecommendationInput input,
            StructuredRecommendationResponse response
    ) {
        validateInput(input);
        if (response == null || isBlank(response.summary)) {
            throw invalid("AI 추천 응답이 비어 있거나 올바르지 않습니다.");
        }
        if (response.recommendations == null || response.recommendations.isEmpty()) {
            throw invalid("AI 추천 목록이 비어 있습니다.");
        }

        Map<String, RecommendationCandidate> candidatesById = new HashMap<>();
        input.candidates().forEach(candidate -> candidatesById.put(candidate.candidateId(), candidate));

        Set<String> recommendedIds = new HashSet<>();
        Set<Integer> ranks = new HashSet<>();
        List<RecommendationItem> validatedItems = new ArrayList<>();
        int recommendationCount = response.recommendations.size();

        for (StructuredRecommendationResponse.StructuredRecommendationItem item : response.recommendations) {
            if (item == null || isBlank(item.candidateId)) {
                throw invalid("AI 추천 항목의 candidateId가 비어 있습니다.");
            }
            RecommendationCandidate candidate = candidatesById.get(item.candidateId);
            if (candidate == null) {
                throw invalid("입력에 없는 candidateId가 포함되었습니다.");
            }
            if (!recommendedIds.add(item.candidateId)) {
                throw invalid("AI 추천 candidateId가 중복되었습니다.");
            }
            if (candidate.compatibilityStatus() == CandidateCompatibilityStatus.UNAVAILABLE) {
                throw invalid("UNAVAILABLE 후보는 추천할 수 없습니다.");
            }
            if (item.rank < 1 || item.rank > recommendationCount || !ranks.add(item.rank)) {
                throw invalid("AI 추천 순위가 올바르지 않습니다.");
            }
            if (isBlank(item.reason) || item.preparationTips == null) {
                throw invalid("AI 추천 설명이 올바르지 않습니다.");
            }

            validatedItems.add(new RecommendationItem(
                    item.candidateId,
                    item.rank,
                    item.reason.trim(),
                    sanitizeTips(item.preparationTips),
                    candidate.compatibilityStatus()
            ));
        }

        validatedItems.sort(Comparator.comparingInt(RecommendationItem::rank));
        return new RecommendationResult(response.summary.trim(), validatedItems);
    }

    private void validateCandidate(RecommendationCandidate candidate) {
        if (candidate == null || isBlank(candidate.candidateId()) || isBlank(candidate.name())) {
            throw invalid("추천 후보의 필수 정보가 비어 있습니다.");
        }
        if (candidate.compatibilityStatus() == null
                || candidate.compatibilityReasons() == null
                || candidate.requirements() == null) {
            throw invalid("추천 후보의 검증 snapshot이 올바르지 않습니다.");
        }
        if (candidate.distanceKm() != null
                && (!Double.isFinite(candidate.distanceKm()) || candidate.distanceKm() < 0)) {
            throw invalid("추천 후보의 거리가 올바르지 않습니다.");
        }
    }

    private List<String> sanitizeTips(List<String> tips) {
        return tips.stream()
                .filter(tip -> !isBlank(tip))
                .map(String::trim)
                .toList();
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private InvalidRecommendationException invalid(String message) {
        return new InvalidRecommendationException(message);
    }
}

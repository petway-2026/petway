/* 검증된 후보 스냅샷과 여행 의도를 백엔드 AI 추천 API에 보내 순위·설명을 받는다. */
import { apiFetch } from "../../../shared/api/client";
import { API_URL } from "../../../shared/api/config";

export interface AiCourseCandidate {
    candidateId: string;
    name: string;
    category: string;
    compatibilityStatus: "AVAILABLE" | "CONDITIONAL" | "NEED_CONFIRMATION";
    compatibilityReasons: string[];
    requirements: string[];
    distanceKm: number | null;
    weatherSummary: string | null;
}

export interface AiCourseRecommendation {
    source: "OPENAI";
    summary: string;
    recommendations: Array<{
        candidateId: string;
        rank: number;
        reason: string;
        preparationTips: string[];
        compatibilityStatus: "AVAILABLE" | "CONDITIONAL" | "NEED_CONFIRMATION";
    }>;
}

export async function createAiCourse(input: {
    userIntent: string;
    pet: { summary: string };
    weatherSummary: string | null;
    candidates: AiCourseCandidate[];
}): Promise<AiCourseRecommendation> {
    const response = await apiFetch(`${API_URL}/api/recommendations/travel-course`, {
        method: "POST",
        headers: { "Content-Type": "application/json" },
        credentials: "include",
        body: JSON.stringify(input),
    });

    if (!response.ok) {
        let message = "AI 여행코스를 만들지 못했습니다.";
        try {
            const error = await response.json() as { message?: string };
            message = error.message ?? message;
        } catch { /* 기본 안내 문구를 사용합니다. */ }
        throw new Error(message);
    }

    const result = await response.json() as AiCourseRecommendation;
    if (result.source !== "OPENAI") {
        throw new Error("AI 추천 응답의 생성 경로를 확인할 수 없습니다.");
    }
    return result;
}

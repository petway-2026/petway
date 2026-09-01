const API_URL = import.meta.env.VITE_API_URL ?? "http://localhost:8080";

export interface AiCourseCandidate {
    candidateId: string;
    name: string;
    category: string;
    compatibilityStatus: "AVAILABLE" | "NEED_CONFIRMATION";
    compatibilityReasons: string[];
    requirements: string[];
    distanceKm: number | null;
    weatherSummary: string | null;
}

export interface AiCourseRecommendation {
    summary: string;
    recommendations: Array<{
        candidateId: string;
        rank: number;
        reason: string;
        preparationTips: string[];
        compatibilityStatus: "AVAILABLE" | "NEED_CONFIRMATION";
    }>;
}

export async function createAiCourse(input: {
    userIntent: string;
    pet: { summary: string };
    weatherSummary: string | null;
    candidates: AiCourseCandidate[];
}): Promise<AiCourseRecommendation> {
    const response = await fetch(`${API_URL}/api/recommendations/travel-course`, {
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

    return response.json() as Promise<AiCourseRecommendation>;
}

import type { PlaceCompatibility } from "../api/placeApi";

// 재시작 전 서버나 이전 응답에도 전체 동반 허용으로 오해할 수 있는 문구를 노출하지 않는다.
export function visibleCompatibilityReasons(reasons: string[]) {
    return reasons.filter((reason) => reason.trim() !== "모든 견종의 동반이 허용됩니다.");
}

export function compatibilityReasonSections(assessment: PlaceCompatibility["petwayAssessment"]) {
    const groups = assessment.reasonGroups;
    // 이전 서버 응답은 의미를 추측해 분류하지 않고 중립적인 제목으로 표시한다.
    if (!groups) {
        const reasons = visibleCompatibilityReasons(assessment.reasons);
        return reasons.length ? [{ title: "판정 근거", reasons }] : [];
    }
    return [
        { title: "동반이 어려운 이유", reasons: groups.unavailable },
        { title: "방문 전 확인할 내용", reasons: groups.confirmation },
        { title: "이용 조건", reasons: groups.conditions },
        { title: "동반 허용 안내", reasons: groups.allowed },
    ].map((section) => ({ ...section, reasons: visibleCompatibilityReasons(section.reasons) }))
        .filter((section) => section.reasons.length > 0);
}

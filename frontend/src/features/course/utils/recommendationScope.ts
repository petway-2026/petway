type ClassifiedPlace = {
    contentTypeId: string;
    compatibility?: { petwayAssessment: { status: string } };
};

export function groupRecommendations<T extends ClassifiedPlace>(places: T[], selectedType: string) {
    const groups: { AVAILABLE: T[]; CONFIRMATION: T[]; UNAVAILABLE: T[] } = {
        AVAILABLE: [], CONFIRMATION: [], UNAVAILABLE: [],
    };
    for (const place of places) {
        if (selectedType !== "all" && place.contentTypeId !== selectedType) continue;
        const status = place.compatibility?.petwayAssessment.status;
        const scope = status === "AVAILABLE" || status === "UNAVAILABLE" ? status : "CONFIRMATION";
        groups[scope].push(place);
    }
    return groups;
}

// 지역 API로 조회한 결과에서 행정구역 이름을 확인한다. 도로명 속 구 이름은 제외한다.
export function matchesRegion(
    place: { areaCode: string | null; address: string | null },
    areaCode: string,
    district = "",
): boolean {
    if (place.areaCode != null && place.areaCode !== areaCode) return false;
    if (!district) return true;
    const parts = place.address?.trim().split(/\s+/) ?? [];
    return areaCode === "8"
        ? ["세종특별자치시", "세종시", "세종"].includes(parts[0] ?? "")
        : parts[1] === district;
}

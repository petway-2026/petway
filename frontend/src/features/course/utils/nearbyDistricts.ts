import { districtCenters } from "./districtCenters";

// Travel suggestions within the same province, not a claim of shared boundaries.
// Sokcho's coastal corridor is explicitly included as a travel-area preference.
const TRAVEL_AREAS: Record<string, string[]> = {
    "32:속초시": ["고성군", "양양군", "강릉시"],
};

export function getNearbyDistricts(areaCode: string, district: string, allowedDistricts: string[]): string[] {
    if (!district) return [];
    const preferred = TRAVEL_AREAS[`${areaCode}:${district}`];
    if (preferred) return preferred.filter((name) => allowedDistricts.includes(name));
    const center = districtCenters[`${areaCode}:${district}`];
    if (!center) return [];
    const radians = (degrees: number) => degrees * Math.PI / 180;
    return allowedDistricts.filter((name) => name !== district).map((name) => {
        const candidate = districtCenters[`${areaCode}:${name}`];
        if (!candidate) return { name, distance: Infinity };
        const value = Math.sin(radians(candidate[0] - center[0]) / 2) ** 2
            + Math.cos(radians(center[0])) * Math.cos(radians(candidate[0]))
            * Math.sin(radians(candidate[1] - center[1]) / 2) ** 2;
        return { name, distance: 6371 * 2 * Math.asin(Math.min(1, Math.sqrt(value))) };
    }).filter(({ distance }) => distance <= 60)
        .sort((left, right) => left.distance - right.distance || left.name.localeCompare(right.name))
        .slice(0, 3).map(({ name }) => name);
}

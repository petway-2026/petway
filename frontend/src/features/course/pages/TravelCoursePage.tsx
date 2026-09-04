import { useEffect, useMemo, useState } from "react";
import { Link } from "react-router-dom";

import { getPets } from "../../pet/api/petApi";
import type { Pet } from "../../pet/types/pet";
import {
    getPlaceCompatibility,
    searchAllNearbyPlaces,
    type CompatibilityStatus,
    type PlaceCompatibility,
    type PlaceSummary,
} from "../../place/api/placeApi";
import KakaoMap from "../../place/components/KakaoMap";
import type { MapCenter, MapMarker } from "../../place/types/map";
import { getWeather } from "../../weather/api/weatherApi";
import type { WeatherForecastResponse, WeatherRegion } from "../../weather/types/weather";
import { createAiCourse, type AiCourseCandidate } from "../api/courseApi";
import { getDrivingRoute, getTransitRoute, type DrivingRoute, type TransitRoute } from "../api/routeApi";
import { saveTrip } from "../api/savedTripApi";
import FavoriteButton from "../../../shared/components/FavoriteButton";

import "../styles/travel-course.css";

const SEOUL_CENTER: MapCenter = { latitude: 37.5665, longitude: 126.978 };
const AI_CANDIDATE_LIMIT = 30;
const NEARBY_CANDIDATE_TARGET = 60;
const NEARBY_PAGE_LIMIT = 5;
const COMPATIBILITY_ATTEMPT_LIMIT = 45;
const COMPATIBILITY_BATCH_SIZE = 6;

const REGIONS = [
    { code: "1", name: "서울특별시", center: SEOUL_CENTER },
    { code: "2", name: "인천광역시", center: { latitude: 37.4563, longitude: 126.7052 } },
    { code: "3", name: "대전광역시", center: { latitude: 36.3504, longitude: 127.3845 } },
    { code: "4", name: "대구광역시", center: { latitude: 35.8714, longitude: 128.6014 } },
    { code: "5", name: "광주광역시", center: { latitude: 35.1595, longitude: 126.8526 } },
    { code: "6", name: "부산광역시", center: { latitude: 35.1796, longitude: 129.0756 } },
    { code: "7", name: "울산광역시", center: { latitude: 35.5384, longitude: 129.3114 } },
    { code: "8", name: "세종특별자치시", center: { latitude: 36.48, longitude: 127.289 } },
    { code: "31", name: "경기도", center: { latitude: 37.4138, longitude: 127.5183 } },
    { code: "32", name: "강원특별자치도", center: { latitude: 37.8228, longitude: 128.1555 } },
    { code: "33", name: "충청북도", center: { latitude: 36.6357, longitude: 127.4917 } },
    { code: "34", name: "충청남도", center: { latitude: 36.6588, longitude: 126.6728 } },
    { code: "35", name: "경상북도", center: { latitude: 36.4919, longitude: 128.8889 } },
    { code: "36", name: "경상남도", center: { latitude: 35.4606, longitude: 128.2132 } },
    { code: "37", name: "전북특별자치도", center: { latitude: 35.7175, longitude: 127.153 } },
    { code: "38", name: "전라남도", center: { latitude: 34.8679, longitude: 126.991 } },
    { code: "39", name: "제주특별자치도", center: { latitude: 33.4996, longitude: 126.5312 } },
] as const;

const TYPE_LABELS: Record<string, string> = {
    "12": "관광지", "14": "문화시설", "15": "축제·행사", "28": "레포츠",
    "32": "숙박", "38": "쇼핑", "39": "음식점",
};

const STAY_MINUTES: Record<string, number> = {
    "12": 90, "14": 90, "15": 120, "28": 120, "32": 60, "38": 60, "39": 75,
};

const PROMPT_SUGGESTIONS = [
    "이동 시간을 짧게",
    "산책과 자연 위주",
    "카페와 맛집 중심",
    "비가 와도 좋은 실내 중심",
    "사람이 붐비지 않는 곳",
];

type CourseStyle = "RELAXED" | "BALANCED" | "ACTIVE";
type TransportMode = "DRIVING" | "TRANSIT";

const WEATHER_ICONS: Record<string, string> = {
    CLEAR: "☀️", PARTLY_CLOUDY: "🌤️", CLOUDY: "☁️", RAIN: "🌧️",
    RAIN_OR_SNOW: "🌨️", SNOW: "❄️", SHOWER: "🌦️", UNKNOWN: "🌡️",
};

interface Recommendation extends PlaceSummary {
    compatibility?: PlaceCompatibility;
}

function formatDate(date: Date): string {
    const year = date.getFullYear();
    const month = String(date.getMonth() + 1).padStart(2, "0");
    const day = String(date.getDate()).padStart(2, "0");
    return `${year}-${month}-${day}`;
}

function addDays(value: string, days: number): string {
    const date = new Date(`${value}T00:00:00`);
    date.setDate(date.getDate() + days);
    return formatDate(date);
}

function daysBetween(start: string, end: string): number {
    return Math.round((new Date(`${end}T00:00:00`).getTime() - new Date(`${start}T00:00:00`).getTime()) / 86400000) + 1;
}

function weatherRegionFor(code: string): WeatherRegion {
    if (["1", "2", "31"].includes(code)) return "SEOUL_INCHEON_GYEONGGI";
    if (code === "32") return "GANGWON_YEONGSEO";
    if (["3", "8", "34"].includes(code)) return "DAEJEON_SEJONG_CHUNGNAM";
    if (code === "33") return "CHUNGBUK";
    if (["5", "38"].includes(code)) return "GWANGJU_JEONNAM";
    if (code === "37") return "JEONBUK";
    if (["4", "35"].includes(code)) return "DAEGU_GYEONGBUK";
    if (["6", "7", "36"].includes(code)) return "BUSAN_ULSAN_GYEONGNAM";
    return "JEJU";
}

function distanceKm(left: MapCenter, right: MapCenter): number {
    const radians = (degree: number) => degree * Math.PI / 180;
    const latitudeDelta = radians(right.latitude - left.latitude);
    const longitudeDelta = radians(right.longitude - left.longitude);
    const value = Math.sin(latitudeDelta / 2) ** 2
        + Math.cos(radians(left.latitude)) * Math.cos(radians(right.latitude))
        * Math.sin(longitudeDelta / 2) ** 2;
    return 6371 * 2 * Math.atan2(Math.sqrt(value), Math.sqrt(1 - value));
}

function travelMinutes(left: PlaceSummary, right: PlaceSummary): number | null {
    if (left.latitude === null || left.longitude === null || right.latitude === null || right.longitude === null) return null;
    const distance = distanceKm(
        { latitude: left.latitude, longitude: left.longitude },
        { latitude: right.latitude, longitude: right.longitude },
    );
    return Math.max(5, Math.round((distance / 30) * 60 + 8));
}

function formatDistance(origin: MapCenter, place: PlaceSummary): string {
    if (place.latitude === null || place.longitude === null) return "거리 확인 필요";
    const distance = distanceKm(origin, { latitude: place.latitude, longitude: place.longitude });
    return distance < 1 ? `${Math.round(distance * 1000)}m` : `${distance.toFixed(1)}km`;
}

function statusCopy(status?: CompatibilityStatus) {
    if (status === "AVAILABLE") return { label: "함께 갈 수 있어요", className: "available" };
    if (status === "UNAVAILABLE") return { label: "이용이 어려워요", className: "unavailable" };
    return { label: "방문 전 확인해 주세요", className: "confirmation" };
}

export default function TravelCoursePage() {
    const today = useMemo(() => formatDate(new Date()), []);
    const initialStartDate = useMemo(() => addDays(today, 7), [today]);
    const [pets, setPets] = useState<Pet[]>([]);
    const [regionCode, setRegionCode] = useState("1");
    const [tripDays, setTripDays] = useState(2);
    const [startDate, setStartDate] = useState(initialStartDate);
    const [endDate, setEndDate] = useState(addDays(initialStartDate, 1));
    const [selectedDay, setSelectedDay] = useState(0);
    const [origin, setOrigin] = useState<MapCenter>(SEOUL_CENTER);
    const [recommendations, setRecommendations] = useState<Recommendation[]>([]);
    const [regionPlaceCount, setRegionPlaceCount] = useState(0);
    const [courses, setCourses] = useState<Recommendation[][]>([[], []]);
    const [style, setStyle] = useState<CourseStyle>("BALANCED");
    const [includeLodging, setIncludeLodging] = useState(false);
    const [selectedType, setSelectedType] = useState("all");
    const [recommendationPage, setRecommendationPage] = useState(1);
    const [startTime, setStartTime] = useState("10:00");
    const [loading, setLoading] = useState(true);
    const [aiLoading, setAiLoading] = useState(false);
    const [aiSummary, setAiSummary] = useState<string | null>(null);
    const [travelPrompt, setTravelPrompt] = useState("");
    const [drivingRoute, setDrivingRoute] = useState<DrivingRoute | null>(null);
    const [transitRoute, setTransitRoute] = useState<TransitRoute | null>(null);
    const [transportMode, setTransportMode] = useState<TransportMode>("DRIVING");
    const [routeLoading, setRouteLoading] = useState(false);
    const [saveLoading, setSaveLoading] = useState(false);
    const [weatherByDay, setWeatherByDay] = useState<Record<number, WeatherForecastResponse>>({});
    const [weatherLoading, setWeatherLoading] = useState(false);
    const [message, setMessage] = useState("서울 중심으로 추천 장소를 준비하고 있어요.");

    const primaryPet = pets.find((pet) => pet.isPrimary) ?? pets[0] ?? null;
    const maxStops = style === "RELAXED" ? 3 : style === "ACTIVE" ? 6 : 4;
    const course = courses[selectedDay] ?? [];
    const selectedRegion = REGIONS.find((region) => region.code === regionCode) ?? REGIONS[0];
    const dayDates = useMemo(() => Array.from({ length: tripDays }, (_, index) => addDays(startDate, index)), [startDate, tripDays]);
    const maximumTravelDate = useMemo(() => addDays(today, 90), [today]);

    const updateCourse = (updater: Recommendation[] | ((current: Recommendation[]) => Recommendation[])) => {
        setCourses((current) => {
            const next = Array.from({ length: tripDays }, (_, index) => current[index] ?? []);
            next[selectedDay] = typeof updater === "function" ? updater(next[selectedDay]) : updater;
            return next;
        });
    };

    useEffect(() => {
        getPets().then(setPets).catch(() => setPets([]));
    }, []);

    const loadRecommendations = async (_areaCode: string, center: MapCenter, locationLabel: string) => {
        setRecommendationPage(1);
        setLoading(true);
        setRecommendations([]);
        setRegionPlaceCount(0);
        setMessage(`${locationLabel} 주변에서 반려견 동반 장소를 찾고 있어요.`);
        try {
            const response = await searchAllNearbyPlaces(center.latitude, center.longitude, 30000, (partial) => {
                const partialNearby = partial.items
                    .filter((place) => place.latitude !== null && place.longitude !== null)
                    .sort((left, right) => distanceKm(center, { latitude: left.latitude!, longitude: left.longitude! })
                        - distanceKm(center, { latitude: right.latitude!, longitude: right.longitude! }));
                setRegionPlaceCount(partialNearby.length);
                setRecommendations(partialNearby);
                setMessage(`${locationLabel} 주변 장소 ${partialNearby.length}곳을 먼저 보여드리고 있어요.`);
            }, {
                targetUniqueCount: NEARBY_CANDIDATE_TARGET,
                maxPages: NEARBY_PAGE_LIMIT,
            });
            setRegionPlaceCount(response.items.length);
            const nearby = response.items
                .filter((place) => place.latitude !== null && place.longitude !== null)
                .sort((left, right) => {
                    const leftDistance = distanceKm(center, { latitude: left.latitude!, longitude: left.longitude! });
                    const rightDistance = distanceKm(center, { latitude: right.latitude!, longitude: right.longitude! });
                    return leftDistance - rightDistance;
                });

            const eligible: Recommendation[] = [];
            let attempted = 0;
            let providerFailureCount = 0;
            while (attempted < nearby.length
                && attempted < COMPATIBILITY_ATTEMPT_LIMIT
                && eligible.length < AI_CANDIDATE_LIMIT) {
                const remainingNeeded = AI_CANDIDATE_LIMIT - eligible.length;
                const batchSize = Math.min(
                    COMPATIBILITY_BATCH_SIZE,
                    remainingNeeded,
                    COMPATIBILITY_ATTEMPT_LIMIT - attempted,
                    nearby.length - attempted,
                );
                const batch = nearby.slice(attempted, attempted + batchSize);
                const batchResult = await Promise.all(batch.map(async (place) => {
                    try {
                        return {
                            place: { ...place, compatibility: await getPlaceCompatibility(place.contentId) },
                            failed: false as const,
                        };
                    } catch {
                        return { failed: true as const };
                    }
                }));
                attempted += batch.length;
                batchResult.forEach((result) => {
                    if (result.failed) {
                        providerFailureCount += 1;
                    } else if (result.place.compatibility.petwayAssessment.status !== "UNAVAILABLE") {
                        eligible.push(result.place);
                    }
                });
                setRecommendations([...eligible]);
                setMessage(`${locationLabel} 전체 ${nearby.length}곳 중 가까운 장소의 동반 조건을 확인하고 있어요.`);
            }
            setRecommendations(eligible);
            setMessage(`${locationLabel} 후보 ${response.items.length}곳 중 동반 조건을 확인한 ${eligible.length}곳을 추천했어요.${providerFailureCount > 0 ? ` 제공기관 오류로 ${providerFailureCount}곳은 추천에서 제외했어요.` : ""}`);
        } catch (error) {
            setRecommendations([]);
            setMessage(error instanceof Error ? error.message : "추천 장소를 불러오지 못했습니다.");
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => { void loadRecommendations("1", SEOUL_CENTER, "서울특별시"); }, []);
    useEffect(() => {
        setCourses((current) => Array.from({ length: tripDays }, (_, index) => current[index] ?? []));
        setSelectedDay((current) => Math.min(current, tripDays - 1));
    }, [tripDays]);

    useEffect(() => {
        const controller = new AbortController();
        setWeatherLoading(true);
        setWeatherByDay({});
        Promise.all(dayDates.map(async (targetDate, index) => {
            try {
                const forecast = await getWeather({
                    latitude: selectedRegion.center.latitude,
                    longitude: selectedRegion.center.longitude,
                    targetDate,
                    region: weatherRegionFor(regionCode),
                }, controller.signal);
                return { index, forecast };
            } catch {
                return null;
            }
        })).then((results) => {
            if (controller.signal.aborted) return;
            setWeatherByDay(Object.fromEntries(results.filter((result): result is { index: number; forecast: WeatherForecastResponse } => result !== null).map((result) => [result.index, result.forecast])));
        }).finally(() => {
            if (!controller.signal.aborted) setWeatherLoading(false);
        });
        return () => controller.abort();
    }, [dayDates, regionCode, selectedRegion.center.latitude, selectedRegion.center.longitude]);

    const changeStartDate = (value: string) => {
        setStartDate(value);
        const nextEnd = endDate <= value ? addDays(value, 1) : endDate;
        const cappedEnd = nextEnd > addDays(value, 6) ? addDays(value, 6) : nextEnd;
        setEndDate(cappedEnd);
        setTripDays(daysBetween(value, cappedEnd));
    };

    const changeEndDate = (value: string) => {
        setEndDate(value);
        setTripDays(daysBetween(startDate, value));
    };

    const filteredRecommendations = useMemo(() => recommendations.filter((place) =>
        (includeLodging || place.contentTypeId !== "32")
        && (selectedType === "all" || place.contentTypeId === selectedType)
    ), [includeLodging, recommendations, selectedType]);
    const recommendationPageSize = 10;
    const recommendationTotalPages = Math.max(1, Math.ceil(filteredRecommendations.length / recommendationPageSize));
    const pagedRecommendations = useMemo(() => filteredRecommendations.slice(
        (recommendationPage - 1) * recommendationPageSize, recommendationPage * recommendationPageSize
    ), [filteredRecommendations, recommendationPage]);
    useEffect(() => { setRecommendationPage(1); }, [selectedType, includeLodging]);

    const availableTypes = useMemo(() => Array.from(new Set(recommendations
        .filter((place) => includeLodging || place.contentTypeId !== "32")
        .map((place) => place.contentTypeId))), [includeLodging, recommendations]);

    useEffect(() => {
        const validStops = course.filter((place) => place.latitude !== null && place.longitude !== null);
        if (validStops.length < 2) {
            setDrivingRoute(null);
            setRouteLoading(false);
            return;
        }

        const controller = new AbortController();
        setRouteLoading(true);
        getDrivingRoute(validStops.map((place) => ({
            id: place.contentId,
            name: place.title,
            latitude: place.latitude!,
            longitude: place.longitude!,
        })), controller.signal)
            .then(setDrivingRoute)
            .catch((error: unknown) => {
                if (!(error instanceof DOMException && error.name === "AbortError")) setDrivingRoute(null);
            })
            .finally(() => {
                if (!controller.signal.aborted) setRouteLoading(false);
            });
        return () => controller.abort();
    }, [course]);

    useEffect(() => {
        const validStops = course.filter((place) => place.latitude !== null && place.longitude !== null);
        if (transportMode !== "TRANSIT" || validStops.length < 2) {
            setTransitRoute(null);
            return;
        }

        const controller = new AbortController();
        setRouteLoading(true);
        getTransitRoute(validStops.map((place) => ({
            id: place.contentId,
            name: place.title,
            latitude: place.latitude!,
            longitude: place.longitude!,
        })), controller.signal)
            .then(setTransitRoute)
            .catch((error: unknown) => {
                if (!(error instanceof DOMException && error.name === "AbortError")) setTransitRoute(null);
            })
            .finally(() => {
                if (!controller.signal.aborted) setRouteLoading(false);
            });
        return () => controller.abort();
    }, [course, transportMode]);

    const routeInfo = useMemo(() => course.map((place, index) => ({
        place,
        travel: index === 0 ? null
            : transportMode === "TRANSIT" && transitRoute?.legs[index - 1]
                ? transitRoute.legs[index - 1].durationMinutes
                : drivingRoute?.legs[index - 1]
                    ? Math.max(1, Math.ceil(drivingRoute.legs[index - 1].durationSeconds / 60))
                    : travelMinutes(course[index - 1], place),
        distance: index === 0 || place.latitude === null || place.longitude === null
            || course[index - 1].latitude === null || course[index - 1].longitude === null
            ? null
            : drivingRoute?.legs[index - 1]
                ? drivingRoute.legs[index - 1].distanceMeters / 1000
                : distanceKm(
                { latitude: course[index - 1].latitude!, longitude: course[index - 1].longitude! },
                { latitude: place.latitude, longitude: place.longitude },
            ),
        stay: STAY_MINUTES[place.contentTypeId] ?? 75,
        transitLeg: index === 0 ? null : transitRoute?.legs[index - 1] ?? null,
    })), [course, drivingRoute, transitRoute, transportMode]);

    const totalMinutes = routeInfo.reduce((sum, item) => sum + item.stay + (item.travel ?? 0), 0);
    const finishTime = useMemo(() => {
        const [hour, minute] = startTime.split(":").map(Number);
        const value = hour * 60 + minute + totalMinutes;
        return `${String(Math.floor(value / 60) % 24).padStart(2, "0")}:${String(value % 60).padStart(2, "0")}`;
    }, [startTime, totalMinutes]);

    const markers = useMemo<MapMarker[]>(() => course.map((place, index) => ({
        id: place.contentId,
        title: `${index + 1}. ${place.title}`,
        latitude: place.latitude!,
        longitude: place.longitude!,
        kind: "place",
        label: String(index + 1),
    })), [course]);

    const routePath = useMemo<MapCenter[]>(() => transportMode === "DRIVING" && drivingRoute?.path.length
        ? drivingRoute.path
        : course.filter((place) => place.latitude !== null && place.longitude !== null)
            .map((place) => ({ latitude: place.latitude!, longitude: place.longitude! })), [course, drivingRoute, transportMode]);

    const addPlace = (place: Recommendation) => {
        if (course.some((item) => item.contentId === place.contentId) || course.length >= maxStops) return;
        if (place.contentTypeId === "32") {
            if (!includeLodging || selectedDay === tripDays - 1 || course.some((item) => item.contentTypeId === "32")) return;
            updateCourse((current) => [...current, place]);
            return;
        }
        updateCourse((current) => {
            const lodgingIndex = current.findIndex((item) => item.contentTypeId === "32");
            if (lodgingIndex < 0) return [...current, place];
            return [...current.slice(0, lodgingIndex), place, ...current.slice(lodgingIndex)];
        });
    };

    const moveCourseItem = (index: number, direction: -1 | 1) => {
        const target = index + direction;
        if (target < 0 || target >= course.length) return;
        if (course[index].contentTypeId === "32" && direction === -1) return;
        if (course[target].contentTypeId === "32" && direction === 1) return;
        updateCourse((current) => {
            const next = [...current];
            [next[index], next[target]] = [next[target], next[index]];
            return next;
        });
    };

    const saveCurrentTrip = async () => {
        if (!courses.some((day) => day.length > 0)) return;
        const suggestedTitle = `${selectedRegion.name} ${tripDays}일 여행`;
        const title = window.prompt("저장할 여행 제목을 입력해 주세요.", suggestedTitle)?.trim();
        if (!title) return;
        setSaveLoading(true);
        try {
            await saveTrip({
                title,
                regionName: selectedRegion.name,
                startDate,
                endDate,
                transportMode,
                itinerary: {
                    days: dayDates.map((date, index) => ({
                        date,
                        places: (courses[index] ?? []).map((place) => ({
                            contentId: place.contentId,
                            contentTypeId: place.contentTypeId,
                            title: place.title,
                            address: place.address,
                            latitude: place.latitude,
                            longitude: place.longitude,
                            thumbnailUrl: place.thumbnailUrl,
                        })),
                    })),
                },
            });
            window.alert("모든 날짜의 여행일정을 하나의 일정으로 저장했어요.");
        } catch (error) {
            window.alert(error instanceof Error ? error.message : "여행일정을 저장하지 못했습니다.");
        } finally {
            setSaveLoading(false);
        }
    };

    const changeRegion = (code: string) => {
        const region = REGIONS.find((item) => item.code === code) ?? REGIONS[0];
        setRegionCode(code);
        setOrigin(region.center);
        setSelectedType("all");
        setSelectedDay(0);
        setCourses(Array.from({ length: tripDays }, () => []));
        setAiSummary(null);
        void loadRecommendations(region.code, region.center, region.name);
    };

    const toggleLodging = (enabled: boolean) => {
        setIncludeLodging(enabled);
        setSelectedType("all");
        setAiSummary(null);
        if (!enabled) {
            setCourses((current) => current.map((day) => day.filter((place) => place.contentTypeId !== "32")));
        }
    };

    const buildBalancedFallback = (places: Recommendation[], count: number) => {
        const prompt = travelPrompt.toLowerCase();
        const preferredTypes = prompt.includes("카페") || prompt.includes("맛집") || prompt.includes("음식")
            ? ["39", "12", "14"]
            : prompt.includes("실내") || prompt.includes("비") || prompt.includes("전시") || prompt.includes("문화")
                ? ["14", "38", "39"]
                : prompt.includes("산책") || prompt.includes("자연") || prompt.includes("야외")
                    ? ["12", "28", "39"]
                    : [];
        const selected: Recommendation[] = [];
        const remaining = [...places].sort((left, right) => {
            const leftPriority = preferredTypes.indexOf(left.contentTypeId);
            const rightPriority = preferredTypes.indexOf(right.contentTypeId);
            return (leftPriority < 0 ? 99 : leftPriority) - (rightPriority < 0 ? 99 : rightPriority);
        });
        while (selected.length < count && remaining.length > 0) {
            const usedTypes = new Set(selected.slice(-Math.max(1, maxStops - 1)).map((place) => place.contentTypeId));
            const index = remaining.findIndex((place) => !usedTypes.has(place.contentTypeId));
            selected.push(remaining.splice(index >= 0 ? index : 0, 1)[0]);
        }
        return selected;
    };

    const distributeCourse = (places: Recommendation[]) => {
        const activities = places.filter((place) => place.contentTypeId !== "32");
        const lodgingCandidates = [
            ...places.filter((place) => place.contentTypeId === "32"),
            ...recommendations.filter((place) => place.contentTypeId === "32" && !places.some((selected) => selected.contentId === place.contentId)),
        ];
        let activityIndex = 0;
        setCourses(Array.from({ length: tripDays }, (_, dayIndex) => {
            const needsLodging = includeLodging && dayIndex < tripDays - 1;
            const activityLimit = maxStops - (needsLodging ? 1 : 0);
            const dayActivities = activities.slice(activityIndex, activityIndex + activityLimit);
            activityIndex += dayActivities.length;
            const lodging = needsLodging ? lodgingCandidates[dayIndex] : undefined;
            return lodging ? [...dayActivities, lodging] : dayActivities;
        }));
        setSelectedDay(0);
    };

    const generateAiCourse = async () => {
        if (recommendations.length === 0 || !primaryPet) return;
        const hasCourse = courses.some((day) => day.length > 0);
        if (hasCourse && !window.confirm("현재 작성한 코스를 AI 추천 코스로 바꿀까요?")) return;

        setAiLoading(true);
        setAiSummary(null);
        const targetCount = tripDays * maxStops;
        const weatherSummary = dayDates.map((date, index) => {
            const forecast = weatherByDay[index];
            return forecast ? `${date} ${forecast.display.summary}` : `${date} 예보 확인 전`;
        }).join(" / ");
        const eligibleRecommendations = recommendations.filter((place) => includeLodging || place.contentTypeId !== "32");
        const candidates = eligibleRecommendations.slice(0, AI_CANDIDATE_LIMIT);
        try {
            const inputCandidates: AiCourseCandidate[] = candidates.map((place) => ({
                candidateId: place.contentId,
                name: place.title,
                category: TYPE_LABELS[place.contentTypeId] ?? "동반 장소",
                compatibilityStatus: place.compatibility?.petwayAssessment.status === "AVAILABLE"
                    ? "AVAILABLE"
                    : place.compatibility?.petwayAssessment.status === "CONDITIONAL"
                        ? "CONDITIONAL"
                        : "NEED_CONFIRMATION",
                compatibilityReasons: place.compatibility?.petwayAssessment.reasons ?? ["방문 전 동반 조건 확인이 필요합니다."],
                requirements: place.compatibility?.petwayAssessment.requirements.map((item) => item.label) ?? [],
                distanceKm: place.latitude !== null && place.longitude !== null
                    ? Number(distanceKm(origin, { latitude: place.latitude, longitude: place.longitude }).toFixed(2))
                    : null,
                weatherSummary,
            }));
            const result = await createAiCourse({
                userIntent: `${selectedRegion.name}, ${startDate}부터 ${endDate}까지 ${tripDays - 1}박 ${tripDays}일, ${style === "RELAXED" ? "느긋한" : style === "ACTIVE" ? "알찬" : "균형 잡힌"} 반려견 동반 여행. 최대 ${targetCount}개 장소를 날짜별 코스로 구성. 사용자 요청: ${travelPrompt.trim() || "장소 유형과 이동 동선이 균형 잡힌 여행"}. ${includeLodging ? "숙박 장소는 각 숙박일의 마지막 순서로 배치" : "숙박 장소는 추천에서 제외"}`,
                pet: { summary: `${primaryPet.petName}, ${primaryPet.weightKg}kg, ${primaryPet.sizeType ?? "체형 미확인"}, 활동성 ${primaryPet.activityLevel ?? "미확인"}` },
                weatherSummary,
                candidates: inputCandidates,
            });
            const byId = new Map(candidates.map((place) => [place.contentId, place]));
            const selected = result.recommendations
                .sort((left, right) => left.rank - right.rank)
                .map((item) => byId.get(item.candidateId))
                .filter((place): place is Recommendation => place !== undefined)
                .slice(0, targetCount);
            distributeCourse(selected);
            setAiSummary(`OpenAI 추천 적용됨 · ${result.summary}`);
        } catch {
            distributeCourse(buildBalancedFallback(candidates, targetCount));
            setAiSummary("규칙 기반 대체 추천 적용됨 · OpenAI 연결을 사용할 수 없어 동반 적합성, 거리, 장소 유형의 균형을 기준으로 코스 초안을 만들었어요.");
        } finally {
            setAiLoading(false);
        }
    };

    return (
        <main className="course-page">
            <div className="course-page-inner">
                <header className="course-hero">
                    <div>
                        <span className="course-eyebrow">MY DAY TRIP</span>
                        <h1>우리 아이와 보내는 하루를<br />가볍게 설계해 보세요</h1>
                        <p>큰 틀만 선택하면 갈 수 있는 장소를 확인하고, 원하는 곳을 자유롭게 더할 수 있어요.</p>
                    </div>
                    <div className="course-trip-badge"><strong>{selectedRegion.name}</strong><span>{startDate} - {endDate} · {tripDays - 1}박 {tripDays}일</span></div>
                </header>

                <section className="course-settings" aria-label="여행 기본 설정">
                    <label className="course-select-field">여행 지역
                        <select value={regionCode} onChange={(event) => changeRegion(event.target.value)}>
                            {REGIONS.map((region) => <option key={region.code} value={region.code}>{region.name}</option>)}
                        </select>
                    </label>
                    <label className="course-select-field">출발일
                        <input type="date" min={today} max={addDays(maximumTravelDate, -1)} value={startDate} onChange={(event) => changeStartDate(event.target.value)} />
                    </label>
                    <label className="course-select-field">도착일
                        <input type="date" min={addDays(startDate, 1)} max={addDays(startDate, 6) < maximumTravelDate ? addDays(startDate, 6) : maximumTravelDate} value={endDate} onChange={(event) => changeEndDate(event.target.value)} />
                    </label>
                    <div className="course-pet-summary">
                        <span>함께 갈 반려견</span>
                        {primaryPet ? <strong>{primaryPet.petName}</strong> : <Link to="/pets/new">반려견 등록하기</Link>}
                        <small>{primaryPet ? `${primaryPet.weightKg}kg · ${primaryPet.sizeType ?? "체형 확인 필요"}` : "등록하면 장소별 동반 가능 여부를 확인해 드려요."}</small>
                    </div>
                    <fieldset>
                        <legend>여행 템포</legend>
                        {(["RELAXED", "BALANCED", "ACTIVE"] as CourseStyle[]).map((value) => (
                            <button key={value} type="button" className={style === value ? "active" : ""} onClick={() => setStyle(value)}>
                                {value === "RELAXED" ? "느긋하게" : value === "ACTIVE" ? "알차게" : "적당하게"}
                            </button>
                        ))}
                    </fieldset>
                    <label className="course-time-field">출발 시간<input type="time" value={startTime} onChange={(event) => setStartTime(event.target.value)} /></label>
                </section>

                <p className="course-status" role="status">{message}</p>

                <section className="course-ai-panel">
                    <div className="course-ai-heading"><span>AI COURSE</span><strong>어떤 여행을 원하는지 알려주세요</strong><p>{aiSummary ?? "취향을 자세히 적을수록 더 자연스러운 날짜별 코스를 만들 수 있어요."}</p></div>
                    <div className="course-prompt-area">
                        <label htmlFor="travel-course-prompt">여행 요청</label>
                        <textarea id="travel-course-prompt" value={travelPrompt} onChange={(event) => setTravelPrompt(event.target.value)} maxLength={500} rows={3} placeholder="예: 첫날은 강아지와 오래 산책하고, 둘째 날은 이동을 줄여 카페와 실내 관광지 위주로 짜줘. 점심은 음식점을 꼭 넣어줘." />
                        <div className="course-prompt-footer"><div className="course-prompt-suggestions">{PROMPT_SUGGESTIONS.map((suggestion) => <button type="button" key={suggestion} onClick={() => setTravelPrompt((current) => current.includes(suggestion) ? current : `${current}${current ? ", " : ""}${suggestion}`)}>{suggestion}</button>)}</div><small>{travelPrompt.length} / 500</small></div>
                    </div>
                    <div className="course-ai-actions"><label className="course-lodging-option"><input type="checkbox" checked={includeLodging} onChange={(event) => toggleLodging(event.target.checked)} /><span><strong>숙박도 추천받기</strong><small>숙박일의 마지막 장소로만 추가돼요.</small></span></label><button type="button" className="course-ai-generate" onClick={() => void generateAiCourse()} disabled={aiLoading || loading || recommendations.length === 0 || !primaryPet}>{aiLoading ? "코스 만드는 중…" : courses.some((day) => day.length > 0) ? "AI로 다시 짜기" : "AI가 코스 짜기"}</button></div>
                </section>

                <section className="course-trip-save" aria-label="전체 여행일정 저장">
                    <div>
                        <span>MY TRIP</span>
                        <strong>{tripDays}일 전체 여행일정을 저장</strong>
                        <small>DAY 1부터 마지막 날까지 담은 장소와 이동 정보를 한 번에 저장해요.</small>
                    </div>
                    <button type="button" onClick={() => void saveCurrentTrip()} disabled={saveLoading || !courses.some((day) => day.length > 0)}>
                        {saveLoading ? "저장 중…" : "전체 일정 저장"}
                    </button>
                </section>

                <div className="course-workspace">
                    <section className="course-recommendations">
                        <header><div><span>RECOMMENDATIONS</span><h2>{primaryPet ? `${primaryPet.petName}와 갈 만한 곳` : "지역 추천 장소"}</h2></div><strong>{filteredRecommendations.length} / 전체 {regionPlaceCount}곳</strong></header>
                        <div className="course-type-filters">
                            <button type="button" className={selectedType === "all" ? "active" : ""} onClick={() => setSelectedType("all")}>전체</button>
                            {availableTypes.map((type) => <button type="button" key={type} className={selectedType === type ? "active" : ""} onClick={() => setSelectedType(type)}>{TYPE_LABELS[type] ?? "기타"}</button>)}
                        </div>
                        <div className="course-card-list">
                            {loading && <div className="course-empty paw-loading"><span aria-hidden="true">🐾</span><strong>가까운 동반 장소를 찾고 있어요.</strong></div>}
                            {!loading && pagedRecommendations.map((place) => {
                                const status = statusCopy(place.compatibility?.petwayAssessment.status);
                                const added = course.some((item) => item.contentId === place.contentId);
                                const lodgingUnavailable = place.contentTypeId === "32"
                                    && (!includeLodging || selectedDay === tripDays - 1 || course.some((item) => item.contentTypeId === "32"));
                                return <article className="course-place-card" key={place.contentId}>
                                    <div className="course-place-image">{place.thumbnailUrl ? <img src={place.thumbnailUrl} alt="" loading="lazy" /> : <span>PetWay</span>}</div>
                                    <div className="course-place-copy">
                                        <div className="course-place-meta"><span>{TYPE_LABELS[place.contentTypeId] ?? "동반 장소"}</span><strong>{formatDistance(origin, place)}</strong></div>
                                        <div className="course-place-title"><h3>{place.title}</h3><FavoriteButton targetType="PLACE" targetId={place.contentId} title={place.title} address={place.address} thumbnailUrl={place.thumbnailUrl} /></div><p>{place.address ?? "주소 정보가 없습니다."}</p>
                                        {place.operatingHours && <small>운영시간 · {place.operatingHours}</small>}
                                        {place.closedDays && <small>휴무일 · {place.closedDays}</small>}
                                        <span className={`course-compatibility ${status.className}`}>{status.label}</span>
                                        {place.compatibility?.petwayAssessment.reasons[0] && <small>{place.compatibility.petwayAssessment.reasons[0]}</small>}
                                        {place.contentTypeId === "32" && <small className="course-lodging-notice">숙박일의 마지막 코스로 추가되며 예약은 해당 업체에서 확인해 주세요.</small>}
                                    </div>
                                    <button type="button" className="course-add-button" disabled={added || lodgingUnavailable || course.length >= maxStops} onClick={() => addPlace(place)} aria-label={`${place.title} 코스에 추가`}>
                                        <span aria-hidden="true">{added ? "✓" : "+"}</span>{added ? "담았어요" : lodgingUnavailable ? "추가 불가" : "코스에 담기"}
                                    </button>
                                </article>;
                            })}
                        </div>
                        {!loading && recommendationTotalPages > 1 && <nav className="facility-pagination" aria-label="추천 장소 페이지">
                            <button type="button" onClick={() => setRecommendationPage((value) => Math.max(1, value - 1))} disabled={recommendationPage === 1}>이전</button>
                            <span>{recommendationPage} / {recommendationTotalPages}</span>
                            <button type="button" onClick={() => setRecommendationPage((value) => Math.min(recommendationTotalPages, value + 1))} disabled={recommendationPage === recommendationTotalPages}>다음</button>
                        </nav>}
                    </section>

                    <aside className="course-builder">
                        <header><div><span>MY COURSE</span><h2>{selectedDay + 1}일차 코스</h2></div><button type="button" onClick={() => updateCourse([])} disabled={course.length === 0}>이 날짜 비우기</button></header>
                        <div className="course-day-tabs" aria-label="여행 날짜 선택">
                            {dayDates.map((date, index) => <button type="button" key={date} className={selectedDay === index ? "active" : ""} onClick={() => setSelectedDay(index)}><strong>DAY {index + 1}</strong><span>{date.slice(5).replace("-", ".")} · {weatherByDay[index] ? WEATHER_ICONS[weatherByDay[index].weatherCondition ?? "UNKNOWN"] : "🌡️"}</span><small>{courses[index]?.length ?? 0}곳</small></button>)}
                        </div>
                        <div className="course-day-weather">
                            {weatherLoading ? <span>기상청 예보를 확인하고 있어요.</span> : weatherByDay[selectedDay] ? <><span className="course-day-weather-icon">{WEATHER_ICONS[weatherByDay[selectedDay].weatherCondition ?? "UNKNOWN"]}</span><div><strong>{dayDates[selectedDay]} · {weatherByDay[selectedDay].display.weather}</strong><p>{weatherByDay[selectedDay].display.temperature} · 강수 {weatherByDay[selectedDay].display.precipitation}</p><small>{weatherByDay[selectedDay].forecastType === "SEASONAL_OUTLOOK" ? "장기 일정은 일별 예보가 아닌 월별 기후 전망이에요." : weatherByDay[selectedDay].display.summary}</small></div></> : <span>이 날짜에 제공된 기상청 예보가 아직 없어요.</span>}
                        </div>
                        <div className="course-transport-tabs" aria-label="이동수단 선택"><button type="button" className={transportMode === "DRIVING" ? "active" : ""} onClick={() => setTransportMode("DRIVING")}>🚗 자동차</button><button type="button" className={transportMode === "TRANSIT" ? "active" : ""} onClick={() => setTransportMode("TRANSIT")}>🚌 대중교통</button></div>
                        {course.length > 0 && <div className="course-route-map"><KakaoMap center={{ latitude: course[0].latitude!, longitude: course[0].longitude! }} markers={markers} routePath={routePath} level={8} /><small className="course-route-source">{routeLoading ? "경로를 찾고 있어요…" : transportMode === "TRANSIT" && transitRoute ? "ODsay 추천 대중교통 경로" : drivingRoute ? "카카오모빌리티 실제 자동차 경로" : "직선거리 기반 예상 경로"}</small></div>}
                        {course.length === 0 ? <div className="course-empty course-empty-builder"><strong>아직 정한 장소가 없어요</strong><span>추천 카드의 + 버튼으로 최대 {maxStops}곳을 담아보세요.</span></div> : <ol className="course-timeline">
                            {routeInfo.map(({ place, travel, distance, stay, transitLeg }, index) => <li key={place.contentId}>
                                {travel !== null && <div className="course-travel-time">{index} → {index + 1} 이동 · {transportMode === "TRANSIT" && transitLeg ? `대중교통 약 ${travel}분 · 환승 ${transitLeg.transfers}회 · ${transitLeg.fare.toLocaleString()}원` : `차량 약 ${travel}분${distance !== null ? ` · 약 ${distance.toFixed(1)}km` : ""}`}{transportMode === "TRANSIT" && transitLeg && <span className="course-transit-steps">{transitLeg.steps.map((step) => `${step.type === "WALK" ? "도보" : step.type === "BUS" ? "버스" : "지하철"} ${step.durationMinutes}분${step.description ? ` (${step.description})` : ""}`).join(" → ")}</span>}</div>}
                                <div className="course-stop-number">{index + 1}</div><div className="course-stop-copy"><strong>{place.title}</strong><span>{TYPE_LABELS[place.contentTypeId] ?? "동반 장소"} · 약 {stay}분 머물기</span></div>
                                <div className="course-stop-actions"><button type="button" onClick={() => moveCourseItem(index, -1)} disabled={index === 0 || place.contentTypeId === "32"}>앞으로</button><button type="button" onClick={() => moveCourseItem(index, 1)} disabled={index === course.length - 1 || course[index + 1]?.contentTypeId === "32"}>뒤로</button><button type="button" className="remove" onClick={() => updateCourse((current) => current.filter((item) => item.contentId !== place.contentId))}>삭제</button></div>
                            </li>)}
                        </ol>}
                        <footer className="course-summary"><div><span>{selectedDay + 1}일차 예상 일정</span><strong>{startTime} - {finishTime}</strong></div><div><span>전체 장소</span><strong>{courses.reduce((sum, day) => sum + day.length, 0)}곳</strong></div><small>{transportMode === "TRANSIT" && transitRoute ? `ODsay 추천 대중교통 기준 · 총 교통비 약 ${transitRoute.totalFare.toLocaleString()}원` : drivingRoute ? "지도와 구간별 시간·거리는 카카오모빌리티 추천 자동차 경로 기준입니다." : "길찾기 정보를 불러오지 못하면 좌표 간 직선거리 기반 참고값을 사용합니다."}</small></footer>
                    </aside>
                </div>
            </div>
        </main>
    );
}

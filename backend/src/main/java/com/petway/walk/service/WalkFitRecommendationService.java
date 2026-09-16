/* 주변 후보에서 동반 불가를 제외하고 규칙 점수와 거리로 산책 장소를 정렬한다. 여행 AI 추천과 별도 흐름이다. */
package com.petway.walk.service;

import com.petway.compatibility.CompatibilityStatus;
import com.petway.external.tour.exception.TourApiErrorType;
import com.petway.external.tour.exception.TourApiException;
import com.petway.pet.entity.Pet;
import com.petway.pet.service.PrimaryPetQueryService;
import com.petway.place.dto.PlaceCompatibilityResponse;
import com.petway.place.model.NearbyPlaceSearchCondition;
import com.petway.place.model.PlaceSearchResult;
import com.petway.place.model.PlaceSummary;
import com.petway.place.service.PlaceCompatibilityService;
import com.petway.place.service.PlaceQueryService;
import com.petway.walk.dto.WalkFitRecommendationResponse;
import com.petway.walk.policy.WalkFitPolicy;
import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.WeatherRegion;
import com.petway.weather.dto.WeatherForecastResponse;
import com.petway.weather.service.WeatherQueryService;
import com.petway.weather.service.WeatherRegionResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class WalkFitRecommendationService {

    // 주변 목록 전체에 상세·날씨 API를 호출하지 않도록 실제 평가 후보 수를 제한한다.
    static final int CANDIDATE_LIMIT = 8;
    static final int NEARBY_POOL_LIMIT = 100;
    private static final int NEAREST_CANDIDATE_COUNT = 2;
    private static final int RECOMMENDATION_LIMIT = CANDIDATE_LIMIT;
    private static final int SEARCH_RADIUS_METERS = 5_000;

    private final PrimaryPetQueryService primaryPetQueryService;
    private final PlaceQueryService placeQueryService;
    private final PlaceCompatibilityService placeCompatibilityService;
    private final WeatherQueryService weatherQueryService;
    private final WeatherRegionResolver weatherRegionResolver;
    private final WalkFitPolicy walkFitPolicy;

    public WalkFitRecommendationResponse recommend(
            String email,
            BigDecimal latitude,
            BigDecimal longitude,
            LocalDate targetDate
    ) {
        ForecastType forecastType = weatherQueryService.resolveForecastType(targetDate);
        Pet primaryPet = primaryPetQueryService.findByUserEmail(email);
        PlaceSearchResult nearby = placeQueryService.findNearbyIncludingCatalog(
                new NearbyPlaceSearchCondition(
                        latitude,
                        longitude,
                        SEARCH_RADIUS_METERS,
                        1,
                        NEARBY_POOL_LIMIT
                )
        );
        // Keeps the service resilient to a mocked/legacy query implementation;
        // the production query service always returns the merged result.
        if (nearby == null) {
            nearby = placeQueryService.findNearby(new NearbyPlaceSearchCondition(
                    latitude, longitude, SEARCH_RADIUS_METERS, 1, NEARBY_POOL_LIMIT));
        }

        List<PlaceSummary> candidatePool = nearby.items().stream()
                .collect(Collectors.toMap(
                        PlaceSummary::contentId,
                        Function.identity(),
                        (first, duplicate) -> first,
                        LinkedHashMap::new
                ))
                .values()
                .stream()
                // 오늘의 산책은 실제로 걸어볼 만한 관광지 중심으로만
                // 구성한다. PlaceQueryService가 관광공사와 CSV를 먼저
                // 합치므로 두 데이터 출처 모두 이 필터를 통과한다.
                .filter(place -> "12".equals(place.contentTypeId()))
                .toList();
        List<PlaceSummary> candidates = selectCandidates(candidatePool, targetDate);
        if (candidates.isEmpty()) {
            return emptyResponse(
                    targetDate,
                    primaryPet,
                    "주변에서 산책 후보 장소를 찾지 못했어요."
            );
        }

        // 중기·계절 전망은 같은 권역의 후보끼리 재사용한다. 단기예보는 장소별 좌표로 조회한다.
        Map<WeatherRegion, WeatherLookup> regionalWeather =
                new EnumMap<>(WeatherRegion.class);
        List<EvaluatedCandidate> evaluated = new ArrayList<>();
        int unavailableCount = 0;
        int compatibilityFailureCount = 0;
        int compatibilityRateLimitCount = 0;

        for (PlaceSummary place : candidates) {
            PlaceCompatibilityResponse compatibility;
            try {
                compatibility = placeCompatibilityService.getCompatibility(
                        primaryPet,
                        place.contentId()
                );
            } catch (RuntimeException exception) {
                compatibilityFailureCount++;
                if (isRateLimited(exception)) {
                    compatibilityRateLimitCount++;
                }
                continue;
            }

            // 동반 불가 장소는 적합도 점수가 높아질 가능성이 있어도 추천 대상에서 먼저 제거한다.
            if (compatibility.petwayAssessment().status()
                    == CompatibilityStatus.UNAVAILABLE) {
                unavailableCount++;
                continue;
            }

            WeatherLookup weather = findWeather(
                    place,
                    targetDate,
                    forecastType,
                    regionalWeather
            );
            WalkFitPolicy.Result walkFit = walkFitPolicy.evaluate(
                    primaryPet,
                    place.contentTypeId(),
                    compatibility.petwayAssessment(),
                    weather.forecast()
            );
            evaluated.add(new EvaluatedCandidate(
                    place,
                    compatibility.petwayAssessment(),
                    walkFit,
                    weather,
                    distanceMeters(latitude, longitude, place)
            ));
        }

        // 후보가 실제로 부적합한 경우와 정보 조회 자체가 실패한 경우를 구분해 안내한다.
        if (evaluated.isEmpty() && compatibilityFailureCount > 0
                && unavailableCount == 0) {
            if (compatibilityRateLimitCount == candidates.size()) {
                throw new WalkFitRateLimitedException(
                        "주변 장소의 반려견 동반 정보를 확인하지 못했어요. 잠시 후 다시 시도해 주세요."
                );
            }
            throw new WalkFitRecommendationException(
                    "장소의 반려견 동반 정보를 확인하지 못했어요. 잠시 후 다시 시도해 주세요."
            );
        }

        if (evaluated.isEmpty()) {
            String message = compatibilityFailureCount > 0
                    ? "일부 장소 정보를 확인하지 못했고, 현재 반려견 조건에 맞는 산책 후보를 찾지 못했어요."
                    : "현재 반려견 조건에 맞는 산책 후보를 찾지 못했어요.";
            return emptyResponse(targetDate, primaryPet, message);
        }

        // 점수 내림차순 → 거리 오름차순 → 장소 ID 순으로 정렬해 동점 결과도 일정하게 유지한다.
        List<EvaluatedCandidate> sorted = evaluated.stream()
                .sorted(Comparator
                        .comparingInt((EvaluatedCandidate item) -> item.walkFit().score())
                        .reversed()
                        .thenComparingDouble(EvaluatedCandidate::distanceMeters)
                        .thenComparing(item -> item.place().contentId()))
                .limit(RECOMMENDATION_LIMIT)
                .toList();

        List<WalkFitRecommendationResponse.Recommendation> recommendations =
                new ArrayList<>();
        for (int index = 0; index < sorted.size(); index++) {
            recommendations.add(toResponse(index + 1, sorted.get(index), forecastType));
        }

        return new WalkFitRecommendationResponse(
                targetDate,
                petSummary(primaryPet),
                recommendations,
                primaryPet.getPetName() + "과 산책하기 좋은 후보를 찾았어요."
        );
    }

    // 가까운 두 후보는 유지하고 나머지는 방문일을 기준으로 순환 선택한다.
    // 같은 날짜에는 동일하게 선택하면서 날짜가 바뀌면 다른 주변 장소도 평가한다.
    static List<PlaceSummary> selectCandidates(
            List<PlaceSummary> candidatePool,
            LocalDate targetDate
    ) {
        if (candidatePool.size() <= CANDIDATE_LIMIT) {
            return List.copyOf(candidatePool);
        }

        List<PlaceSummary> selected = new ArrayList<>(CANDIDATE_LIMIT);
        int nearestCount = Math.min(NEAREST_CANDIDATE_COUNT, candidatePool.size());
        selected.addAll(candidatePool.subList(0, nearestCount));

        List<PlaceSummary> rotatingPool = candidatePool.subList(nearestCount, candidatePool.size());
        int rotatingCandidateCount = CANDIDATE_LIMIT - nearestCount;
        int start = Math.floorMod(
                targetDate.toEpochDay() * rotatingCandidateCount,
                rotatingPool.size()
        );
        for (int offset = 0; selected.size() < CANDIDATE_LIMIT; offset++) {
            selected.add(rotatingPool.get((start + offset) % rotatingPool.size()));
        }
        return List.copyOf(selected);
    }

    private boolean isRateLimited(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof TourApiException tourApiException
                    && tourApiException.getErrorType() == TourApiErrorType.RATE_LIMITED) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private WeatherLookup findWeather(
            PlaceSummary place,
            LocalDate targetDate,
            ForecastType forecastType,
            Map<WeatherRegion, WeatherLookup> regionalWeather
    ) {
        if (place.latitude() == null || place.longitude() == null) {
            return WeatherLookup.unavailable();
        }
        WeatherRegion region = weatherRegionResolver
                .resolve(place.areaCode(), place.sigunguCode())
                .orElse(null);
        if (region == null) {
            return WeatherLookup.unavailable();
        }

        if (forecastType != ForecastType.SHORT_TERM
                && regionalWeather.containsKey(region)) {
            return regionalWeather.get(region);
        }

        WeatherLookup lookup;
        try {
            lookup = new WeatherLookup(
                    weatherQueryService.getForecast(
                            place.latitude().doubleValue(),
                            place.longitude().doubleValue(),
                            targetDate,
                            region
                    )
            );
        } catch (RuntimeException exception) {
            lookup = WeatherLookup.unavailable();
        }

        if (forecastType != ForecastType.SHORT_TERM) {
            regionalWeather.put(region, lookup);
        }
        return lookup;
    }

    private WalkFitRecommendationResponse.Recommendation toResponse(
            int rank,
            EvaluatedCandidate item,
            ForecastType forecastType
    ) {
        PlaceSummary place = item.place();
        PlaceCompatibilityResponse.PetwayAssessment compatibility = item.compatibility();
        WeatherForecastResponse forecast = item.weather().forecast();
        WalkFitRecommendationResponse.Weather weather = forecast == null
                ? WalkFitRecommendationResponse.Weather.unavailable(forecastType)
                : new WalkFitRecommendationResponse.Weather(
                        true,
                        forecast.forecastType(),
                        forecast.display() == null ? null : forecast.display().summary(),
                        forecast.display() == null ? null : forecast.display().temperature(),
                        forecast.display() == null ? null : forecast.display().precipitation()
                );

        return new WalkFitRecommendationResponse.Recommendation(
                rank,
                place.contentId(),
                place.title(),
                place.address(),
                place.latitude(),
                place.longitude(),
                place.thumbnailUrl(),
                new WalkFitRecommendationResponse.WalkFit(
                        item.walkFit().score(),
                        item.walkFit().grade(),
                        item.walkFit().reasons()
                ),
                new WalkFitRecommendationResponse.Compatibility(
                        compatibility.status(),
                        compatibility.requirements(),
                        compatibility.needsConfirmation()
                ),
                weather
        );
    }

    private WalkFitRecommendationResponse emptyResponse(
            LocalDate targetDate,
            Pet pet,
            String message
    ) {
        return new WalkFitRecommendationResponse(
                targetDate,
                petSummary(pet),
                List.of(),
                message
        );
    }

    private WalkFitRecommendationResponse.PetSummary petSummary(Pet pet) {
        return new WalkFitRecommendationResponse.PetSummary(
                pet.getId(),
                pet.getPetName()
        );
    }

    // 위·경도 사이의 직선 거리를 미터 단위로 계산한다. 실제 보행 경로나 이동시간은 아니다.
    private double distanceMeters(
            BigDecimal latitude,
            BigDecimal longitude,
            PlaceSummary place
    ) {
        if (place.latitude() == null || place.longitude() == null) {
            return Double.MAX_VALUE;
        }
        double fromLatitude = Math.toRadians(latitude.doubleValue());
        double toLatitude = Math.toRadians(place.latitude().doubleValue());
        double latitudeDelta = toLatitude - fromLatitude;
        double longitudeDelta = Math.toRadians(
                place.longitude().doubleValue() - longitude.doubleValue()
        );
        double a = Math.sin(latitudeDelta / 2) * Math.sin(latitudeDelta / 2)
                + Math.cos(fromLatitude) * Math.cos(toLatitude)
                * Math.sin(longitudeDelta / 2) * Math.sin(longitudeDelta / 2);
        return 6_371_000 * 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
    }

    private record WeatherLookup(WeatherForecastResponse forecast) {
        private static WeatherLookup unavailable() {
            return new WeatherLookup(null);
        }
    }

    private record EvaluatedCandidate(
            PlaceSummary place,
            PlaceCompatibilityResponse.PetwayAssessment compatibility,
            WalkFitPolicy.Result walkFit,
            WeatherLookup weather,
            double distanceMeters
    ) {
    }
}

/* 동반 조건·장소 유형·반려견 성향·날씨를 점수화한다. AI를 호출하지 않는 결정론적 평가 정책이다. */
package com.petway.walk.policy;

import com.petway.compatibility.CompatibilityStatus;
import com.petway.pet.entity.Pet;
import com.petway.pet.enums.ActivityLevel;
import com.petway.pet.enums.PlacePreference;
import com.petway.place.dto.PlaceCompatibilityResponse.PetwayAssessment;
import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.OutlookLevel;
import com.petway.weather.domain.WeatherCondition;
import com.petway.weather.dto.WeatherForecastResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class WalkFitPolicy {

    // 총점의 기준 배점은 동반 조건 40, 장소 유형 30, 반려견 성향 10, 날씨 20이다.
    private static final int COMPATIBILITY_MAX = 40;
    private static final int PLACE_TYPE_MAX = 30;
    private static final int PET_MAX = 10;
    private static final int WEATHER_MAX = 20;

    private final WalkPlaceTypePolicy placeTypePolicy;

    public Result evaluate(
            Pet pet,
            String contentTypeId,
            PetwayAssessment compatibility,
            WeatherForecastResponse weather
    ) {
        if (compatibility.status() == CompatibilityStatus.UNAVAILABLE) {
            throw new IllegalArgumentException(
                    "동반 불가 장소는 WalkFit 점수를 계산하지 않습니다."
            );
        }

        List<String> reasons = new ArrayList<>();
        int rawScore = compatibilityScore(compatibility.status(), reasons);
        reasons.addAll(compatibility.reasons());
        int maximumScore = COMPATIBILITY_MAX + PLACE_TYPE_MAX + PET_MAX;

        WalkPlaceTypePolicy.Evaluation placeType = placeTypePolicy.evaluate(contentTypeId);
        rawScore += placeType.score();
        reasons.add(placeType.reason());

        rawScore += petScore(pet, placeType, reasons);

        if (weather == null) {
            reasons.add("날씨 정보를 확인하지 못해 날씨는 평가에서 제외했어요.");
        } else {
            rawScore += weatherScore(weather, reasons);
            maximumScore += WEATHER_MAX;
        }

        // 날씨 조회에 실패하면 날씨 배점을 분모에서도 제외한 뒤 100점으로 환산한다.
        // 정보가 없다는 이유만으로 나쁜 날씨와 같은 감점을 주지 않는다.
        int score = clamp(
                (int) Math.round(rawScore * 100.0 / maximumScore),
                0,
                100
        );
        return new Result(score, grade(score), reasons);
    }

    private int compatibilityScore(
            CompatibilityStatus status,
            List<String> reasons
    ) {
        return switch (status) {
            case AVAILABLE -> {
                reasons.add("대표 반려견이 안내된 동반 조건을 충족해요.");
                yield 40;
            }
            case CONDITIONAL -> {
                reasons.add("안내된 준비사항을 지키면 동반 후보로 볼 수 있어요.");
                yield 30;
            }
            case NEED_CONFIRMATION -> {
                reasons.add("방문 전 반려견 동반 가능 여부 확인이 필요해요.");
                yield 18;
            }
            case UNAVAILABLE -> throw new IllegalArgumentException(
                    "동반 불가 상태는 점수화할 수 없습니다."
            );
        };
    }

    // 등록된 선호도·활동량과 장소 유형의 일반적 성격을 비교한다. 개별 장소 시설을 확정하는 판정은 아니다.
    private int petScore(
            Pet pet,
            WalkPlaceTypePolicy.Evaluation placeType,
            List<String> reasons
    ) {
        int score = 0;
        PlacePreference preference = pet.getPlacePreference();
        if (preference == PlacePreference.OUTDOOR && placeType.outdoorOriented()) {
            score += 7;
            reasons.add(pet.getPetName() + "의 야외 장소 선호와 잘 맞는 유형이에요.");
        } else if (preference == PlacePreference.INDOOR && placeType.indoorOriented()) {
            score += 6;
            reasons.add(pet.getPetName() + "의 실내 장소 선호와 잘 맞는 유형이에요.");
        } else if (preference == PlacePreference.BOTH) {
            score += 3;
            reasons.add(pet.getPetName() + "은 실내외 장소를 모두 선호해요.");
        }

        ActivityLevel activity = pet.getActivityLevel();
        if (activity == ActivityLevel.HIGH && placeType.active()) {
            score += 3;
            reasons.add(pet.getPetName() + "의 높은 활동량과 잘 맞는 장소 유형이에요.");
        } else if (activity == ActivityLevel.HIGH && placeType.outdoorOriented()) {
            score += 2;
            reasons.add(pet.getPetName() + "의 높은 활동량을 긍정적으로 반영했어요.");
        } else if (activity == ActivityLevel.MEDIUM && placeType.outdoorOriented()) {
            score += 2;
        } else if (activity == ActivityLevel.LOW && placeType.indoorOriented()) {
            score += 2;
            reasons.add(pet.getPetName() + "의 낮은 활동량에 비교적 무리가 적은 유형이에요.");
        }

        return Math.min(score, PET_MAX);
    }

    private int weatherScore(
            WeatherForecastResponse weather,
            List<String> reasons
    ) {
        // 계절 전망에는 방문 당일의 기온·강수확률을 적용하지 않고 평년 대비 강수 경향만 반영한다.
        if (weather.forecastType() == ForecastType.SEASONAL_OUTLOOK) {
            int score = 12;
            if (weather.precipitationOutlook() == OutlookLevel.ABOVE_NORMAL) {
                score -= 4;
                reasons.add("평년보다 강수량이 많을 가능성이 있어 방문 시 주의가 필요해요.");
            } else if (weather.precipitationOutlook() == OutlookLevel.BELOW_NORMAL) {
                score += 2;
                reasons.add("평년보다 강수량이 적을 가능성을 긍정적으로 반영했어요.");
            }
            reasons.add("월별 기후 전망이라 실제 방문일 날씨와 다를 수 있어요.");
            return score;
        }

        WeatherCondition condition = weather.weatherCondition();
        int score = switch (condition == null ? WeatherCondition.UNKNOWN : condition) {
            case CLEAR -> 18;
            case PARTLY_CLOUDY -> 16;
            case CLOUDY -> 13;
            case RAIN, SHOWER -> 4;
            case RAIN_OR_SNOW, SNOW -> 2;
            case UNKNOWN -> 10;
        };

        if (condition == WeatherCondition.CLEAR) {
            reasons.add("맑은 예보를 산책 후보 평가에 긍정적으로 반영했어요.");
        } else if (condition == WeatherCondition.RAIN
                || condition == WeatherCondition.RAIN_OR_SNOW
                || condition == WeatherCondition.SNOW
                || condition == WeatherCondition.SHOWER) {
            reasons.add("비나 눈 예보가 있어 야외 산책 시 주의가 필요해요.");
        }

        Integer rainProbability = weather.rainProbability();
        if (rainProbability != null) {
            if (rainProbability >= 70) {
                score -= 6;
                reasons.add("강수확률이 높아 산책 적합도를 낮게 반영했어요.");
            } else if (rainProbability >= 50) {
                score -= 3;
                reasons.add("강수 가능성이 있어 우산과 노면 상태를 확인해 주세요.");
            } else if (rainProbability <= 20) {
                score += 2;
            }
        }

        Double minimum = weather.minimumTemperature();
        Double maximum = weather.maximumTemperature();
        if (minimum != null && maximum != null) {
            if (minimum <= 0 || maximum >= 30) {
                score -= 5;
                reasons.add("예상 기온이 산책하기 부담스러울 수 있어 시간을 조절해 주세요.");
            } else if (minimum <= 5 || maximum >= 27) {
                score -= 3;
                reasons.add("기온을 고려해 산책 시간과 준비물을 조절해 주세요.");
            }
        }

        return clamp(score, 0, WEATHER_MAX);
    }

    private int clamp(int value, int minimum, int maximum) {
        return Math.max(minimum, Math.min(maximum, value));
    }

    private WalkFitGrade grade(int score) {
        if (score >= 85) return WalkFitGrade.VERY_GOOD;
        if (score >= 70) return WalkFitGrade.GOOD;
        if (score >= 50) return WalkFitGrade.CAUTION;
        return WalkFitGrade.LOW;
    }

    public record Result(
            int score,
            WalkFitGrade grade,
            List<String> reasons
    ) {
        public Result {
            reasons = List.copyOf(reasons);
        }
    }
}

package com.petway.external.weather.mapper;

import com.petway.external.weather.dto.KmaMidLandItem;
import com.petway.external.weather.dto.KmaMidTemperatureItem;
import com.petway.external.weather.dto.KmaShortForecastItem;
import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.WeatherCondition;
import com.petway.weather.domain.WeatherForecast;
import com.petway.weather.domain.WeatherRegion;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Comparator;
import java.util.List;

@Component
public class KmaWeatherMapper {

    private static final DateTimeFormatter DATE = DateTimeFormatter.BASIC_ISO_DATE;
    private static final DateTimeFormatter DATE_TIME = DateTimeFormatter.ofPattern("yyyyMMddHHmm");

    public WeatherForecast mapShortTerm(
            List<KmaShortForecastItem> items,
            LocalDate targetDate,
            LocalDateTime announcedAt,
            WeatherRegion region
    ) {
        List<KmaShortForecastItem> targetItems = items.stream()
                .filter(item -> targetDate.format(DATE).equals(item.fcstDate()))
                .toList();
        if (targetItems.isEmpty()) {
            throw new IllegalStateException("요청한 날짜의 단기예보가 아직 제공되지 않았습니다.");
        }

        List<Double> temperatures = targetItems.stream()
                .filter(item -> "TMP".equals(item.category()))
                .map(KmaShortForecastItem::fcstValue)
                .map(this::parseDouble)
                .filter(value -> value != null)
                .toList();

        Double minimum = valueOf(targetItems, "TMN");
        Double maximum = valueOf(targetItems, "TMX");
        if (minimum == null) minimum = temperatures.stream().min(Comparator.naturalOrder()).orElse(null);
        if (maximum == null) maximum = temperatures.stream().max(Comparator.naturalOrder()).orElse(null);

        Integer rainProbability = targetItems.stream()
                .filter(item -> "POP".equals(item.category()))
                .map(KmaShortForecastItem::fcstValue)
                .map(this::parseInteger)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .orElse(null);

        WeatherCondition condition = targetItems.stream()
                .filter(item -> "PTY".equals(item.category()))
                .map(KmaShortForecastItem::fcstValue)
                .map(this::precipitationCondition)
                .filter(value -> value != WeatherCondition.UNKNOWN)
                .findFirst()
                .orElseGet(() -> skyCondition(targetItems));

        return new WeatherForecast(
                targetDate,
                ForecastType.SHORT_TERM,
                region.getDisplayName(),
                minimum,
                maximum,
                rainProbability,
                condition,
                null,
                null,
                null,
                null,
                announcedAt,
                "시간별 예보를 여행일 단위로 요약한 정보입니다."
        );
    }

    public WeatherForecast mapMidTerm(
            KmaMidLandItem land,
            KmaMidTemperatureItem temperature,
            WeatherRegion region,
            LocalDate targetDate,
            int forecastDay,
            LocalDateTime announcedAt
    ) {
        return new WeatherForecast(
                targetDate,
                ForecastType.MID_TERM,
                region.getDisplayName(),
                temperature.minimumTemperature(forecastDay),
                temperature.maximumTemperature(forecastDay),
                land.rainProbability(forecastDay),
                descriptionCondition(land.weatherDescription(forecastDay)),
                null,
                null,
                null,
                null,
                announcedAt,
                "중기예보의 지역 대표 지점 기온과 권역 날씨입니다."
        );
    }

    public LocalDateTime announcedAt(String baseDate, String baseTime) {
        return LocalDateTime.parse(baseDate + baseTime, DATE_TIME);
    }

    private Double valueOf(List<KmaShortForecastItem> items, String category) {
        return items.stream()
                .filter(item -> category.equals(item.category()))
                .map(KmaShortForecastItem::fcstValue)
                .map(this::parseDouble)
                .filter(value -> value != null)
                .findFirst()
                .orElse(null);
    }

    private WeatherCondition skyCondition(List<KmaShortForecastItem> items) {
        return items.stream()
                .filter(item -> "SKY".equals(item.category()))
                .map(KmaShortForecastItem::fcstValue)
                .map(this::parseInteger)
                .filter(value -> value != null)
                .max(Integer::compareTo)
                .map(value -> switch (value) {
                    case 1 -> WeatherCondition.CLEAR;
                    case 3 -> WeatherCondition.PARTLY_CLOUDY;
                    case 4 -> WeatherCondition.CLOUDY;
                    default -> WeatherCondition.UNKNOWN;
                })
                .orElse(WeatherCondition.UNKNOWN);
    }

    private WeatherCondition precipitationCondition(String value) {
        Integer code = parseInteger(value);
        if (code == null) return WeatherCondition.UNKNOWN;
        return switch (code) {
            case 1, 5 -> WeatherCondition.RAIN;
            case 2, 6 -> WeatherCondition.RAIN_OR_SNOW;
            case 3, 7 -> WeatherCondition.SNOW;
            case 4 -> WeatherCondition.SHOWER;
            default -> WeatherCondition.UNKNOWN;
        };
    }

    private WeatherCondition descriptionCondition(String description) {
        if (description == null || description.isBlank()) return WeatherCondition.UNKNOWN;
        if (description.contains("눈") && description.contains("비")) return WeatherCondition.RAIN_OR_SNOW;
        if (description.contains("소나기")) return WeatherCondition.SHOWER;
        if (description.contains("눈")) return WeatherCondition.SNOW;
        if (description.contains("비")) return WeatherCondition.RAIN;
        if (description.contains("흐림")) return WeatherCondition.CLOUDY;
        if (description.contains("구름")) return WeatherCondition.PARTLY_CLOUDY;
        if (description.contains("맑")) return WeatherCondition.CLEAR;
        return WeatherCondition.UNKNOWN;
    }

    private Double parseDouble(String value) {
        try {
            return Double.valueOf(value);
        } catch (NumberFormatException | NullPointerException ignored) {
            return null;
        }
    }

    private Integer parseInteger(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException | NullPointerException ignored) {
            return null;
        }
    }
}

/* 일별 예보와 월별 전망을 구분해 사용자에게 표시할 한국어 설명을 만든다. */
package com.petway.weather.dto;

import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.OutlookLevel;
import com.petway.weather.domain.OutlookProbabilities;
import com.petway.weather.domain.WeatherCondition;
import com.petway.weather.domain.WeatherForecast;

import java.math.BigDecimal;

public record WeatherDisplay(
        String temperature,
        String precipitation,
        String weather,
        String summary
) {
    public static WeatherDisplay from(WeatherForecast forecast) {
        if (forecast.forecastType() == ForecastType.SEASONAL_OUTLOOK) {
            return seasonal(forecast);
        }
        return daily(forecast);
    }

    private static WeatherDisplay daily(WeatherForecast forecast) {
        String temperature = formatTemperatureRange(
                forecast.minimumTemperature(),
                forecast.maximumTemperature()
        );
        String precipitation = forecast.rainProbability() == null
                ? "강수확률 정보 없음"
                : "강수확률 " + forecast.rainProbability() + "%";
        String weather = weatherText(forecast.weatherCondition());
        String summary = weatherSummaryLead(forecast.weatherCondition()) + ", " + precipitation + "입니다.";
        return new WeatherDisplay(temperature, precipitation, weather, summary);
    }

    private static WeatherDisplay seasonal(WeatherForecast forecast) {
        String temperature = temperatureOutlook(
                forecast.temperatureOutlook(),
                forecast.temperatureProbabilities()
        );
        String precipitation = precipitationOutlook(
                forecast.precipitationOutlook(),
                forecast.precipitationProbabilities()
        );
        String summary = "기온은 " + temperature + "이며, 강수량은 " + precipitation + "입니다.";
        return new WeatherDisplay(temperature, precipitation, "월별 기후 전망", summary);
    }

    private static String formatTemperatureRange(Double minimum, Double maximum) {
        if (minimum == null || maximum == null) return "기온 정보 없음";
        return number(minimum) + "~" + number(maximum) + "℃";
    }

    private static String number(Double value) {
        return BigDecimal.valueOf(value).stripTrailingZeros().toPlainString();
    }

    private static String weatherText(WeatherCondition condition) {
        if (condition == null) return "날씨 정보 없음";
        return switch (condition) {
            case CLEAR -> "맑음";
            case PARTLY_CLOUDY -> "구름많음";
            case CLOUDY -> "흐림";
            case RAIN -> "비";
            case RAIN_OR_SNOW -> "비 또는 눈";
            case SNOW -> "눈";
            case SHOWER -> "소나기";
            case UNKNOWN -> "날씨 상태 확인 필요";
        };
    }

    private static String weatherSummaryLead(WeatherCondition condition) {
        if (condition == null) return "날씨 정보가 제공되지 않았으며";
        return switch (condition) {
            case CLEAR -> "맑은 날씨가 예상되며";
            case PARTLY_CLOUDY -> "구름이 많겠으며";
            case CLOUDY -> "흐린 날씨가 예상되며";
            case RAIN -> "비가 예상되며";
            case RAIN_OR_SNOW -> "비 또는 눈이 예상되며";
            case SNOW -> "눈이 예상되며";
            case SHOWER -> "소나기가 예상되며";
            case UNKNOWN -> "날씨 상태 확인이 필요하며";
        };
    }

    private static String temperatureOutlook(OutlookLevel level, OutlookProbabilities probabilities) {
        if (level == null || probabilities == null) return "기온 전망 정보 없음";
        String description = switch (level) {
            case BELOW_NORMAL -> "평년보다 낮을 가능성";
            case NEAR_NORMAL -> "평년과 비슷할 가능성";
            case ABOVE_NORMAL -> "평년보다 높을 가능성";
            case MIXED -> "복수 전망의 최고 확률이 동일함";
        };
        return description + " " + maximum(probabilities) + "%";
    }

    private static String precipitationOutlook(OutlookLevel level, OutlookProbabilities probabilities) {
        if (level == null || probabilities == null) return "강수량 전망 정보 없음";
        String description = switch (level) {
            case BELOW_NORMAL -> "평년보다 적을 가능성";
            case NEAR_NORMAL -> "평년과 비슷할 가능성";
            case ABOVE_NORMAL -> "평년보다 많을 가능성";
            case MIXED -> "복수 전망의 최고 확률이 동일함";
        };
        return description + " " + maximum(probabilities) + "%";
    }

    private static int maximum(OutlookProbabilities probabilities) {
        return Math.max(
                probabilities.belowNormal(),
                Math.max(probabilities.nearNormal(), probabilities.aboveNormal())
        );
    }
}

package com.petway.external.weather.mapper;

import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.OutlookLevel;
import com.petway.weather.domain.WeatherRegion;
import org.junit.jupiter.api.Test;

import java.time.LocalDate;
import java.nio.charset.StandardCharsets;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class KmaSeasonalOutlookParserTest {

    private final KmaSeasonalOutlookParser parser = new KmaSeasonalOutlookParser();

    @Test
    void mapsOfficialXmlShapeWithoutTurningMonthlyOutlookIntoDailyWeather() {
        String fixture = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss><channel><item><description>
                  <header><ydate>2026년 8월 24일 10시</ydate></header>
                  <threemonth_ta_forecast><local_ta>
                    <local_ta_name><![CDATA[ 서울ㆍ인천ㆍ경기도 ]]></local_ta_name>
                    <month1_local_ta_minVal>20</month1_local_ta_minVal>
                    <month1_local_ta_similarVal>40</month1_local_ta_similarVal>
                    <month1_local_ta_maxVal>40</month1_local_ta_maxVal>
                  </local_ta></threemonth_ta_forecast>
                  <threemonth_rn_forecast><local_rn>
                    <local_rn_name><![CDATA[ 서울ㆍ인천ㆍ경기도 ]]></local_rn_name>
                    <month1_local_rn_minVal>20</month1_local_rn_minVal>
                    <month1_local_rn_similarVal>50</month1_local_rn_similarVal>
                    <month1_local_rn_maxVal>30</month1_local_rn_maxVal>
                  </local_rn></threemonth_rn_forecast>
                </description></item></channel></rss>
                """;

        var forecast = parser.parse(
                fixture.getBytes(StandardCharsets.UTF_8),
                WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                LocalDate.of(2026, 9, 15)
        );

        assertThat(forecast.forecastType()).isEqualTo(ForecastType.SEASONAL_OUTLOOK);
        assertThat(forecast.minimumTemperature()).isNull();
        assertThat(forecast.weatherCondition()).isNull();
        assertThat(forecast.temperatureOutlook()).isEqualTo(OutlookLevel.MIXED);
        assertThat(forecast.precipitationOutlook()).isEqualTo(OutlookLevel.NEAR_NORMAL);
        assertThat(forecast.temperatureProbabilities().aboveNormal()).isEqualTo(40);
        assertThat(forecast.notice()).contains("특정 날짜의 날씨가 아닌");
    }

    @Test
    void mapsJeonbukAutonomousProvinceFromCurrentOfficialRegionName() {
        String fixture = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss><channel><item><description>
                  <header><ydate>2026년 8월 24일 11시</ydate></header>
                  <threemonth_ta_forecast><local_ta>
                    <local_ta_name><![CDATA[ 전북자치도 ]]></local_ta_name>
                    <month1_local_ta_minVal>20</month1_local_ta_minVal>
                    <month1_local_ta_similarVal>40</month1_local_ta_similarVal>
                    <month1_local_ta_maxVal>40</month1_local_ta_maxVal>
                  </local_ta></threemonth_ta_forecast>
                  <threemonth_rn_forecast><local_rn>
                    <local_rn_name><![CDATA[ 전북자치도 ]]></local_rn_name>
                    <month1_local_rn_minVal>30</month1_local_rn_minVal>
                    <month1_local_rn_similarVal>50</month1_local_rn_similarVal>
                    <month1_local_rn_maxVal>20</month1_local_rn_maxVal>
                  </local_rn></threemonth_rn_forecast>
                </description></item></channel></rss>
                """;

        var forecast = parser.parse(
                fixture.getBytes(StandardCharsets.UTF_8),
                WeatherRegion.JEONBUK,
                LocalDate.of(2026, 9, 15)
        );

        assertThat(forecast.regionName()).isEqualTo("전북자치도");
        assertThat(forecast.temperatureOutlook()).isEqualTo(OutlookLevel.MIXED);
        assertThat(forecast.precipitationOutlook()).isEqualTo(OutlookLevel.NEAR_NORMAL);
    }

    @Test
    void distinguishesUnavailableTargetMonthFromInvalidXml() {
        String fixture = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss><channel><item><description>
                  <header><ydate>2026년 8월 24일 11시</ydate></header>
                </description></item></channel></rss>
                """;

        assertThatThrownBy(() -> parser.parse(
                fixture.getBytes(StandardCharsets.UTF_8),
                WeatherRegion.JEJU,
                LocalDate.of(2026, 12, 1)
        )).isInstanceOf(SeasonalOutlookUnavailableException.class);

        assertThatThrownBy(() -> parser.parse(
                "<not-closed>".getBytes(StandardCharsets.UTF_8),
                WeatherRegion.JEJU,
                LocalDate.of(2026, 9, 1)
        ))
                .isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(SeasonalOutlookUnavailableException.class)
                .hasMessageContaining("형식");
    }

    @Test
    void rejectsSeasonalProbabilityOutsideZeroToOneHundred() {
        String fixture = """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss><channel><item><description>
                  <header><ydate>2026년 8월 24일 11시</ydate></header>
                  <threemonth_ta_forecast><local_ta>
                    <local_ta_name>제주도</local_ta_name>
                    <month1_local_ta_minVal>101</month1_local_ta_minVal>
                    <month1_local_ta_similarVal>0</month1_local_ta_similarVal>
                    <month1_local_ta_maxVal>0</month1_local_ta_maxVal>
                  </local_ta></threemonth_ta_forecast>
                  <threemonth_rn_forecast><local_rn>
                    <local_rn_name>제주도</local_rn_name>
                    <month1_local_rn_minVal>20</month1_local_rn_minVal>
                    <month1_local_rn_similarVal>50</month1_local_rn_similarVal>
                    <month1_local_rn_maxVal>30</month1_local_rn_maxVal>
                  </local_rn></threemonth_rn_forecast>
                </description></item></channel></rss>
                """;

        assertThatThrownBy(() -> parser.parse(
                fixture.getBytes(StandardCharsets.UTF_8),
                WeatherRegion.JEJU,
                LocalDate.of(2026, 9, 15)
        ))
                .isInstanceOf(IllegalStateException.class)
                .isNotInstanceOf(SeasonalOutlookUnavailableException.class)
                .hasMessageContaining("형식");
    }
}

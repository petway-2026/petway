/* WeatherApiProperties의 동작을 검증하는 테스트다. 검증 대상: 단기·중기 API 주소와 키, 계절 전망 안내 주소, HTTP 제한 시간을 관리한다. */
package com.petway.external.weather.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.context.properties.bind.Bindable;
import org.springframework.boot.context.properties.bind.Binder;
import org.springframework.boot.context.properties.source.MapConfigurationPropertySource;

import java.time.Duration;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherApiPropertiesTest {

    @Test
    void bindsSeparateShortAndMidApiSettings() {
        MapConfigurationPropertySource source = new MapConfigurationPropertySource(Map.of(
                "weather.short.base-url", "https://weather.test/short",
                "weather.short.api-key", "short-key",
                "weather.mid.base-url", "https://weather.test/mid",
                "weather.mid.api-key", "mid-key",
                "weather.seasonal-guide-url", "https://weather.test/seasonal-guide",
                "weather.timeout.connect-timeout", "2s",
                "weather.timeout.read-timeout", "4s"
        ));

        WeatherApiProperties properties = new Binder(source)
                .bind("weather", Bindable.of(WeatherApiProperties.class))
                .orElseThrow(() -> new IllegalStateException("날씨 설정을 바인딩할 수 없습니다."));

        assertThat(properties.getShort().getBaseUrl()).isEqualTo("https://weather.test/short");
        assertThat(properties.getShort().getApiKey()).isEqualTo("short-key");
        assertThat(properties.getMid().getBaseUrl()).isEqualTo("https://weather.test/mid");
        assertThat(properties.getMid().getApiKey()).isEqualTo("mid-key");
        assertThat(properties.getSeasonalGuideUrl()).isEqualTo("https://weather.test/seasonal-guide");
        assertThat(properties.getTimeout().getConnectTimeout()).isEqualTo(Duration.ofSeconds(2));
        assertThat(properties.getTimeout().getReadTimeout()).isEqualTo(Duration.ofSeconds(4));
    }
}

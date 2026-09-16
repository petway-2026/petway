/* WeatherRegionResolver의 동작을 검증하는 테스트다. 검증 대상: 관광공사 지역·시군구 코드를 서비스 날씨 권역으로 변환한다. */
package com.petway.weather.service;

import com.petway.weather.domain.WeatherRegion;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class WeatherRegionResolverTest {

    private final WeatherRegionResolver resolver = new WeatherRegionResolver();

    @Test
    void resolvesTourAreaAndSigunguCodesToExistingWeatherRegions() {
        assertThat(resolver.resolve("1", "1"))
                .contains(WeatherRegion.SEOUL_INCHEON_GYEONGGI);
        assertThat(resolver.resolve("32", "1"))
                .contains(WeatherRegion.GANGWON_YEONGDONG);
        assertThat(resolver.resolve("32", null))
                .contains(WeatherRegion.GANGWON_YEONGSEO);
        assertThat(resolver.resolve(null, null)).isEmpty();
    }
}

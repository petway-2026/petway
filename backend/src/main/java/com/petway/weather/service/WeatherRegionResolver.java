/* 관광공사 지역·시군구 코드를 서비스 날씨 권역으로 변환한다. */
package com.petway.weather.service;

import com.petway.weather.domain.WeatherRegion;
import org.springframework.stereotype.Component;

import java.util.Optional;
import java.util.Set;

@Component
public class WeatherRegionResolver {

    private static final Set<String> GANGWON_YEONGDONG_SIGUNGU_CODES = Set.of(
            "1", "2", "3", "4", "5", "7", "14"
    );

    public Optional<WeatherRegion> resolve(String areaCode, String sigunguCode) {
        if (areaCode == null) {
            return Optional.empty();
        }

        return Optional.ofNullable(switch (areaCode) {
            case "1", "2", "31" -> WeatherRegion.SEOUL_INCHEON_GYEONGGI;
            case "32" -> sigunguCode != null
                    && GANGWON_YEONGDONG_SIGUNGU_CODES.contains(sigunguCode)
                    ? WeatherRegion.GANGWON_YEONGDONG
                    : WeatherRegion.GANGWON_YEONGSEO;
            case "3", "8", "34" -> WeatherRegion.DAEJEON_SEJONG_CHUNGNAM;
            case "33" -> WeatherRegion.CHUNGBUK;
            case "5", "38" -> WeatherRegion.GWANGJU_JEONNAM;
            case "37" -> WeatherRegion.JEONBUK;
            case "4", "35" -> WeatherRegion.DAEGU_GYEONGBUK;
            case "6", "7", "36" -> WeatherRegion.BUSAN_ULSAN_GYEONGNAM;
            case "39" -> WeatherRegion.JEJU;
            default -> null;
        });
    }
}

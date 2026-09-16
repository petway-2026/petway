/* 관광공사 API 접속 주소, 인증 및 요청 설정을 외부 설정에서 받는다. */
package com.petway.external.tour.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;

@ConfigurationProperties(prefix = "tour-api")
public record TourApiProperties(
        String baseUrl,
        String serviceKey,
        String mobileOs,
        String mobileApp,
        int defaultPageSize,
        Duration connectTimeout,
        Duration readTimeout
) {
}

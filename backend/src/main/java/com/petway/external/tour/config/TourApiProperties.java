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

package com.petway.hospital.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
public class AnimalHospitalApiProperties {

    private final String baseUrl;
    private final String serviceKey;
    private final Duration connectTimeout;
    private final Duration readTimeout;

    public AnimalHospitalApiProperties(
            @Value("${animal-hospital-api.base-url:https://apis.data.go.kr/1741000/animal_hospitals}")
            String baseUrl,
            @Value("${animal-hospital-api.service-key:${ANIMAL_HOSPITAL_API_KEY:}}")
            String serviceKey,
            @Value("${animal-hospital-api.connect-timeout:3s}")
            Duration connectTimeout,
            @Value("${animal-hospital-api.read-timeout:10s}")
            Duration readTimeout
    ) {
        this.baseUrl = stripTrailingSlash(baseUrl);
        this.serviceKey = serviceKey == null ? "" : serviceKey.trim();
        this.connectTimeout = connectTimeout;
        this.readTimeout = readTimeout;
    }

    public String baseUrl() {
        return baseUrl;
    }

    public String serviceKey() {
        return serviceKey;
    }

    public Duration connectTimeout() {
        return connectTimeout;
    }

    public Duration readTimeout() {
        return readTimeout;
    }

    private static String stripTrailingSlash(String value) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("동물병원 API base URL이 비어 있습니다.");
        }

        return value.endsWith("/")
                ? value.substring(0, value.length() - 1)
                : value;
    }
}

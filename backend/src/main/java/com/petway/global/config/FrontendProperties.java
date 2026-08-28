package com.petway.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;

@Component
public class FrontendProperties {

    private final List<String> origins;

    public FrontendProperties(
            @Value("${FRONTEND_ORIGINS:http://localhost:5173,http://localhost:5174}")
            String configuredOrigins
    ) {
        this.origins = Arrays.stream(configuredOrigins.split(","))
                .map(String::trim)
                .filter(origin -> !origin.isBlank())
                .map(origin -> origin.replaceAll("/+$", ""))
                .toList();

        if (origins.isEmpty()) {
            throw new IllegalArgumentException(
                    "FRONTEND_ORIGINS에 하나 이상의 origin이 필요합니다."
            );
        }
    }

    public List<String> getOrigins() {
        return origins;
    }

    public String primaryOrigin() {
        return origins.get(0);
    }
}

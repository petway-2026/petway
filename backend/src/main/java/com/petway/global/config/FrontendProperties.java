/* 허용할 프론트엔드 origin 목록을 검증하고 CORS 및 로그인 리다이렉트의 기준 주소를 제공한다. */
package com.petway.global.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.util.Arrays;
import java.util.List;
import java.net.URI;

@Component
public class FrontendProperties {

    private final List<String> origins;

    public FrontendProperties(
            @Value("${frontend.origins:${FRONTEND_ORIGINS:http://localhost:5173,http://localhost:5174}}")
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
        for (String origin : origins) {
            URI uri = URI.create(origin);
            if (!("http".equals(uri.getScheme()) || "https".equals(uri.getScheme()))
                    || uri.getHost() == null || uri.getUserInfo() != null
                    || uri.getQuery() != null || uri.getFragment() != null
                    || !uri.getPath().isEmpty()) {
                throw new IllegalArgumentException("FRONTEND_ORIGINS에는 정확한 HTTP(S) origin만 지정할 수 있습니다.");
            }
        }
    }

    public List<String> getOrigins() {
        return origins;
    }

    public String primaryOrigin() {
        return origins.get(0);
    }
}

/* 단기·중기 API 주소와 키, 계절 전망 안내 주소, HTTP 제한 시간을 관리한다. */
package com.petway.external.weather.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

import java.time.Duration;

@Component
@ConfigurationProperties(prefix = "weather")
public class WeatherApiProperties {

    private final Api shortConfig = new Api();
    private final Api midConfig = new Api();
    private final Timeout timeout = new Timeout();
    private String seasonalGuideUrl = "https://www.weather.go.kr/w/pop/rss-guide.do";

    public Api getShort() {
        return shortConfig;
    }

    public Api getMid() {
        return midConfig;
    }

    public String getSeasonalGuideUrl() {
        return seasonalGuideUrl;
    }

    public void setSeasonalGuideUrl(String seasonalGuideUrl) {
        this.seasonalGuideUrl = seasonalGuideUrl;
    }

    public Timeout getTimeout() {
        return timeout;
    }

    public static class Timeout {

        private Duration connectTimeout = Duration.ofSeconds(3);
        private Duration readTimeout = Duration.ofSeconds(5);

        public Duration getConnectTimeout() {
            return connectTimeout;
        }

        public void setConnectTimeout(Duration connectTimeout) {
            this.connectTimeout = connectTimeout;
        }

        public Duration getReadTimeout() {
            return readTimeout;
        }

        public void setReadTimeout(Duration readTimeout) {
            this.readTimeout = readTimeout;
        }
    }

    public static class Api {

        private String baseUrl = "";
        private String apiKey = "";

        public String getBaseUrl() {
            return baseUrl;
        }

        public void setBaseUrl(String baseUrl) {
            this.baseUrl = baseUrl;
        }

        public String getApiKey() {
            return apiKey;
        }

        public void setApiKey(String apiKey) {
            this.apiKey = apiKey;
        }
    }
}

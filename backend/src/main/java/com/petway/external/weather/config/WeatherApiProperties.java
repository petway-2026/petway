package com.petway.external.weather.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "weather")
public class WeatherApiProperties {

    private final Api shortConfig = new Api();
    private final Api midConfig = new Api();
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

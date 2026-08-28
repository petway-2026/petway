package com.petway.external.weather.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

@Component
@ConfigurationProperties(prefix = "weather")
public class WeatherApiProperties {

    private String apiKey = "";
    private String shortTermBaseUrl = "https://apis.data.go.kr/1360000/VilageFcstInfoService_2.0";
    private String midTermBaseUrl = "https://apis.data.go.kr/1360000/MidFcstInfoService";
    private String seasonalGuideUrl = "https://www.weather.go.kr/w/pop/rss-guide.do";

    public String getApiKey() {
        return apiKey;
    }

    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }

    public String getShortTermBaseUrl() {
        return shortTermBaseUrl;
    }

    public void setShortTermBaseUrl(String shortTermBaseUrl) {
        this.shortTermBaseUrl = shortTermBaseUrl;
    }

    public String getMidTermBaseUrl() {
        return midTermBaseUrl;
    }

    public void setMidTermBaseUrl(String midTermBaseUrl) {
        this.midTermBaseUrl = midTermBaseUrl;
    }

    public String getSeasonalGuideUrl() {
        return seasonalGuideUrl;
    }

    public void setSeasonalGuideUrl(String seasonalGuideUrl) {
        this.seasonalGuideUrl = seasonalGuideUrl;
    }
}

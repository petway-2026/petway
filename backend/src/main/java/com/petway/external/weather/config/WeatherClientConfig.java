/* 날씨 전용 HTTP 클라이언트와 날짜 계산에 사용할 Clock을 등록한다. */
package com.petway.external.weather.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.ZoneId;

@Configuration
public class WeatherClientConfig {

    @Bean
    public RestClient weatherRestClient(WeatherApiProperties properties) {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(properties.getTimeout().getConnectTimeout());
        requestFactory.setReadTimeout(properties.getTimeout().getReadTimeout());
        return RestClient.builder()
                .requestFactory(requestFactory)
                .build();
    }

    @Bean
    public Clock weatherClock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}

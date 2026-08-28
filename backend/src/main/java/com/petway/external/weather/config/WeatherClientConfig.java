package com.petway.external.weather.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Duration;
import java.time.ZoneId;

@Configuration
public class WeatherClientConfig {

    @Bean
    public RestClient.Builder weatherRestClientBuilder() {
        SimpleClientHttpRequestFactory requestFactory = new SimpleClientHttpRequestFactory();
        requestFactory.setConnectTimeout(Duration.ofSeconds(3));
        requestFactory.setReadTimeout(Duration.ofSeconds(5));
        return RestClient.builder().requestFactory(requestFactory);
    }

    @Bean
    public Clock weatherClock() {
        return Clock.system(ZoneId.of("Asia/Seoul"));
    }
}

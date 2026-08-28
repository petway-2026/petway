package com.petway.external.weather.client;

import com.petway.external.weather.config.WeatherApiProperties;
import com.petway.external.weather.mapper.KmaGridConverter;
import com.petway.external.weather.mapper.KmaSeasonalOutlookParser;
import com.petway.external.weather.mapper.KmaWeatherMapper;
import com.petway.weather.domain.WeatherCondition;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KmaWeatherClientTest {

    private MockRestServiceServer server;
    private KmaWeatherClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        WeatherApiProperties properties = new WeatherApiProperties();
        properties.setApiKey("test-key");
        properties.setShortTermBaseUrl("https://weather.test/short");
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-28T03:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        client = new KmaWeatherClient(
                builder,
                properties,
                new KmaGridConverter(),
                new KmaWeatherMapper(),
                new KmaSeasonalOutlookParser(),
                clock
        );
    }

    @Test
    void callsOfficialShortTermContractAndMapsFixture() {
        server.expect(requestTo(allOf(
                        containsString("https://weather.test/short/getVilageFcst"),
                        containsString("base_date=20260828"),
                        containsString("base_time=1100"),
                        containsString("nx=60"),
                        containsString("ny=127")
                )))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {"resultCode": "00", "resultMsg": "NORMAL_SERVICE"},
                            "body": {
                              "totalCount": 4,
                              "items": {"item": [
                                {"baseDate":"20260828","baseTime":"1100","category":"TMP","fcstDate":"20260829","fcstTime":"0900","fcstValue":"22","nx":60,"ny":127},
                                {"baseDate":"20260828","baseTime":"1100","category":"TMP","fcstDate":"20260829","fcstTime":"1500","fcstValue":"29","nx":60,"ny":127},
                                {"baseDate":"20260828","baseTime":"1100","category":"POP","fcstDate":"20260829","fcstTime":"1500","fcstValue":"60","nx":60,"ny":127},
                                {"baseDate":"20260828","baseTime":"1100","category":"SKY","fcstDate":"20260829","fcstTime":"1500","fcstValue":"4","nx":60,"ny":127}
                              ]}
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        var forecast = client.getShortTerm(
                37.5665,
                126.9780,
                LocalDate.of(2026, 8, 29)
        );

        assertThat(forecast.minimumTemperature()).isEqualTo(22.0);
        assertThat(forecast.maximumTemperature()).isEqualTo(29.0);
        assertThat(forecast.rainProbability()).isEqualTo(60);
        assertThat(forecast.weatherCondition()).isEqualTo(WeatherCondition.CLOUDY);
        server.verify();
    }
}

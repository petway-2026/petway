/* KmaWeatherClient의 동작을 검증하는 테스트다. 검증 대상: 단기·중기·계절 전망을 조회하고 발표 시각과 제공자별 응답 형식 차이를 처리한다. */
package com.petway.external.weather.client;

import com.petway.external.weather.config.WeatherApiProperties;
import com.petway.external.weather.mapper.KmaGridConverter;
import com.petway.external.weather.mapper.KmaSeasonalOutlookParser;
import com.petway.external.weather.mapper.KmaWeatherMapper;
import com.petway.weather.domain.ForecastType;
import com.petway.weather.domain.WeatherCondition;
import com.petway.weather.domain.WeatherRegion;
import com.petway.weather.service.WeatherForecastUnavailableException;
import com.petway.weather.service.WeatherProviderException;
import com.petway.weather.service.WeatherProviderTimeoutException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.hamcrest.Matchers.allOf;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.requestTo;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class KmaWeatherClientTest {

    private MockRestServiceServer server;
    private KmaWeatherClient client;
    private WeatherApiProperties properties;
    private RestClient restClient;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        restClient = builder.build();
        properties = new WeatherApiProperties();
        properties.getShort().setApiKey("short-key");
        properties.getShort().setBaseUrl("https://weather.test/short");
        properties.getMid().setApiKey("mid-key");
        properties.getMid().setBaseUrl("https://weather.test/mid");
        Clock clock = Clock.fixed(
                Instant.parse("2026-08-28T03:00:00Z"),
                ZoneId.of("Asia/Seoul")
        );
        client = newClient(restClient, clock);
    }

    @Test
    void callsOfficialShortTermContractAndMapsFixture() {
        server.expect(requestTo(allOf(
                        containsString("https://weather.test/short/getVilageFcst"),
                        containsString("serviceKey=short-key"),
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
                LocalDate.of(2026, 8, 29),
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        );

        assertThat(forecast.minimumTemperature()).isEqualTo(22.0);
        assertThat(forecast.maximumTemperature()).isEqualTo(29.0);
        assertThat(forecast.rainProbability()).isEqualTo(60);
        assertThat(forecast.weatherCondition()).isEqualTo(WeatherCondition.CLOUDY);
        server.verify();
    }

    @Test
    void usesMidTermBaseUrlAndKeyForBothMidTermRequests() {
        server.expect(requestTo(allOf(
                        containsString("https://weather.test/mid/getMidLandFcst"),
                        containsString("serviceKey=mid-key"),
                        containsString("regId=11B00000"),
                        containsString("tmFc=202608280600")
                )))
                .andRespond(withSuccess(midResponse("""
                        {"regId":"11B00000","rnSt5Am":30,"rnSt5Pm":60,"wf5Am":"구름많음","wf5Pm":"흐리고 비"}
                        """), MediaType.APPLICATION_JSON));
        server.expect(requestTo(allOf(
                        containsString("https://weather.test/mid/getMidTa"),
                        containsString("serviceKey=mid-key"),
                        containsString("regId=11B10101")
                )))
                .andRespond(withSuccess(midResponse("""
                        {"regId":"11B10101","taMin5":21,"taMax5":28}
                        """), MediaType.APPLICATION_JSON));

        var forecast = client.getMidTerm(
                WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                LocalDate.of(2026, 9, 2)
        );

        assertThat(forecast.minimumTemperature()).isEqualTo(21.0);
        assertThat(forecast.maximumTemperature()).isEqualTo(28.0);
        assertThat(forecast.rainProbability()).isEqualTo(60);
        assertThat(forecast.weatherCondition()).isEqualTo(WeatherCondition.RAIN);
        server.verify();
    }

    @Test
    void returnsUnavailableWhenMidTermFieldsAreIncomplete() {
        server.expect(requestTo(containsString("/getMidLandFcst")))
                .andRespond(withSuccess(midResponse("""
                        {"regId":"11B00000","rnSt5Am":30,"rnSt5Pm":60}
                        """), MediaType.APPLICATION_JSON));
        server.expect(requestTo(containsString("/getMidTa")))
                .andRespond(withSuccess(midResponse("""
                        {"regId":"11B10101","taMin5":21,"taMax5":28}
                        """), MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getMidTerm(
                WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                LocalDate.of(2026, 9, 2)
        ))
                .isInstanceOf(WeatherForecastUnavailableException.class)
                .hasMessageContaining("완전하게");

        server.verify();
    }

    @Test
    void returnsUnavailableForDayTenBeforeNewMidTermForecastIsPublished() {
        KmaWeatherClient beforeMorningIssue = newClient(
                restClient,
                fixedClock("2026-08-27T20:00:00Z")
        );

        assertThatThrownBy(() -> beforeMorningIssue.getMidTerm(
                WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                LocalDate.of(2026, 9, 7)
        ))
                .isInstanceOf(WeatherForecastUnavailableException.class)
                .hasMessageContaining("2026-09-07");

        server.verify();
    }

    @Test
    void usesNewMorningIssueForDayTenAfterPublicationDelay() {
        KmaWeatherClient afterMorningIssue = newClient(
                restClient,
                fixedClock("2026-08-27T21:10:00Z")
        );
        server.expect(requestTo(allOf(
                        containsString("https://weather.test/mid/getMidLandFcst"),
                        containsString("tmFc=202608280600")
                )))
                .andRespond(withSuccess(midResponse("""
                        {"regId":"11B00000","rnSt10":40,"wf10":"구름많음"}
                        """), MediaType.APPLICATION_JSON));
        server.expect(requestTo(allOf(
                        containsString("https://weather.test/mid/getMidTa"),
                        containsString("tmFc=202608280600")
                )))
                .andRespond(withSuccess(midResponse("""
                        {"regId":"11B10101","taMin10":20,"taMax10":29}
                        """), MediaType.APPLICATION_JSON));

        var forecast = afterMorningIssue.getMidTerm(
                WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                LocalDate.of(2026, 9, 7)
        );

        assertThat(forecast.minimumTemperature()).isEqualTo(20.0);
        assertThat(forecast.maximumTemperature()).isEqualTo(29.0);
        assertThat(forecast.rainProbability()).isEqualTo(40);
        server.verify();
    }

    @Test
    void returnsUnavailableForDayThreeMissingFromLatestShortTermIssue() {
        KmaWeatherClient beforeFirstShortIssue = newClient(
                restClient,
                fixedClock("2026-08-27T16:00:00Z")
        );
        server.expect(requestTo(allOf(
                        containsString("base_date=20260827"),
                        containsString("base_time=2300")
                )))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {"resultCode": "00", "resultMsg": "NORMAL_SERVICE"},
                            "body": {
                              "totalCount": 1,
                              "items": {"item": [
                                {"category":"TMP","fcstDate":"20260830","fcstTime":"1500","fcstValue":"28","nx":60,"ny":127}
                              ]}
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> beforeFirstShortIssue.getShortTerm(
                37.5665,
                126.9780,
                LocalDate.of(2026, 8, 31),
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        ))
                .isInstanceOf(WeatherForecastUnavailableException.class)
                .hasMessageContaining("2026-08-31");

        server.verify();
    }

    @Test
    void rejectsProviderErrorResultCode() {
        server.expect(requestTo(containsString("/getVilageFcst")))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {"resultCode": "03", "resultMsg": "NO_DATA"}
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getShortTerm(
                37.5665,
                126.9780,
                LocalDate.of(2026, 8, 29),
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        )).isInstanceOf(WeatherProviderException.class);

        server.verify();
    }

    @Test
    void returnsUnavailableWhenProviderItemsAreEmpty() {
        server.expect(requestTo(containsString("/getVilageFcst")))
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {"resultCode": "00", "resultMsg": "NORMAL_SERVICE"},
                            "body": {"totalCount": 0, "items": {"item": []}}
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() -> client.getShortTerm(
                37.5665,
                126.9780,
                LocalDate.of(2026, 8, 29),
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        )).isInstanceOf(WeatherForecastUnavailableException.class);

        server.verify();
    }

    @Test
    void rejectsRequestWhenShortTermApiKeyIsMissing() {
        properties.getShort().setApiKey(" ");

        assertThatThrownBy(() -> client.getShortTerm(
                37.5665,
                126.9780,
                LocalDate.of(2026, 8, 29),
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        ))
                .isInstanceOf(WeatherProviderException.class)
                .hasMessageContaining("인증키");

        server.verify();
    }

    @Test
    void distinguishesProviderTimeout() {
        RestClient timeoutRestClient = RestClient.builder()
                .requestFactory((uri, httpMethod) -> {
                    throw new SocketTimeoutException("Read timed out");
                })
                .build();
        KmaWeatherClient timeoutClient = newClient(
                timeoutRestClient,
                fixedClock("2026-08-28T03:00:00Z")
        );

        assertThatThrownBy(() -> timeoutClient.getShortTerm(
                37.5665,
                126.9780,
                LocalDate.of(2026, 8, 29),
                WeatherRegion.SEOUL_INCHEON_GYEONGGI
        )).isInstanceOf(WeatherProviderTimeoutException.class);
    }

    @Test
    void upgradesHttpSeasonalRssUrlToHttps() {
        assertSeasonalRssUrl(
                "http://www.kma.go.kr/repositary/xml/fct/mon/img/fct_mon3rss_108_20260824.xml",
                "https://www.kma.go.kr/repositary/xml/fct/mon/img/fct_mon3rss_108_20260824.xml"
        );
    }

    @Test
    void keepsHttpsSeasonalRssUrl() {
        assertSeasonalRssUrl(
                "https://www.kma.go.kr/repositary/xml/fct/mon/img/fct_mon3rss_108_20260824.xml",
                "https://www.kma.go.kr/repositary/xml/fct/mon/img/fct_mon3rss_108_20260824.xml"
        );
    }

    private void assertSeasonalRssUrl(String guideRssUrl, String expectedRequestUrl) {
        properties.setSeasonalGuideUrl("https://weather.test/seasonal-guide");
        server.expect(requestTo("https://weather.test/seasonal-guide"))
                .andRespond(withSuccess(
                        "<html><body><a href=\"" + guideRssUrl + "\">3개월전망 RSS</a></body></html>",
                        MediaType.TEXT_HTML
                ));
        server.expect(requestTo(expectedRequestUrl))
                .andRespond(withSuccess(seasonalResponse(), MediaType.APPLICATION_XML));

        var forecast = client.getSeasonalOutlook(
                WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                LocalDate.of(2026, 9, 15)
        );

        assertThat(forecast.forecastType()).isEqualTo(ForecastType.SEASONAL_OUTLOOK);
        assertThat(forecast.targetDate()).isEqualTo(LocalDate.of(2026, 9, 15));
        server.verify();
    }

    private String midResponse(String item) {
        return """
                {
                  "response": {
                    "header": {"resultCode": "00", "resultMsg": "NORMAL_SERVICE"},
                    "body": {
                      "totalCount": 1,
                      "items": {"item": [%s]}
                    }
                  }
                }
                """.formatted(item);
    }

    private String seasonalResponse() {
        return """
                <?xml version="1.0" encoding="UTF-8"?>
                <rss><channel><item><description>
                  <header><ydate>2026년 8월 24일 11시</ydate></header>
                  <threemonth_ta_forecast><local_ta>
                    <local_ta_name><![CDATA[ 서울ㆍ인천ㆍ경기도 ]]></local_ta_name>
                    <month1_local_ta_minVal>20</month1_local_ta_minVal>
                    <month1_local_ta_similarVal>40</month1_local_ta_similarVal>
                    <month1_local_ta_maxVal>40</month1_local_ta_maxVal>
                  </local_ta></threemonth_ta_forecast>
                  <threemonth_rn_forecast><local_rn>
                    <local_rn_name><![CDATA[ 서울ㆍ인천ㆍ경기도 ]]></local_rn_name>
                    <month1_local_rn_minVal>20</month1_local_rn_minVal>
                    <month1_local_rn_similarVal>50</month1_local_rn_similarVal>
                    <month1_local_rn_maxVal>30</month1_local_rn_maxVal>
                  </local_rn></threemonth_rn_forecast>
                </description></item></channel></rss>
                """;
    }

    private KmaWeatherClient newClient(RestClient clientRestClient, Clock clock) {
        return new KmaWeatherClient(
                clientRestClient,
                properties,
                new KmaGridConverter(),
                new KmaWeatherMapper(),
                new KmaSeasonalOutlookParser(),
                clock
        );
    }

    @Test
    void treatsMissingSeasonalRssLinkAsProviderFailure() {
        properties.setSeasonalGuideUrl("https://weather.test/seasonal-guide");
        server.expect(requestTo("https://weather.test/seasonal-guide"))
                .andRespond(withSuccess("<html><body>링크 없음</body></html>", MediaType.TEXT_HTML));

        assertThatThrownBy(() -> client.getSeasonalOutlook(
                WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                LocalDate.of(2026, 9, 15)
        )).isInstanceOf(WeatherProviderException.class);

        server.verify();
    }

    @Test
    void treatsMalformedSeasonalXmlAsProviderFailure() {
        properties.setSeasonalGuideUrl("https://weather.test/seasonal-guide");
        String rssUrl = "https://www.kma.go.kr/repositary/xml/fct/mon/img/fct_mon3rss_108_20260824.xml";
        server.expect(requestTo("https://weather.test/seasonal-guide"))
                .andRespond(withSuccess(
                        "<html><body><a href=\"" + rssUrl + "\">3개월전망 RSS</a></body></html>",
                        MediaType.TEXT_HTML
                ));
        server.expect(requestTo(rssUrl))
                .andRespond(withSuccess("<not-closed>", MediaType.APPLICATION_XML));

        assertThatThrownBy(() -> client.getSeasonalOutlook(
                WeatherRegion.SEOUL_INCHEON_GYEONGGI,
                LocalDate.of(2026, 9, 15)
        )).isInstanceOf(WeatherProviderException.class);

        server.verify();
    }

    private Clock fixedClock(String instant) {
        return Clock.fixed(Instant.parse(instant), ZoneId.of("Asia/Seoul"));
    }
}

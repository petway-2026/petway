package com.petway.external.tour.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petway.external.tour.config.TourApiProperties;
import com.petway.external.tour.exception.TourApiErrorType;
import com.petway.external.tour.exception.TourApiException;
import com.petway.external.tour.mapper.TourPlaceMapper;
import com.petway.place.model.PlaceSearchCondition;
import com.petway.place.model.PlaceSearchResult;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.SocketTimeoutException;
import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TourApiClientTest {

    private static final String BASE_URL =
            "https://apis.data.go.kr/B551011/KorService2";

    @Test
    void sendsRequiredAndOptionalAreaSearchParameters() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TourApiClient client = createClient(
                builder.baseUrl(BASE_URL).build(),
                "test-key"
        );

        server.expect(request -> {
                    assertThat(request.getURI().getPath())
                            .isEqualTo("/B551011/KorService2/areaBasedList2");

                    var params = UriComponentsBuilder
                            .fromUri(request.getURI())
                            .build()
                            .getQueryParams();

                    assertThat(params.getFirst("serviceKey")).isEqualTo("test-key");
                    assertThat(params.getFirst("MobileOS")).isEqualTo("ETC");
                    assertThat(params.getFirst("MobileApp")).isEqualTo("PetWay");
                    assertThat(params.getFirst("_type")).isEqualTo("json");
                    assertThat(params.getFirst("numOfRows")).isEqualTo("20");
                    assertThat(params.getFirst("pageNo")).isEqualTo("1");
                    assertThat(params.getFirst("arrange")).isEqualTo("A");
                    assertThat(params.getFirst("areaCode")).isEqualTo("1");
                    assertThat(params.getFirst("sigunguCode")).isEqualTo("1");
                    assertThat(params.getFirst("contentTypeId")).isEqualTo("12");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        PlaceSearchResult result = client.searchByArea(
                new PlaceSearchCondition("1", "1", "12", 1, 20)
        );

        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.items()).extracting("contentId")
                .containsExactly("125266");
        server.verify();
    }

    @Test
    void doesNotSendEmptyOptionalParameters() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TourApiClient client = createClient(
                builder.baseUrl(BASE_URL).build(),
                "test-key"
        );

        server.expect(request -> {
                    var params = UriComponentsBuilder
                            .fromUri(request.getURI())
                            .build()
                            .getQueryParams();

                    assertThat(params).doesNotContainKeys(
                            "sigunguCode",
                            "contentTypeId"
                    );
                })
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        client.searchByArea(
                new PlaceSearchCondition("1", null, null, 1, 20)
        );

        server.verify();
    }

    @Test
    void failsBeforeRequestWhenServiceKeyIsMissing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TourApiClient client = createClient(
                builder.baseUrl(BASE_URL).build(),
                " "
        );

        assertThatThrownBy(() -> client.searchByArea(
                new PlaceSearchCondition("1", null, null, 1, 20)
        ))
                .isInstanceOf(TourApiException.class)
                .extracting(exception -> ((TourApiException) exception).getErrorType())
                .isEqualTo(TourApiErrorType.CONFIGURATION);

        server.verify();
    }

    @Test
    void convertsHttpFailureToTourApiException() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TourApiClient client = createClient(
                builder.baseUrl(BASE_URL).build(),
                "test-key"
        );

        server.expect(request -> {
                })
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.searchByArea(
                new PlaceSearchCondition("1", null, null, 1, 20)
        ))
                .isInstanceOf(TourApiException.class)
                .extracting(exception -> ((TourApiException) exception).getErrorType())
                .isEqualTo(TourApiErrorType.HTTP_ERROR);

        server.verify();
    }

    @Test
    void convertsSocketTimeoutToTimeoutError() {
        RestClient restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory((uri, httpMethod) -> {
                    throw new SocketTimeoutException("timed out");
                })
                .build();
        TourApiClient client = createClient(restClient, "test-key");

        assertThatThrownBy(() -> client.searchByArea(
                new PlaceSearchCondition("1", null, null, 1, 20)
        ))
                .isInstanceOf(TourApiException.class)
                .extracting(exception -> ((TourApiException) exception).getErrorType())
                .isEqualTo(TourApiErrorType.TIMEOUT);
    }

    private TourApiClient createClient(RestClient restClient, String serviceKey) {
        TourApiProperties properties = new TourApiProperties(
                BASE_URL,
                serviceKey,
                "ETC",
                "PetWay",
                20,
                Duration.ofSeconds(3),
                Duration.ofSeconds(5)
        );

        return new TourApiClient(
                restClient,
                properties,
                new TourPlaceMapper(new ObjectMapper())
        );
    }

    private String successResponse() {
        return """
                {
                  "response": {
                    "header": {
                      "resultCode": "0000",
                      "resultMsg": "OK"
                    },
                    "body": {
                      "items": {
                        "item": [
                          {
                            "contentid": "125266",
                            "contenttypeid": "12",
                            "title": "경복궁",
                            "addr1": "서울특별시 종로구 사직로 161",
                            "mapx": "126.9769930325",
                            "mapy": "37.5788222356",
                            "firstimage": "https://example.com/image.jpg"
                          }
                        ]
                      },
                      "numOfRows": 20,
                      "pageNo": 1,
                      "totalCount": 1
                    }
                  }
                }
                """;
    }
}

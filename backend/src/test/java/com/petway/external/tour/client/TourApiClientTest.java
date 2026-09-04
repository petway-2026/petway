package com.petway.external.tour.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petway.external.tour.config.TourApiProperties;
import com.petway.external.tour.exception.TourApiErrorType;
import com.petway.external.tour.exception.TourApiException;
import com.petway.external.tour.mapper.TourPlaceMapper;
import com.petway.place.model.PlaceSearchCondition;
import com.petway.place.model.PlaceSearchResult;
import com.petway.place.model.NearbyPlaceSearchCondition;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.SocketTimeoutException;
import java.time.Clock;
import java.time.Duration;
import java.math.BigDecimal;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;

class TourApiClientTest {

    private static final String BASE_URL =
            "https://apis.data.go.kr/B551011/KorPetTourService2";

    @Test
    void sendsRequiredAndOptionalAreaSearchParameters() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TourApiClient client = createClient(
                builder.baseUrl(BASE_URL).build(),
                "test+/=key"
        );

        server.expect(request -> {
                    assertThat(request.getURI().getPath())
                            .isEqualTo("/B551011/KorPetTourService2/areaBasedList2");

                    var params = UriComponentsBuilder
                            .fromUri(request.getURI())
                            .build()
                            .getQueryParams();

                    assertThat(params.getFirst("serviceKey"))
                            .isEqualTo("test%2B%2F%3Dkey");
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
                new TourPlaceMapper(new ObjectMapper()),
                new TourPetDetailCache(Clock.systemUTC()),
                new TourNearbySearchCache(Clock.systemUTC())
        );
    }

    private NearbyPlaceSearchCondition nearbyCondition() {
        return new NearbyPlaceSearchCondition(
                new BigDecimal("37.5665"),
                new BigDecimal("126.9780"),
                5000,
                1,
                100
        );
    }

    private void assertPetDetailFailure(TourApiClient client, TourApiErrorType errorType) {
        assertThatThrownBy(() -> client.getPetDetail("100"))
                .isInstanceOf(TourApiException.class)
                .extracting(exception -> ((TourApiException) exception).getErrorType())
                .isEqualTo(errorType);
    }

    private String petDetailResponse(boolean empty) {
        String items = empty
                ? "{\"item\": []}"
                : "{\"item\": [{\"contentid\": \"100\", \"acmpyNeedMtr\": \"목줄\"}]}";
        return """
                {
                  "response": {
                    "header": {"resultCode": "0000", "resultMsg": "OK"},
                    "body": {"items": %s}
                  }
                }
                """.formatted(items);
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

    @Test
    void cachesSuccessfulPetDetailByContentId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TourApiClient client = createClient(builder.baseUrl(BASE_URL).build(), "test-key");

        server.expect(request -> assertThat(request.getURI().getPath())
                        .endsWith("/detailPetTour2"))
                .andRespond(withSuccess(petDetailResponse(false), MediaType.APPLICATION_JSON));

        assertThat(client.getPetDetail("100").requiredItems()).isEqualTo("목줄");
        assertThat(client.getPetDetail("100").requiredItems()).isEqualTo("목줄");

        server.verify();
    }

    @Test
    void coalescesConcurrentPetDetailRequestsByContentId() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TourApiClient client = createClient(builder.baseUrl(BASE_URL).build(), "test-key");
        CountDownLatch start = new CountDownLatch(1);

        server.expect(request -> assertThat(request.getURI().getPath())
                        .endsWith("/detailPetTour2"))
                .andRespond(request -> {
                    try {
                        Thread.sleep(100);
                    } catch (InterruptedException exception) {
                        Thread.currentThread().interrupt();
                    }
                    return withSuccess(petDetailResponse(false), MediaType.APPLICATION_JSON)
                            .createResponse(request);
                });

        CompletableFuture<String> first = CompletableFuture.supplyAsync(() -> {
            await(start);
            return client.getPetDetail("100").requiredItems();
        });
        CompletableFuture<String> second = CompletableFuture.supplyAsync(() -> {
            await(start);
            return client.getPetDetail("100").requiredItems();
        });
        start.countDown();

        assertThat(first.join()).isEqualTo("목줄");
        assertThat(second.join()).isEqualTo("목줄");
        server.verify();
    }

    @Test
    void cachesSuccessfulNearbySearch() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TourApiClient client = createClient(builder.baseUrl(BASE_URL).build(), "test-key");

        server.expect(request -> assertThat(request.getURI().getPath())
                        .endsWith("/locationBasedList2"))
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        assertThat(client.searchNearby(nearbyCondition()).items()).hasSize(1);
        assertThat(client.searchNearby(nearbyCondition()).items()).hasSize(1);
        server.verify();
    }

    @Test
    void doesNotCacheNearbyRateLimit() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TourApiClient client = createClient(builder.baseUrl(BASE_URL).build(), "test-key");

        server.expect(request -> { }).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(request -> { }).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertThatThrownBy(() -> client.searchNearby(nearbyCondition()))
                .isInstanceOf(TourApiException.class)
                .extracting(exception -> ((TourApiException) exception).getErrorType())
                .isEqualTo(TourApiErrorType.RATE_LIMITED);
        assertThatThrownBy(() -> client.searchNearby(nearbyCondition()))
                .isInstanceOf(TourApiException.class)
                .extracting(exception -> ((TourApiException) exception).getErrorType())
                .isEqualTo(TourApiErrorType.RATE_LIMITED);
        server.verify();
    }

    @Test
    void doesNotCacheNearbyServerFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TourApiClient client = createClient(builder.baseUrl(BASE_URL).build(), "test-key");

        server.expect(request -> { }).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(request -> { }).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertThatThrownBy(() -> client.searchNearby(nearbyCondition()))
                .isInstanceOf(TourApiException.class);
        assertThatThrownBy(() -> client.searchNearby(nearbyCondition()))
                .isInstanceOf(TourApiException.class);
        server.verify();
    }

    private void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(exception);
        }
    }

    @Test
    void cachesSuccessfulEmptyPetDetail() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TourApiClient client = createClient(builder.baseUrl(BASE_URL).build(), "test-key");

        server.expect(request -> assertThat(request.getURI().getPath())
                        .endsWith("/detailPetTour2"))
                .andRespond(withSuccess(petDetailResponse(true), MediaType.APPLICATION_JSON));

        assertThat(client.getPetDetail("100")).isEqualTo(com.petway.place.model.PlacePetDetail.empty());
        assertThat(client.getPetDetail("100")).isEqualTo(com.petway.place.model.PlacePetDetail.empty());

        server.verify();
    }

    @Test
    void distinguishesRateLimitAndDoesNotCacheIt() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TourApiClient client = createClient(builder.baseUrl(BASE_URL).build(), "test-key");

        server.expect(request -> { }).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));
        server.expect(request -> { }).andRespond(withStatus(HttpStatus.TOO_MANY_REQUESTS));

        assertPetDetailFailure(client, TourApiErrorType.RATE_LIMITED);
        assertPetDetailFailure(client, TourApiErrorType.RATE_LIMITED);
        server.verify();
    }

    @Test
    void doesNotCacheServerFailure() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        TourApiClient client = createClient(builder.baseUrl(BASE_URL).build(), "test-key");

        server.expect(request -> { }).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));
        server.expect(request -> { }).andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertPetDetailFailure(client, TourApiErrorType.HTTP_ERROR);
        assertPetDetailFailure(client, TourApiErrorType.HTTP_ERROR);
        server.verify();
    }
}

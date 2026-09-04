package com.petway.external.tour.client;

import com.petway.external.tour.config.TourApiProperties;
import com.petway.external.tour.dto.TourApiResponse;
import com.petway.external.tour.exception.TourApiErrorType;
import com.petway.external.tour.exception.TourApiException;
import com.petway.external.tour.mapper.TourPlaceMapper;
import com.petway.place.model.PlaceSearchCondition;
import com.petway.place.model.PlaceSearchResult;
import com.petway.place.model.NearbyPlaceSearchCondition;
import com.petway.place.model.PlacePetDetail;
import com.petway.place.port.TourPlaceGateway;
import org.springframework.beans.factory.annotation.Qualifier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.http.HttpTimeoutException;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

@Component
public class TourApiClient implements TourPlaceGateway {

    private static final Logger log = LoggerFactory.getLogger(TourApiClient.class);

    private final RestClient tourApiRestClient;
    private final TourApiProperties properties;
    private final TourPlaceMapper tourPlaceMapper;
    private final TourPetDetailCache petDetailCache;
    private final TourNearbySearchCache nearbySearchCache;
    private final ConcurrentMap<String, CompletableFuture<PlacePetDetail>> petDetailInFlight =
            new ConcurrentHashMap<>();

    public TourApiClient(
            @Qualifier("tourApiRestClient") RestClient tourApiRestClient,
            TourApiProperties properties,
            TourPlaceMapper tourPlaceMapper,
            TourPetDetailCache petDetailCache,
            TourNearbySearchCache nearbySearchCache
    ) {
        this.tourApiRestClient = tourApiRestClient;
        this.properties = properties;
        this.tourPlaceMapper = tourPlaceMapper;
        this.petDetailCache = petDetailCache;
        this.nearbySearchCache = nearbySearchCache;
    }

    @Override
    public PlaceSearchResult searchByArea(PlaceSearchCondition condition) {
        validateConfiguration();

        try {
            TourApiResponse response = tourApiRestClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder
                                .path("/areaBasedList2")
                                .queryParam("serviceKey", "{serviceKey}")
                                .queryParam("MobileOS", properties.mobileOs())
                                .queryParam("MobileApp", properties.mobileApp())
                                .queryParam("_type", "json")
                                .queryParam("numOfRows", condition.size())
                                .queryParam("pageNo", condition.page())
                                .queryParam("arrange", "A")
                                .queryParam("areaCode", condition.areaCode());

                        if (hasText(condition.sigunguCode())) {
                            uriBuilder.queryParam("sigunguCode", condition.sigunguCode());
                        }
                        if (hasText(condition.contentTypeId())) {
                            uriBuilder.queryParam("contentTypeId", condition.contentTypeId());
                        }

                        return uriBuilder.build(properties.serviceKey());
                    })
                    .retrieve()
                    .body(TourApiResponse.class);

            return tourPlaceMapper.toPlaceSearchResult(response);
        } catch (ResourceAccessException e) {
            if (hasTimeoutCause(e)) {
                throw new TourApiException(
                        TourApiErrorType.TIMEOUT,
                        "관광정보 조회 시간이 초과되었습니다.",
                        e
                );
            }
            throw new TourApiException(
                    TourApiErrorType.CONNECTION,
                    "관광정보 제공기관에 연결할 수 없습니다.",
                    e
            );
        } catch (RestClientResponseException e) {
            log.warn("Tour API HTTP request failed: status={}", e.getStatusCode());
            throw new TourApiException(
                    TourApiErrorType.HTTP_ERROR,
                    "관광정보 제공기관 호출에 실패했습니다.",
                    e
            );
        } catch (TourApiException e) {
            throw e;
        } catch (RestClientException e) {
            throw new TourApiException(
                    TourApiErrorType.CONNECTION,
                    "관광정보 제공기관에 연결할 수 없습니다.",
                    e
            );
        }
    }

    @Override
    public PlaceSearchResult searchNearby(NearbyPlaceSearchCondition condition) {
        validateConfiguration();
        PlaceSearchResult cached = nearbySearchCache.get(condition).orElse(null);
        if (cached != null) {
            return cached;
        }

        try {
            TourApiResponse response = tourApiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/locationBasedList2")
                            .queryParam("serviceKey", "{serviceKey}")
                            .queryParam("MobileOS", properties.mobileOs())
                            .queryParam("MobileApp", properties.mobileApp())
                            .queryParam("_type", "json")
                            .queryParam("numOfRows", condition.size())
                            .queryParam("pageNo", condition.page())
                            .queryParam("arrange", "E")
                            .queryParam("mapX", condition.longitude())
                            .queryParam("mapY", condition.latitude())
                            .queryParam("radius", condition.radius())
                            .build(properties.serviceKey()))
                    .retrieve()
                    .body(TourApiResponse.class);

            PlaceSearchResult result = tourPlaceMapper.toPlaceSearchResult(response);
            nearbySearchCache.put(condition, result);
            return result;
        } catch (ResourceAccessException e) {
            if (hasTimeoutCause(e)) {
                throw new TourApiException(
                        TourApiErrorType.TIMEOUT,
                        "주변 반려동물 동반 장소 조회 시간이 초과되었습니다.",
                        e
                );
            }
            throw new TourApiException(
                    TourApiErrorType.CONNECTION,
                    "반려동물 관광정보 제공기관에 연결할 수 없습니다.",
                    e
            );
        } catch (RestClientResponseException e) {
            log.warn("Tour API nearby request failed: status={}", e.getStatusCode());
            if (e.getStatusCode().value() == 429) {
                throw new TourApiException(
                        TourApiErrorType.RATE_LIMITED,
                        "주변 장소 제공기관의 일시적인 요청 한도로 정보를 불러오지 못했습니다.",
                        e
                );
            }
            throw new TourApiException(
                    TourApiErrorType.HTTP_ERROR,
                    "주변 반려동물 동반 장소 조회에 실패했습니다.",
                    e
            );
        } catch (TourApiException e) {
            throw e;
        } catch (RestClientException e) {
            throw new TourApiException(
                    TourApiErrorType.CONNECTION,
                    "반려동물 관광정보 제공기관에 연결할 수 없습니다.",
                    e
            );
        }
    }

    @Override
    public PlacePetDetail getPetDetail(String contentId) {
        validateConfiguration();
        PlacePetDetail cached = petDetailCache.get(contentId).orElse(null);
        if (cached != null) {
            return cached;
        }
        CompletableFuture<PlacePetDetail> request = new CompletableFuture<>();
        CompletableFuture<PlacePetDetail> existing = petDetailInFlight.putIfAbsent(contentId, request);
        if (existing != null) {
            try {
                return existing.join();
            } catch (CompletionException exception) {
                if (exception.getCause() instanceof RuntimeException runtimeException) {
                    throw runtimeException;
                }
                throw exception;
            }
        }
        try {
            TourApiResponse response = tourApiRestClient.get()
                    .uri(uriBuilder -> uriBuilder
                            .path("/detailPetTour2")
                            .queryParam("serviceKey", "{serviceKey}")
                            .queryParam("MobileOS", properties.mobileOs())
                            .queryParam("MobileApp", properties.mobileApp())
                            .queryParam("_type", "json")
                            .queryParam("contentId", contentId)
                            .build(properties.serviceKey()))
                    .retrieve()
                    .body(TourApiResponse.class);
            PlacePetDetail detail = tourPlaceMapper.toPlacePetDetail(response);
            petDetailCache.put(contentId, detail);
            request.complete(detail);
            return detail;
        } catch (RestClientResponseException e) {
            if (e.getStatusCode().value() == 429) {
                log.warn("Tour API pet detail rate limited: contentId={}", contentId);
                TourApiException exception = new TourApiException(
                        TourApiErrorType.RATE_LIMITED,
                        "관광정보 제공기관의 일시적인 요청 한도로 정보를 불러오지 못했습니다.",
                        e
                );
                request.completeExceptionally(exception);
                throw exception;
            }
            TourApiException exception = new TourApiException(TourApiErrorType.HTTP_ERROR, "반려동물 동반 상세정보 조회에 실패했습니다.", e);
            request.completeExceptionally(exception);
            throw exception;
        } catch (ResourceAccessException e) {
            TourApiException exception = new TourApiException(TourApiErrorType.CONNECTION, "반려동물 관광정보 제공기관에 연결할 수 없습니다.", e);
            request.completeExceptionally(exception);
            throw exception;
        } catch (RestClientException e) {
            TourApiException exception = new TourApiException(TourApiErrorType.CONNECTION, "반려동물 관광정보 제공기관에 연결할 수 없습니다.", e);
            request.completeExceptionally(exception);
            throw exception;
        } catch (RuntimeException e) {
            request.completeExceptionally(e);
            throw e;
        } finally {
            petDetailInFlight.remove(contentId, request);
        }
    }

    private void validateConfiguration() {
        if (!hasText(properties.serviceKey())) {
            throw new TourApiException(
                    TourApiErrorType.CONFIGURATION,
                    "관광정보 API 인증키가 설정되지 않았습니다."
            );
        }
    }

    private boolean hasTimeoutCause(Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof SocketTimeoutException
                    || current instanceof HttpTimeoutException) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }

    private boolean hasText(String value) {
        return value != null && !value.isBlank();
    }
}

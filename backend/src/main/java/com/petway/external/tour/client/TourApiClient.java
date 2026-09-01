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

@Component
public class TourApiClient implements TourPlaceGateway {

    private static final Logger log = LoggerFactory.getLogger(TourApiClient.class);

    private final RestClient tourApiRestClient;
    private final TourApiProperties properties;
    private final TourPlaceMapper tourPlaceMapper;

    public TourApiClient(
            @Qualifier("tourApiRestClient") RestClient tourApiRestClient,
            TourApiProperties properties,
            TourPlaceMapper tourPlaceMapper
    ) {
        this.tourApiRestClient = tourApiRestClient;
        this.properties = properties;
        this.tourPlaceMapper = tourPlaceMapper;
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

            return tourPlaceMapper.toPlaceSearchResult(response);
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
            return tourPlaceMapper.toPlacePetDetail(response);
        } catch (TourApiException e) {
            throw e;
        } catch (RestClientResponseException e) {
            throw new TourApiException(TourApiErrorType.HTTP_ERROR, "반려동물 동반 상세정보 조회에 실패했습니다.", e);
        } catch (ResourceAccessException e) {
            throw new TourApiException(TourApiErrorType.CONNECTION, "반려동물 관광정보 제공기관에 연결할 수 없습니다.", e);
        } catch (RestClientException e) {
            throw new TourApiException(TourApiErrorType.CONNECTION, "반려동물 관광정보 제공기관에 연결할 수 없습니다.", e);
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

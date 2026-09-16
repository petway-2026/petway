/* 동물등록 조회 조건을 공공 API로 보내고, 통신·응답 오류를 내부 예외로 변환한다. */
package com.petway.external.animalregistration.client;

import com.petway.external.animalregistration.config.AnimalRegistrationApiProperties;
import com.petway.external.animalregistration.dto.AnimalRegistrationApiResponse;
import com.petway.external.animalregistration.exception.AnimalRegistrationErrorType;
import com.petway.external.animalregistration.exception.AnimalRegistrationException;
import com.petway.external.animalregistration.mapper.AnimalRegistrationMapper;
import com.petway.pet.model.AnimalRegistrationLookupCondition;
import com.petway.pet.model.RegisteredPetProfile;
import com.petway.pet.port.AnimalRegistrationGateway;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestClientResponseException;

import java.net.SocketTimeoutException;
import java.net.URLDecoder;
import java.net.http.HttpTimeoutException;
import java.nio.charset.StandardCharsets;

@Component
public class AnimalRegistrationApiClient implements AnimalRegistrationGateway {

    private static final Logger log =
            LoggerFactory.getLogger(AnimalRegistrationApiClient.class);

    private final RestClient restClient;
    private final AnimalRegistrationApiProperties properties;
    private final AnimalRegistrationMapper mapper;

    public AnimalRegistrationApiClient(
            @Qualifier("animalRegistrationRestClient") RestClient restClient,
            AnimalRegistrationApiProperties properties,
            AnimalRegistrationMapper mapper
    ) {
        this.restClient = restClient;
        this.properties = properties;
        this.mapper = mapper;
    }

    @Override
    public RegisteredPetProfile lookup(AnimalRegistrationLookupCondition condition) {
        validateConfiguration();

        try {
            AnimalRegistrationApiResponse response = restClient.get()
                    .uri(uriBuilder -> {
                        uriBuilder
                                .path("/animalInfo_v3")
                                .queryParam("serviceKey", "{serviceKey}")
                                .queryParam("_type", "json");

                        addIfPresent(uriBuilder, "dog_reg_no", condition.animalRegistrationNumber());
                        addIfPresent(uriBuilder, "rfid_cd", condition.rfidCode());
                        addIfPresent(uriBuilder, "owner_nm", condition.ownerName());
                        addIfPresent(uriBuilder, "owner_birth", condition.ownerBirthDate());

                        return uriBuilder.build(decodedServiceKey());
                    })
                    .retrieve()
                    .body(AnimalRegistrationApiResponse.class);

            return mapper.toRegisteredPetProfile(response);
        } catch (AnimalRegistrationException e) {
            throw e;
        } catch (ResourceAccessException e) {
            if (hasTimeoutCause(e)) {
                throw new AnimalRegistrationException(
                        AnimalRegistrationErrorType.TIMEOUT,
                        "동물등록정보 조회 시간이 초과되었습니다.",
                        e
                );
            }
            throw new AnimalRegistrationException(
                    AnimalRegistrationErrorType.CONNECTION,
                    "동물등록정보 제공기관에 연결할 수 없습니다.",
                    e
            );
        } catch (RestClientResponseException e) {
            log.warn("Animal registration API HTTP request failed: status={}", e.getStatusCode());
            throw new AnimalRegistrationException(
                    AnimalRegistrationErrorType.HTTP_ERROR,
                    "동물등록정보 제공기관 호출에 실패했습니다.",
                    e
            );
        } catch (RestClientException e) {
            throw new AnimalRegistrationException(
                    AnimalRegistrationErrorType.INVALID_RESPONSE,
                    "동물등록정보 응답 형식이 올바르지 않습니다.",
                    e
            );
        }
    }

    private void addIfPresent(
            org.springframework.web.util.UriBuilder uriBuilder,
            String name,
            String value
    ) {
        if (value != null && !value.isBlank()) {
            uriBuilder.queryParam(name, value);
        }
    }

    private void validateConfiguration() {
        if (properties.getServiceKey() == null
                || properties.getServiceKey().isBlank()) {
            throw new AnimalRegistrationException(
                    AnimalRegistrationErrorType.CONFIGURATION,
                    "동물등록정보 API 인증키가 설정되지 않았습니다."
            );
        }
    }

    private String decodedServiceKey() {
        String serviceKey = properties.getServiceKey().trim();
        return serviceKey.contains("%")
                ? URLDecoder.decode(serviceKey, StandardCharsets.UTF_8)
                : serviceKey;
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
}

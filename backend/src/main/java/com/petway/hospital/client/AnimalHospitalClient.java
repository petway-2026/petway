package com.petway.hospital.client;

import com.petway.hospital.client.dto.AnimalHospitalApiResponse;
import com.petway.hospital.config.AnimalHospitalApiProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;
import org.springframework.web.util.UriUtils;

import java.net.URI;
import java.nio.charset.StandardCharsets;

@Component
public class AnimalHospitalClient {

    private static final String ACTIVE_STATUS_QUERY =
            "cond%5BSALS_STTS_CD%3A%3AEQ%5D=01";

    private final RestClient restClient;
    private final AnimalHospitalApiProperties properties;

    @Autowired
    public AnimalHospitalClient(
            RestClient.Builder builder,
            AnimalHospitalApiProperties properties
    ) {
        this(
                builder.requestFactory(requestFactory(properties)).build(),
                properties
        );
    }

    AnimalHospitalClient(
            RestClient restClient,
            AnimalHospitalApiProperties properties
    ) {
        this.restClient = restClient;
        this.properties = properties;
    }

    public AnimalHospitalApiResponse.Body findActiveHospitals(
            int page,
            int size,
            String address,
            String name
    ) {
        if (properties.serviceKey().isBlank()) {
            throw new AnimalHospitalApiException(
                    "동물병원 API 인증키가 설정되지 않았습니다."
            );
        }

        URI requestUri = createRequestUri(page, size, address, name);

        try {
            AnimalHospitalApiResponse apiResponse = restClient.get()
                    .uri(requestUri)
                    .retrieve()
                    .body(AnimalHospitalApiResponse.class);

            return validate(apiResponse);
        } catch (AnimalHospitalApiException exception) {
            throw exception;
        } catch (RestClientException exception) {
            // Do not retain the original exception: its message can contain
            // the request URI, including the service key.
            throw new AnimalHospitalApiException(
                    "동물병원 공공 API 호출에 실패했습니다."
            );
        }
    }

    private URI createRequestUri(
            int page,
            int size,
            String address,
            String name
    ) {
        StringBuilder query = new StringBuilder()
                .append("serviceKey=")
                .append(encodedServiceKey())
                .append("&pageNo=")
                .append(page)
                .append("&numOfRows=")
                .append(size)
                .append("&returnType=JSON&")
                .append(ACTIVE_STATUS_QUERY);

        appendQueryParameter(
                query,
                "cond%5BROAD_NM_ADDR%3A%3ALIKE%5D",
                address
        );
        appendQueryParameter(
                query,
                "cond%5BBPLC_NM%3A%3ALIKE%5D",
                name
        );

        return URI.create(
                properties.baseUrl() + "/info?" + query
        );
    }

    private String encodedServiceKey() {
        String serviceKey = properties.serviceKey();

        // The public-data portal exposes both encoded and decoded keys.
        // Preserve an already encoded key to avoid double encoding.
        return serviceKey.contains("%")
                ? serviceKey
                : UriUtils.encodeQueryParam(
                        serviceKey,
                        StandardCharsets.UTF_8
                );
    }

    private static void appendQueryParameter(
            StringBuilder query,
            String encodedName,
            String value
    ) {
        if (value == null || value.isBlank()) {
            return;
        }

        query.append('&')
                .append(encodedName)
                .append('=')
                .append(UriUtils.encodeQueryParam(
                        value.trim(),
                        StandardCharsets.UTF_8
                ));
    }

    private static AnimalHospitalApiResponse.Body validate(
            AnimalHospitalApiResponse apiResponse
    ) {
        if (apiResponse == null
                || apiResponse.response() == null
                || apiResponse.response().header() == null) {
            throw new AnimalHospitalApiException(
                    "동물병원 공공 API 응답 형식이 올바르지 않습니다."
            );
        }

        String resultCode = apiResponse.response().header().resultCode();
        if (!("0".equals(resultCode) || "00".equals(resultCode))) {
            throw new AnimalHospitalApiException(
                    "동물병원 공공 API가 오류를 반환했습니다."
            );
        }

        if (apiResponse.response().body() == null) {
            throw new AnimalHospitalApiException(
                    "동물병원 공공 API 응답 본문이 비어 있습니다."
            );
        }

        return apiResponse.response().body();
    }

    private static SimpleClientHttpRequestFactory requestFactory(
            AnimalHospitalApiProperties properties
    ) {
        SimpleClientHttpRequestFactory factory =
                new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(properties.connectTimeout());
        factory.setReadTimeout(properties.readTimeout());
        return factory;
    }
}

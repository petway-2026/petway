package com.petway.external.animalregistration.client;

import com.petway.external.animalregistration.config.AnimalRegistrationApiProperties;
import com.petway.external.animalregistration.exception.AnimalRegistrationErrorType;
import com.petway.external.animalregistration.exception.AnimalRegistrationException;
import com.petway.external.animalregistration.mapper.AnimalRegistrationMapper;
import com.petway.pet.model.AnimalRegistrationLookupCondition;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.ConnectException;
import java.net.SocketTimeoutException;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withStatus;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AnimalRegistrationApiClientTest {

    private static final String BASE_URL =
            "https://apis.data.go.kr/1543061/animalInfoSrvc_v3";

    @Test
    void sendsOnlySelectedOfficialLookupParameters() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnimalRegistrationApiClient client = createClient(
                builder.baseUrl(BASE_URL).build(),
                "test+/=key"
        );

        server.expect(request -> {
                    assertThat(request.getURI().getPath())
                            .isEqualTo("/1543061/animalInfoSrvc_v3/animalInfo_v3");

                    var params = UriComponentsBuilder
                            .fromUri(request.getURI())
                            .build()
                            .getQueryParams();

                    assertThat(params.getFirst("serviceKey"))
                            .isEqualTo("test%2B%2F%3Dkey");
                    assertThat(params.getFirst("dog_reg_no"))
                            .isEqualTo("410123456789012");
                    assertThat(params.getFirst("owner_nm"))
                            .isEqualTo("%ED%99%8D%EA%B8%B8%EB%8F%99");
                    assertThat(params.getFirst("_type")).isEqualTo("json");
                    assertThat(params).doesNotContainKeys("rfid_cd", "owner_birth");
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        var profile = client.lookup(
                new AnimalRegistrationLookupCondition(
                        "410123456789012",
                        null,
                        "홍길동",
                        null
                )
        );

        assertThat(profile.petName()).isEqualTo("보리");
        server.verify();
    }

    @Test
    void sendsRfidAndOwnerBirthDateAlternative() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnimalRegistrationApiClient client = createClient(
                builder.baseUrl(BASE_URL).build(),
                "test-key"
        );

        server.expect(request -> {
                    var params = UriComponentsBuilder
                            .fromUri(request.getURI())
                            .build()
                            .getQueryParams();
                    assertThat(params.getFirst("rfid_cd"))
                            .isEqualTo("900123456789012");
                    assertThat(params.getFirst("owner_birth")).isEqualTo("19900101");
                    assertThat(params).doesNotContainKeys("dog_reg_no", "owner_nm");
                })
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        client.lookup(
                new AnimalRegistrationLookupCondition(
                        null,
                        "900123456789012",
                        null,
                        "19900101"
                )
        );

        server.verify();
    }

    @Test
    void failsBeforeRequestWhenServiceKeyIsMissing() {
        RestClient.Builder builder = RestClient.builder();
        MockRestServiceServer server = MockRestServiceServer.bindTo(builder).build();
        AnimalRegistrationApiClient client = createClient(
                builder.baseUrl(BASE_URL).build(),
                " "
        );

        assertErrorType(
                () -> client.lookup(condition()),
                AnimalRegistrationErrorType.CONFIGURATION
        );
        server.verify();
    }

    @Test
    void convertsHttpAndInvalidJsonResponses() {
        RestClient.Builder httpBuilder = RestClient.builder();
        MockRestServiceServer httpServer = MockRestServiceServer.bindTo(httpBuilder).build();
        AnimalRegistrationApiClient httpClient = createClient(
                httpBuilder.baseUrl(BASE_URL).build(),
                "test-key"
        );
        httpServer.expect(request -> {
                })
                .andRespond(withStatus(HttpStatus.INTERNAL_SERVER_ERROR));

        assertErrorType(
                () -> httpClient.lookup(condition()),
                AnimalRegistrationErrorType.HTTP_ERROR
        );
        httpServer.verify();

        RestClient.Builder jsonBuilder = RestClient.builder();
        MockRestServiceServer jsonServer = MockRestServiceServer.bindTo(jsonBuilder).build();
        AnimalRegistrationApiClient jsonClient = createClient(
                jsonBuilder.baseUrl(BASE_URL).build(),
                "test-key"
        );
        jsonServer.expect(request -> {
                })
                .andRespond(withSuccess("not-json", MediaType.APPLICATION_JSON));

        assertErrorType(
                () -> jsonClient.lookup(condition()),
                AnimalRegistrationErrorType.INVALID_RESPONSE
        );
        jsonServer.verify();
    }

    @Test
    void convertsSocketTimeoutToTimeoutError() {
        RestClient restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory((uri, method) -> {
                    throw new SocketTimeoutException("timed out");
                })
                .build();

        assertErrorType(
                () -> createClient(restClient, "test-key").lookup(condition()),
                AnimalRegistrationErrorType.TIMEOUT
        );
    }

    @Test
    void convertsConnectionFailureToConnectionError() {
        RestClient restClient = RestClient.builder()
                .baseUrl(BASE_URL)
                .requestFactory((uri, method) -> {
                    throw new ConnectException("connection refused");
                })
                .build();

        assertErrorType(
                () -> createClient(restClient, "test-key").lookup(condition()),
                AnimalRegistrationErrorType.CONNECTION
        );
    }

    private AnimalRegistrationApiClient createClient(
            RestClient restClient,
            String serviceKey
    ) {
        AnimalRegistrationApiProperties properties =
                new AnimalRegistrationApiProperties();
        properties.setBaseUrl(BASE_URL);
        properties.setServiceKey(serviceKey);
        return new AnimalRegistrationApiClient(
                restClient,
                properties,
                new AnimalRegistrationMapper()
        );
    }

    private AnimalRegistrationLookupCondition condition() {
        return new AnimalRegistrationLookupCondition(
                "410123456789012",
                null,
                "홍길동",
                null
        );
    }

    private void assertErrorType(Runnable invocation, AnimalRegistrationErrorType type) {
        assertThatThrownBy(invocation::run)
                .isInstanceOf(AnimalRegistrationException.class)
                .extracting(error -> ((AnimalRegistrationException) error).getErrorType())
                .isEqualTo(type);
    }

    private String successResponse() {
        return """
                {
                  "response": {
                    "header": {
                      "reqNo": "request-number",
                      "resultCode": "00",
                      "resultMsg": "NORMAL SERVICE",
                      "errorMsg": null
                    },
                    "body": {
                      "item": {
                        "dogRegNo": "410123456789012",
                        "rfidCd": "900123456789012",
                        "rfidGubun": "Y",
                        "birthDt": "20210510",
                        "dogNm": "보리",
                        "sexNm": "수컷",
                        "kindNm": "말티즈",
                        "neuterYn": "Y",
                        "orgNm": "관할기관",
                        "regTm": "20210510",
                        "aprTm": "20210511"
                      }
                    }
                  }
                }
                """;
    }
}

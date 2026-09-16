/* AnimalHospitalClient의 동작을 검증하는 테스트다. 검증 대상: 행정안전부 동물병원 정보를 HTTP로 조회하고 내부 병원 목록으로 변환한다. */
package com.petway.hospital.client;

import com.petway.hospital.client.dto.AnimalHospitalApiResponse;
import com.petway.hospital.config.AnimalHospitalApiProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.MediaType;
import org.springframework.test.web.client.MockRestServiceServer;
import org.springframework.web.client.RestClient;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.test.web.client.ExpectedCount.once;
import static org.springframework.test.web.client.match.MockRestRequestMatchers.method;
import static org.springframework.test.web.client.response.MockRestResponseCreators.withSuccess;

class AnimalHospitalClientTest {

    private MockRestServiceServer server;
    private AnimalHospitalClient client;

    @BeforeEach
    void setUp() {
        RestClient.Builder builder = RestClient.builder();
        server = MockRestServiceServer.bindTo(builder).build();
        AnimalHospitalApiProperties properties =
                new AnimalHospitalApiProperties(
                        "https://hospital.test",
                        "encoded%2Fkey",
                        Duration.ofSeconds(1),
                        Duration.ofSeconds(1)
                );
        client = new AnimalHospitalClient(builder.build(), properties);
    }

    @Test
    void mapsActualResponseWrapperAndUppercaseFields() {
        server.expect(once(), request -> {
                    String rawQuery = request.getURI().getRawQuery();
                    assertThat(rawQuery).contains("serviceKey=encoded%2Fkey");
                    assertThat(rawQuery).contains("pageNo=1");
                    assertThat(rawQuery).contains("numOfRows=3");
                    assertThat(rawQuery).contains(
                            "cond%5BSALS_STTS_CD%3A%3AEQ%5D=01"
                    );
                    assertThat(rawQuery).contains(
                            "cond%5BROAD_NM_ADDR%3A%3ALIKE%5D="
                    );
                })
                .andExpect(method(HttpMethod.GET))
                .andRespond(withSuccess(successResponse(), MediaType.APPLICATION_JSON));

        AnimalHospitalApiResponse.Body body =
                client.findActiveHospitals(
                        1,
                        3,
                        "경기도 화성시",
                        null
                );

        assertThat(body.totalCount()).isEqualTo(1);
        assertThat(body.itemList()).singleElement().satisfies(item -> {
            assertThat(item.managementNumber()).isEqualTo("M-1");
            assertThat(item.businessName()).isEqualTo("테스트동물병원");
            assertThat(item.coordinateX()).isEqualTo("207822.739588256");
            assertThat(item.coordinateY()).isEqualTo("410789.35102542");
        });
        server.verify();
    }

    @Test
    void rejectsExternalResultCodeWithoutExposingUpstreamDetails() {
        server.expect(request -> {
                })
                .andRespond(withSuccess("""
                        {
                          "response": {
                            "header": {
                              "resultCode": "-4",
                              "resultMsg": "등록되지 않은 인증키"
                            }
                          }
                        }
                        """, MediaType.APPLICATION_JSON));

        assertThatThrownBy(() ->
                client.findActiveHospitals(1, 3, null, null)
        )
                .isInstanceOf(AnimalHospitalApiException.class)
                .hasMessage("동물병원 공공 API가 오류를 반환했습니다.")
                .hasMessageNotContaining("encoded");
    }

    private static String successResponse() {
        return """
                {
                  "response": {
                    "header": {
                      "resultCode": "0",
                      "resultMsg": "정상"
                    },
                    "body": {
                      "dataType": "JSON",
                      "numOfRows": 3,
                      "pageNo": 1,
                      "totalCount": 1,
                      "items": {
                        "item": [
                          {
                            "MNG_NO": "M-1",
                            "BPLC_NM": "테스트동물병원",
                            "ROAD_NM_ADDR": "경기도 화성시 테스트로 1",
                            "SALS_STTS_CD": "01",
                            "SALS_STTS_NM": "영업/정상",
                            "CRD_INFO_X": "207822.739588256",
                            "CRD_INFO_Y": "410789.35102542",
                            "UNEXPECTED_FIELD": "ignored"
                          }
                        ]
                      }
                    }
                  }
                }
                """;
    }
}

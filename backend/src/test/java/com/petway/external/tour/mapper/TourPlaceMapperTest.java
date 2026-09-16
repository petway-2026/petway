/* TourPlaceMapper의 동작을 검증하는 테스트다. 검증 대상: 관광공사 결과 코드를 확인하고 장소 목록·동반 상세를 내부 모델로 변환한다. */
package com.petway.external.tour.mapper;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.petway.external.tour.dto.TourApiResponse;
import com.petway.external.tour.exception.TourApiErrorType;
import com.petway.external.tour.exception.TourApiException;
import com.petway.place.model.PlaceSearchResult;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class TourPlaceMapperTest {

    private final ObjectMapper objectMapper = new ObjectMapper();
    private TourPlaceMapper mapper;

    @BeforeEach
    void setUp() {
        mapper = new TourPlaceMapper(objectMapper);
    }

    @Test
    void mapsTourApiResponseToPlaceSearchResult() throws Exception {
        TourApiResponse response = objectMapper.readValue("""
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
                            "areacode": "1",
                            "sigungucode": "1",
                            "title": "경복궁",
                            "addr1": "서울특별시 종로구 사직로 161",
                            "mapx": "126.9769930325",
                            "mapy": "37.5788222356",
                            "firstimage": "https://example.com/image.jpg",
                            "unknownField": "ignored"
                          }
                        ]
                      },
                      "numOfRows": 20,
                      "pageNo": 1,
                      "totalCount": 1
                    }
                  }
                }
                """, TourApiResponse.class);

        PlaceSearchResult result = mapper.toPlaceSearchResult(response);

        assertThat(result.page()).isEqualTo(1);
        assertThat(result.size()).isEqualTo(20);
        assertThat(result.totalCount()).isEqualTo(1);
        assertThat(result.items()).hasSize(1);
        assertThat(result.items().get(0).contentId()).isEqualTo("125266");
        assertThat(result.items().get(0).contentTypeId()).isEqualTo("12");
        assertThat(result.items().get(0).areaCode()).isEqualTo("1");
        assertThat(result.items().get(0).sigunguCode()).isEqualTo("1");
        assertThat(result.items().get(0).title()).isEqualTo("경복궁");
        assertThat(result.items().get(0).latitude())
                .isEqualByComparingTo("37.5788222356");
        assertThat(result.items().get(0).longitude())
                .isEqualByComparingTo("126.9769930325");
    }

    @Test
    void returnsEmptyItemsWhenTourApiUsesEmptyString() throws Exception {
        TourApiResponse response = objectMapper.readValue("""
                {
                  "response": {
                    "header": {
                      "resultCode": "0000",
                      "resultMsg": "OK"
                    },
                    "body": {
                      "items": "",
                      "numOfRows": 20,
                      "pageNo": 1,
                      "totalCount": 0
                    }
                  }
                }
                """, TourApiResponse.class);

        PlaceSearchResult result = mapper.toPlaceSearchResult(response);

        assertThat(result.items()).isEmpty();
        assertThat(result.totalCount()).isZero();
    }

    @Test
    void rejectsExternalFailureResultCode() throws Exception {
        TourApiResponse response = objectMapper.readValue("""
                {
                  "response": {
                    "header": {
                      "resultCode": "10",
                      "resultMsg": "INVALID_REQUEST_PARAMETER_ERROR"
                    },
                    "body": {}
                  }
                }
                """, TourApiResponse.class);

        assertThatThrownBy(() -> mapper.toPlaceSearchResult(response))
                .isInstanceOf(TourApiException.class)
                .extracting(exception -> ((TourApiException) exception).getErrorType())
                .isEqualTo(TourApiErrorType.API_ERROR);
    }

    @Test
    void rejectsItemWithoutRequiredIdentifier() throws Exception {
        TourApiResponse response = objectMapper.readValue("""
                {
                  "response": {
                    "header": {
                      "resultCode": "0000",
                      "resultMsg": "OK"
                    },
                    "body": {
                      "items": {
                        "item": {
                          "contenttypeid": "12",
                          "title": "식별자 없는 장소"
                        }
                      },
                      "numOfRows": 1,
                      "pageNo": 1,
                      "totalCount": 1
                    }
                  }
                }
                """, TourApiResponse.class);

        assertThatThrownBy(() -> mapper.toPlaceSearchResult(response))
                .isInstanceOf(TourApiException.class)
                .extracting(exception -> ((TourApiException) exception).getErrorType())
                .isEqualTo(TourApiErrorType.INVALID_RESPONSE);
    }

    @Test
    void rejectsMissingPaginationMetadata() throws Exception {
        TourApiResponse response = objectMapper.readValue("""
                {
                  "response": {
                    "header": {
                      "resultCode": "0000",
                      "resultMsg": "OK"
                    },
                    "body": {
                      "items": "",
                      "pageNo": 1,
                      "totalCount": 0
                    }
                  }
                }
                """, TourApiResponse.class);

        assertThatThrownBy(() -> mapper.toPlaceSearchResult(response))
                .isInstanceOf(TourApiException.class)
                .extracting(exception -> ((TourApiException) exception).getErrorType())
                .isEqualTo(TourApiErrorType.INVALID_RESPONSE);
    }

    @Test
    void mapsPetDetailFieldsWithoutChangingOfficialText() throws Exception {
        TourApiResponse response = objectMapper.readValue("""
                {
                  "response": {
                    "header": {"resultCode": "0000", "resultMsg": "OK"},
                    "body": {
                      "items": {
                        "item": [{
                          "acmpyTypeCd": "전구역 동반가능",
                          "acmpyPsblCpam": "10kg 이하 동반 가능",
                          "acmpyNeedMtr": "목줄 필수",
                          "etcAcmpyInfo": "현장 문의",
                          "relaAcdntRiskMtr": "맹견 입마개 필수",
                          "relaPosesFclty": "반려견 놀이터",
                          "relaFrnshPrdlst": "배변봉투",
                          "relaPurcPrdlst": "반려견 간식",
                          "relaRntlPrdlst": "반려견 유모차"
                        }]
                      }
                    }
                  }
                }
                """, TourApiResponse.class);

        var detail = mapper.toPlacePetDetail(response);

        assertThat(detail.accompanimentType()).isEqualTo("전구역 동반가능");
        assertThat(detail.allowedCapacity()).isEqualTo("10kg 이하 동반 가능");
        assertThat(detail.requiredItems()).isEqualTo("목줄 필수");
        assertThat(detail.additionalInformation()).isEqualTo("현장 문의");
        assertThat(detail.safetyNotice()).isEqualTo("맹견 입마개 필수");
        assertThat(detail.availableFacilities()).isEqualTo("반려견 놀이터");
        assertThat(detail.providedItems()).isEqualTo("배변봉투");
        assertThat(detail.purchasableItems()).isEqualTo("반려견 간식");
        assertThat(detail.rentableItems()).isEqualTo("반려견 유모차");
    }

    @Test
    void mapsEmptyPetDetailItemsToEmptyOfficialInformation() throws Exception {
        TourApiResponse response = objectMapper.readValue("""
                {
                  "response": {
                    "header": {"resultCode": "0000", "resultMsg": "OK"},
                    "body": {"items": ""}
                  }
                }
                """, TourApiResponse.class);

        var detail = mapper.toPlacePetDetail(response);

        assertThat(detail).isEqualTo(com.petway.place.model.PlacePetDetail.empty());
    }

    @Test
    void rejectsUnexpectedTextInPetDetailItems() throws Exception {
        TourApiResponse response = objectMapper.readValue("""
                {
                  "response": {
                    "header": {"resultCode": "0000", "resultMsg": "OK"},
                    "body": {"items": "unexpected"}
                  }
                }
                """, TourApiResponse.class);

        assertThatThrownBy(() -> mapper.toPlacePetDetail(response))
                .isInstanceOf(TourApiException.class)
                .extracting(exception -> ((TourApiException) exception).getErrorType())
                .isEqualTo(TourApiErrorType.INVALID_RESPONSE);
    }
}

/* 동물등록 API의 응답 봉투와 등록정보 필드를 역직렬화하는 외부 응답 모델이다. */
package com.petway.external.animalregistration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnimalRegistrationApiResponse(
        Response response
) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(
        Header header,
        Body body
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(
            String reqNo,
            String resultCode,
            String resultMsg,
            String errorMsg
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(Item item) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            String officeTel,
            String aprGbNm,
            String dogRegNo,
            String rfidCd,
            String rfidGubun,
            String birthDt,
            String dogNm,
            String sexNm,
            String kindNm,
            String neuterYn,
            String orgNm,
            String regTm,
            String aprTm
    ) {
    }
}

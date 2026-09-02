package com.petway.external.animalregistration.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnimalRegistrationApiResponse(
        Header header,
        Body body
) {

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

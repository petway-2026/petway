package com.petway.hospital.client.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AnimalHospitalApiResponse(Response response) {

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Response(Header header, Body body) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Header(String resultCode, String resultMsg) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            String dataType,
            int numOfRows,
            int pageNo,
            long totalCount,
            Items items
    ) {
        public List<Item> itemList() {
            return items == null || items.item() == null
                    ? List.of()
                    : items.item();
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Items(List<Item> item) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @JsonProperty("MNG_NO") String managementNumber,
            @JsonProperty("BPLC_NM") String businessName,
            @JsonProperty("ROAD_NM_ADDR") String roadAddress,
            @JsonProperty("LOTNO_ADDR") String lotNumberAddress,
            @JsonProperty("TELNO") String telephone,
            @JsonProperty("SALS_STTS_CD") String businessStatusCode,
            @JsonProperty("SALS_STTS_NM") String businessStatusName,
            @JsonProperty("DTL_SALS_STTS_CD") String detailedStatusCode,
            @JsonProperty("DTL_SALS_STTS_NM") String detailedStatusName,
            @JsonProperty("CRD_INFO_X") String coordinateX,
            @JsonProperty("CRD_INFO_Y") String coordinateY,
            @JsonProperty("LCPMT_YMD") String licenseDate,
            @JsonProperty("DAT_UPDT_PNT") String dataUpdatedAt
    ) {
    }
}

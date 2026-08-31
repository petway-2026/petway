package com.petway.external.tour.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.databind.JsonNode;

@JsonIgnoreProperties(ignoreUnknown = true)
public record TourApiResponse(
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
            String resultCode,
            String resultMsg
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Body(
            JsonNode items,
            Integer numOfRows,
            Integer pageNo,
            Integer totalCount
    ) {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record Item(
            @JsonProperty("contentid") String contentId,
            @JsonProperty("contenttypeid") String contentTypeId,
            String title,
            @JsonProperty("addr1") String address,
            @JsonProperty("mapy") String latitude,
            @JsonProperty("mapx") String longitude,
            @JsonProperty("firstimage") String thumbnailUrl
    ) {
    }
}

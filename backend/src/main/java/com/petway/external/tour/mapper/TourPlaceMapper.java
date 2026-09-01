package com.petway.external.tour.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petway.external.tour.dto.TourApiResponse;
import com.petway.external.tour.exception.TourApiErrorType;
import com.petway.external.tour.exception.TourApiException;
import com.petway.place.model.PlaceSearchResult;
import com.petway.place.model.PlaceSummary;
import com.petway.place.model.PlacePetDetail;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

@Component
@RequiredArgsConstructor
public class TourPlaceMapper {

    private static final String SUCCESS_RESULT_CODE = "0000";

    private final ObjectMapper objectMapper;

    public PlaceSearchResult toPlaceSearchResult(TourApiResponse apiResponse) {
        TourApiResponse.Response response = requireResponse(apiResponse);

        if (response.header() == null
                || !SUCCESS_RESULT_CODE.equals(response.header().resultCode())) {
            throw new TourApiException(
                    TourApiErrorType.API_ERROR,
                    "관광정보 제공기관에서 요청을 처리하지 못했습니다."
            );
        }

        TourApiResponse.Body body = response.body();
        if (body == null
                || body.pageNo() == null
                || body.numOfRows() == null
                || body.totalCount() == null) {
            throw invalidResponse();
        }

        List<PlaceSummary> places = readItems(body.items()).stream()
                .map(this::toPlaceSummary)
                .toList();

        return new PlaceSearchResult(
                body.pageNo(),
                body.numOfRows(),
                body.totalCount(),
                places
        );
    }

    public PlacePetDetail toPlacePetDetail(TourApiResponse apiResponse) {
        TourApiResponse.Response response = requireResponse(apiResponse);
        if (response.header() == null || !SUCCESS_RESULT_CODE.equals(response.header().resultCode())) {
            throw new TourApiException(TourApiErrorType.API_ERROR, "반려동물 동반 상세정보를 불러오지 못했습니다.");
        }
        if (response.body() == null) {
            throw invalidResponse();
        }

        JsonNode items = response.body().items();
        if (items == null || items.isNull()) {
            return PlacePetDetail.empty();
        }
        if (items.isTextual()) {
            if (items.textValue().isBlank()) {
                return PlacePetDetail.empty();
            }
            throw invalidResponse();
        }
        if (!items.isObject()) {
            throw invalidResponse();
        }

        JsonNode item = items.get("item");
        if (item == null || item.isNull() || (item.isArray() && item.isEmpty())) {
            return PlacePetDetail.empty();
        }
        if (item.isArray()) item = item.path(0);
        if (item == null || !item.isObject()) throw invalidResponse();

        return new PlacePetDetail(
                textOrNull(item, "acmpyTypeCd"),
                textOrNull(item, "acmpyPsblCpam"),
                textOrNull(item, "acmpyNeedMtr"),
                textOrNull(item, "etcAcmpyInfo"),
                textOrNull(item, "relaAcdntRiskMtr")
        );
    }

    private String textOrNull(JsonNode item, String field) {
        return blankToNull(item.path(field).asText(null));
    }

    private TourApiResponse.Response requireResponse(TourApiResponse apiResponse) {
        if (apiResponse == null || apiResponse.response() == null) {
            throw invalidResponse();
        }
        return apiResponse.response();
    }

    private List<TourApiResponse.Item> readItems(JsonNode itemsNode) {
        if (itemsNode == null || itemsNode.isNull()) {
            return List.of();
        }
        if (itemsNode.isTextual()) {
            if (itemsNode.textValue().isBlank()) {
                return List.of();
            }
            throw invalidResponse();
        }

        JsonNode itemNode = itemsNode.get("item");
        if (itemNode == null || itemNode.isNull()) {
            return List.of();
        }

        try {
            List<TourApiResponse.Item> items = new ArrayList<>();
            if (itemNode.isArray()) {
                for (JsonNode node : itemNode) {
                    items.add(objectMapper.treeToValue(node, TourApiResponse.Item.class));
                }
            } else if (itemNode.isObject()) {
                items.add(objectMapper.treeToValue(itemNode, TourApiResponse.Item.class));
            } else {
                throw invalidResponse();
            }
            return items;
        } catch (TourApiException e) {
            throw e;
        } catch (Exception e) {
            throw new TourApiException(
                    TourApiErrorType.INVALID_RESPONSE,
                    "관광정보 응답 형식이 올바르지 않습니다.",
                    e
            );
        }
    }

    private PlaceSummary toPlaceSummary(TourApiResponse.Item item) {
        if (isBlank(item.contentId())
                || isBlank(item.contentTypeId())
                || isBlank(item.title())) {
            throw invalidResponse();
        }

        return new PlaceSummary(
                item.contentId(),
                item.contentTypeId(),
                blankToNull(item.areaCode()),
                blankToNull(item.sigunguCode()),
                item.title(),
                blankToNull(item.address()),
                parseCoordinate(item.latitude()),
                parseCoordinate(item.longitude()),
                blankToNull(item.thumbnailUrl())
        );
    }

    private BigDecimal parseCoordinate(String value) {
        if (isBlank(value)) {
            return null;
        }

        try {
            return new BigDecimal(value);
        } catch (NumberFormatException e) {
            throw new TourApiException(
                    TourApiErrorType.INVALID_RESPONSE,
                    "관광정보 응답의 좌표 형식이 올바르지 않습니다.",
                    e
            );
        }
    }

    private String blankToNull(String value) {
        return isBlank(value) ? null : value;
    }

    private boolean isBlank(String value) {
        return value == null || value.isBlank();
    }

    private TourApiException invalidResponse() {
        return new TourApiException(
                TourApiErrorType.INVALID_RESPONSE,
                "관광정보 응답 형식이 올바르지 않습니다."
        );
    }
}

package com.petway.external.tour.mapper;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.petway.external.tour.dto.TourApiResponse;
import com.petway.external.tour.exception.TourApiErrorType;
import com.petway.external.tour.exception.TourApiException;
import com.petway.place.model.PlaceSearchResult;
import com.petway.place.model.PlaceSummary;
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

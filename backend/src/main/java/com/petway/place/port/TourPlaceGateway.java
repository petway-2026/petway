/* 장소 검색과 동반 상세 조회의 내부 계약이다. 서비스는 관광공사 HTTP 구현 대신 이 포트에 의존한다. */
package com.petway.place.port;

import com.petway.place.model.PlaceSearchCondition;
import com.petway.place.model.PlaceSearchResult;
import com.petway.place.model.NearbyPlaceSearchCondition;
import com.petway.place.model.PlacePetDetail;

public interface TourPlaceGateway {

    PlaceSearchResult searchByArea(PlaceSearchCondition condition);

    PlaceSearchResult searchNearby(NearbyPlaceSearchCondition condition);

    PlacePetDetail getPetDetail(String contentId);
}

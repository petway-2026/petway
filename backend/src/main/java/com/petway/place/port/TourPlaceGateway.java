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

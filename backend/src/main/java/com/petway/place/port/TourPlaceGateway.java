package com.petway.place.port;

import com.petway.place.model.PlaceSearchCondition;
import com.petway.place.model.PlaceSearchResult;

public interface TourPlaceGateway {

    PlaceSearchResult searchByArea(PlaceSearchCondition condition);
}

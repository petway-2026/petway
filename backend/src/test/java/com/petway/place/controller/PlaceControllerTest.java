package com.petway.place.controller;

import com.petway.place.dto.PlaceSearchResponse;
import com.petway.place.dto.PlaceSummaryResponse;
import com.petway.place.service.PlaceQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@ExtendWith(MockitoExtension.class)
class PlaceControllerTest {

    @Mock
    private PlaceQueryService placeQueryService;

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders
                .standaloneSetup(new PlaceController(placeQueryService))
                .build();
    }

    @Test
    void returnsPlaceSearchResponse() throws Exception {
        when(placeQueryService.searchByArea(any()))
                .thenReturn(new PlaceSearchResponse(
                        1,
                        20,
                        1,
                        List.of(new PlaceSummaryResponse(
                                "125266",
                                "12",
                                "1",
                                "1",
                                "경복궁",
                                "서울특별시 종로구 사직로 161",
                                null,
                                null,
                                null
                        ))
                ));

        mockMvc.perform(get("/api/places")
                        .param("areaCode", "1")
                        .param("contentTypeId", "12"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.page").value(1))
                .andExpect(jsonPath("$.items[0].contentId").value("125266"))
                .andExpect(jsonPath("$.items[0].areaCode").value("1"))
                .andExpect(jsonPath("$.items[0].sigunguCode").value("1"));
    }

    @Test
    void rejectsRequestWithoutAreaCode() throws Exception {
        mockMvc.perform(get("/api/places"))
                .andExpect(status().isBadRequest());
    }

    @Test
    void rejectsAreaCodeThatExceedsTwoDigits() throws Exception {
        mockMvc.perform(get("/api/places")
                        .param("areaCode", "92034254717780654844"))
                .andExpect(status().isBadRequest());
    }
}

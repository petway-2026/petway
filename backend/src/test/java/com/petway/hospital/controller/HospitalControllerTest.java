package com.petway.hospital.controller;

import com.petway.hospital.client.AnimalHospitalApiException;
import com.petway.hospital.dto.HospitalPageResponse;
import com.petway.hospital.dto.HospitalResponse;
import com.petway.hospital.service.HospitalService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import java.util.List;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class HospitalControllerTest {

    private HospitalService hospitalService;
    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        hospitalService = mock(HospitalService.class);
        mockMvc = MockMvcBuilders
                .standaloneSetup(new HospitalController(hospitalService))
                .setControllerAdvice(new HospitalExceptionHandler())
                .build();
    }

    @Test
    void exposesIndependentHospitalSearchApi() throws Exception {
        HospitalResponse hospital = new HospitalResponse(
                "M-1",
                "테스트동물병원",
                "서울특별시 테스트로 1",
                "서울특별시 테스트로 1",
                null,
                "02-0000-0000",
                "영업/정상",
                "정상",
                "20260101",
                "20260828000000",
                37.5,
                127.0
        );
        when(hospitalService.findHospitals(
                2,
                20,
                "서울특별시",
                "테스트"
        )).thenReturn(new HospitalPageResponse(
                List.of(hospital),
                2,
                20,
                1
        ));

        mockMvc.perform(get("/api/hospitals")
                        .param("page", "2")
                        .param("size", "20")
                        .param("address", "서울특별시")
                        .param("name", "테스트"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hospitals[0].id").value("M-1"))
                .andExpect(jsonPath("$.hospitals[0].latitude").value(37.5))
                .andExpect(jsonPath("$.page").value(2));

        verify(hospitalService).findHospitals(
                2,
                20,
                "서울특별시",
                "테스트"
        );
    }

    @Test
    void translatesExternalFailureToBadGateway() throws Exception {
        when(hospitalService.findHospitals(1, 100, null, null))
                .thenThrow(new AnimalHospitalApiException("외부 API 오류"));

        mockMvc.perform(get("/api/hospitals"))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.message").value("외부 API 오류"));
    }
}

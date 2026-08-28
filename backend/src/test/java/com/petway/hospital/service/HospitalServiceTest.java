package com.petway.hospital.service;

import com.petway.hospital.client.AnimalHospitalClient;
import com.petway.hospital.client.dto.AnimalHospitalApiResponse;
import com.petway.hospital.coordinate.HospitalCoordinateConverter;
import com.petway.hospital.dto.HospitalPageResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class HospitalServiceTest {

    @Mock
    private AnimalHospitalClient animalHospitalClient;

    private final HospitalCoordinateConverter coordinateConverter =
            new HospitalCoordinateConverter();

    @InjectMocks
    private HospitalService hospitalService;

    @Test
    void returnsOnlyActiveHospitalsAndConvertsCoordinates() {
        hospitalService = new HospitalService(
                animalHospitalClient,
                coordinateConverter
        );
        when(animalHospitalClient.findActiveHospitals(
                1,
                100,
                "경기도",
                null
        )).thenReturn(body(List.of(
                item("active", "01", "207822.739588256", "410789.35102542"),
                item("closed", "03", "207822.739588256", "410789.35102542")
        )));

        HospitalPageResponse response = hospitalService.findHospitals(
                1,
                100,
                "경기도",
                null
        );

        assertThat(response.totalCount()).isEqualTo(2);
        assertThat(response.hospitals()).singleElement().satisfies(hospital -> {
            assertThat(hospital.id()).isEqualTo("active");
            assertThat(hospital.address()).isEqualTo("도로명 주소");
            assertThat(hospital.latitude()).isBetween(37.1, 37.3);
            assertThat(hospital.longitude()).isBetween(127.0, 127.2);
        });
        verify(animalHospitalClient).findActiveHospitals(
                1,
                100,
                "경기도",
                null
        );
    }

    @Test
    void keepsHospitalWithoutCoordinateAsNullableMapPosition() {
        hospitalService = new HospitalService(
                animalHospitalClient,
                coordinateConverter
        );
        when(animalHospitalClient.findActiveHospitals(
                1,
                100,
                null,
                null
        )).thenReturn(body(List.of(
                item("missing", "01", "", "")
        )));

        HospitalPageResponse response = hospitalService.findHospitals(
                1,
                100,
                null,
                null
        );

        assertThat(response.hospitals()).singleElement().satisfies(hospital -> {
            assertThat(hospital.latitude()).isNull();
            assertThat(hospital.longitude()).isNull();
        });
    }

    private static AnimalHospitalApiResponse.Body body(
            List<AnimalHospitalApiResponse.Item> items
    ) {
        return new AnimalHospitalApiResponse.Body(
                "JSON",
                100,
                1,
                items.size(),
                new AnimalHospitalApiResponse.Items(items)
        );
    }

    private static AnimalHospitalApiResponse.Item item(
            String id,
            String statusCode,
            String x,
            String y
    ) {
        return new AnimalHospitalApiResponse.Item(
                id,
                "동물병원",
                "도로명 주소",
                "지번 주소",
                "02-0000-0000",
                statusCode,
                "영업/정상",
                "0000",
                "정상",
                x,
                y,
                "20260101",
                "20260828000000"
        );
    }
}

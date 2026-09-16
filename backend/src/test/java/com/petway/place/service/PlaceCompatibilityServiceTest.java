/* PlaceCompatibilityService의 동작을 검증하는 테스트다. 검증 대상: 출처에 맞게 동반 상세를 얻고 원문 변환, 규정 파싱, 반려견 판정을 순서대로 실행한다. */
package com.petway.place.service;

import com.petway.compatibility.CompatibilityEvaluator;
import com.petway.compatibility.CompatibilityStatus;
import com.petway.pet.entity.Pet;
import com.petway.pet.enums.PetSizeType;
import com.petway.pet.repository.PetRepository;
import com.petway.pet.service.PrimaryPetQueryService;
import com.petway.petrule.PetRule.RequirementType;
import com.petway.petrule.PetRuleParser;
import com.petway.place.dto.PlaceCompatibilityResponse;
import com.petway.place.mapper.PlacePetRuleSourceAdapter;
import com.petway.place.model.PlacePetDetail;
import com.petway.place.port.TourPlaceGateway;
import com.petway.user.entity.User;
import com.petway.user.repository.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PlaceCompatibilityServiceTest {

    @Mock
    private TourPlaceGateway tourPlaceGateway;
    @Mock
    private UserRepository userRepository;
    @Mock
    private PetRepository petRepository;

    private PlaceCompatibilityService service;
    private User owner;

    @BeforeEach
    void setUp() {
        service = new PlaceCompatibilityService(
                tourPlaceGateway,
                new PlacePetRuleSourceAdapter(),
                new PetRuleParser(),
                new CompatibilityEvaluator(),
                new PrimaryPetQueryService(userRepository, petRepository)
        );
        owner = User.create("owner@petway.test", "보호자");
    }

    @Test
    void evaluatesDecimalWeightWithinOfficialLimit() {
        stubPrimaryPet(new BigDecimal("7.8"));
        when(tourPlaceGateway.getPetDetail("100"))
                .thenReturn(detail(null, "10kg 이하 동반 가능", null));

        PlaceCompatibilityResponse response = service.getCompatibility(
                "owner@petway.test",
                "100"
        );

        assertThat(response.petId()).isEqualTo(11L);
        assertThat(response.petName()).isEqualTo("보리");
        assertThat(response.petwayAssessment().status())
                .isEqualTo(CompatibilityStatus.AVAILABLE);
        assertThat(response.petwayAssessment().reasons())
                .anyMatch(reason -> reason.contains("체중"));
    }

    @Test
    void rejectsPetOverOfficialWeightLimit() {
        stubPrimaryPet(new BigDecimal("12"));
        when(tourPlaceGateway.getPetDetail("100"))
                .thenReturn(detail(null, "10kg 이하 동반 가능", null));

        PlaceCompatibilityResponse response = service.getCompatibility(
                "owner@petway.test",
                "100"
        );

        assertThat(response.petwayAssessment().status())
                .isEqualTo(CompatibilityStatus.UNAVAILABLE);
    }

    @Test
    void exposesParsedRequirementsWithoutFrontendCalculation() {
        stubPrimaryPet(new BigDecimal("7.8"));
        when(tourPlaceGateway.getPetDetail("100"))
                .thenReturn(detail(null, null, "목줄 필수, 이동장 필요"));

        PlaceCompatibilityResponse response = service.getCompatibility(
                "owner@petway.test",
                "100"
        );

        assertThat(response.petwayAssessment().status())
                .isEqualTo(CompatibilityStatus.CONDITIONAL);
        assertThat(response.petwayAssessment().requirements())
                .extracting(PlaceCompatibilityResponse.RequirementItem::type)
                .containsExactlyInAnyOrder(
                        RequirementType.LEASH,
                        RequirementType.CARRIER
                );
        assertThat(response.petwayAssessment().requirements())
                .extracting(PlaceCompatibilityResponse.RequirementItem::label)
                .containsExactlyInAnyOrder("목줄", "이동장");
    }

    @Test
    void exposesVaccinationCertificateForVaccinatedPet() {
        stubPrimaryPet(new BigDecimal("7.8"), true);
        when(tourPlaceGateway.getPetDetail("100"))
                .thenReturn(detail(null, null, "예방접종 증명서 지참"));

        PlaceCompatibilityResponse response = service.getCompatibility(
                "owner@petway.test",
                "100"
        );

        assertThat(response.petwayAssessment().status())
                .isEqualTo(CompatibilityStatus.CONDITIONAL);
        assertThat(response.petwayAssessment().requirements())
                .extracting(PlaceCompatibilityResponse.RequirementItem::type)
                .containsExactly(RequirementType.VACCINATION_CERTIFICATE);
        assertThat(response.petwayAssessment().requirements())
                .extracting(PlaceCompatibilityResponse.RequirementItem::label)
                .containsExactly("예방접종 증명서");
    }

    @Test
    void keepsAmbiguousOfficialTextAsNeedConfirmation() {
        stubPrimaryPet(new BigDecimal("7.8"));
        when(tourPlaceGateway.getPetDetail("100"))
                .thenReturn(new PlacePetDetail(
                        null,
                        null,
                        null,
                        "사전 문의 필수",
                        null
                ));

        PlaceCompatibilityResponse response = service.getCompatibility(
                "owner@petway.test",
                "100"
        );

        assertThat(response.petwayAssessment().status())
                .isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
        assertThat(response.petwayAssessment().needsConfirmation()).isTrue();
    }

    @Test
    void reportsMissingPrimaryPetBeforeCallingTourProvider() {
        when(userRepository.findByEmail("owner@petway.test"))
                .thenReturn(Optional.of(owner));
        when(petRepository.findByUserAndIsPrimaryTrue(owner))
                .thenReturn(Optional.empty());
        when(petRepository.findAllByUserOrderByIsPrimaryDescIdAsc(owner))
                .thenReturn(List.of());

        assertThatThrownBy(() -> service.getCompatibility(
                "owner@petway.test",
                "100"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("먼저 반려견을 등록");

        verify(tourPlaceGateway, never()).getPetDetail("100");
    }

    @Test
    void reportsAbnormalStateWhenPetsExistWithoutPrimaryPet() {
        Pet secondaryPet = Pet.create(
                owner,
                "초코",
                "믹스견",
                null,
                null,
                null,
                new BigDecimal("5"),
                PetSizeType.SMALL,
                null,
                null,
                null,
                null,
                null,
                null,
                false
        );
        when(userRepository.findByEmail("owner@petway.test"))
                .thenReturn(Optional.of(owner));
        when(petRepository.findByUserAndIsPrimaryTrue(owner))
                .thenReturn(Optional.empty());
        when(petRepository.findAllByUserOrderByIsPrimaryDescIdAsc(owner))
                .thenReturn(List.of(secondaryPet));

        assertThatThrownBy(() -> service.getCompatibility(
                "owner@petway.test",
                "100"
        ))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("대표 반려견 설정");

        verify(tourPlaceGateway, never()).getPetDetail("100");
    }

    @Test
    void returnsNeedConfirmationWhenOfficialPetDetailIsEmpty() {
        stubPrimaryPet(new BigDecimal("7.8"));
        when(tourPlaceGateway.getPetDetail("100"))
                .thenReturn(PlacePetDetail.empty());

        PlaceCompatibilityResponse response = service.getCompatibility(
                "owner@petway.test",
                "100"
        );

        assertThat(response.petwayAssessment().status())
                .isEqualTo(CompatibilityStatus.NEED_CONFIRMATION);
        assertThat(response.officialInformation().accompanimentType()).isNull();
    }

    @Test
    void resolvesPrimaryPetOnlyWithinAuthenticatedUserOwnership() {
        Pet primaryPet = stubPrimaryPet(new BigDecimal("7.8"));
        User otherUser = User.create("other@petway.test", "다른 사용자");
        when(tourPlaceGateway.getPetDetail("100"))
                .thenReturn(detail("전구역 동반가능", null, null));

        PlaceCompatibilityResponse response = service.getCompatibility(
                "owner@petway.test",
                "100"
        );

        assertThat(response.petId()).isEqualTo(primaryPet.getId());
        verify(petRepository).findByUserAndIsPrimaryTrue(owner);
        verify(petRepository, never()).findByUserAndIsPrimaryTrue(otherUser);
    }

    private Pet stubPrimaryPet(BigDecimal weight) {
        return stubPrimaryPet(weight, null);
    }

    private Pet stubPrimaryPet(BigDecimal weight, Boolean vaccinated) {
        Pet pet = Pet.create(
                owner,
                "보리",
                "믹스견",
                null,
                null,
                null,
                weight,
                PetSizeType.SMALL,
                null,
                null,
                null,
                null,
                vaccinated,
                null,
                true
        );
        ReflectionTestUtils.setField(pet, "id", 11L);
        when(userRepository.findByEmail("owner@petway.test"))
                .thenReturn(Optional.of(owner));
        when(petRepository.findByUserAndIsPrimaryTrue(owner))
                .thenReturn(Optional.of(pet));
        return pet;
    }

    private PlacePetDetail detail(
            String accompanimentType,
            String allowedCapacity,
            String requiredItems
    ) {
        return new PlacePetDetail(
                accompanimentType,
                allowedCapacity,
                requiredItems,
                null,
                null
        );
    }
}

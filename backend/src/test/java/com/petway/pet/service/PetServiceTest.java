package com.petway.pet.service;

import com.petway.pet.dto.PetCreateRequest;
import com.petway.pet.dto.PetResponse;
import com.petway.pet.dto.PetUpdateRequest;
import com.petway.pet.entity.Pet;
import com.petway.pet.enums.PetSizeType;
import com.petway.pet.repository.PetRepository;
import com.petway.user.entity.User;
import com.petway.user.repository.UserRepository;
import jakarta.persistence.Column;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PetServiceTest {

    @Mock
    private PetRepository petRepository;

    @Mock
    private UserRepository userRepository;

    private PetService petService;
    private User user;

    @BeforeEach
    void setUp() {
        petService = new PetService(petRepository, userRepository);
        user = User.create("owner@petway.test", "보호자");
    }

    @Test
    void createsPetWithExplicitSizeType() {
        stubUser();
        when(petRepository.existsByUserAndIsPrimaryTrue(user))
                .thenReturn(false);

        PetResponse response = petService.createPet(
                "owner@petway.test",
                createRequest(PetSizeType.SMALL)
        );

        ArgumentCaptor<Pet> captor = ArgumentCaptor.forClass(Pet.class);
        verify(petRepository).save(captor.capture());
        assertThat(captor.getValue().getSizeType()).isEqualTo(PetSizeType.SMALL);
        assertThat(response.sizeType()).isEqualTo(PetSizeType.SMALL);
        assertThat(response.isPrimary()).isTrue();
    }

    @Test
    void updatesAndReturnsExplicitSizeType() {
        stubUser();
        Pet pet = pet(PetSizeType.SMALL);
        when(petRepository.findByIdAndUser(1L, user))
                .thenReturn(Optional.of(pet));

        PetResponse response = petService.updatePet(
                "owner@petway.test",
                1L,
                updateRequest(PetSizeType.LARGE)
        );

        assertThat(pet.getSizeType()).isEqualTo(PetSizeType.LARGE);
        assertThat(response.sizeType()).isEqualTo(PetSizeType.LARGE);
    }

    @Test
    void readsExistingPetWithNullableSizeThroughOwnershipQuery() {
        stubUser();
        Pet pet = pet(null);
        when(petRepository.findByIdAndUser(1L, user))
                .thenReturn(Optional.of(pet));

        PetResponse response = petService.getPet(
                "owner@petway.test",
                1L
        );

        verify(petRepository).findByIdAndUser(1L, user);
        assertThat(response.sizeType()).isNull();
        assertThat(response.petName()).isEqualTo("두부");
    }

    @Test
    void listsExistingPetsWithoutChangingCrudBehavior() {
        stubUser();
        Pet primary = pet(PetSizeType.SMALL, true);
        Pet secondary = pet(PetSizeType.MEDIUM, false);
        when(petRepository.findAllByUserOrderByIsPrimaryDescIdAsc(user))
                .thenReturn(List.of(primary, secondary));

        List<PetResponse> responses = petService.getMyPets("owner@petway.test");

        assertThat(responses)
                .extracting(PetResponse::sizeType)
                .containsExactly(PetSizeType.SMALL, PetSizeType.MEDIUM);
    }

    @Test
    void deletingPrimaryPetStillPromotesRemainingPet() {
        stubUser();
        Pet primary = pet(PetSizeType.SMALL, true);
        Pet remaining = pet(PetSizeType.MEDIUM, false);
        when(petRepository.findByIdAndUser(1L, user))
                .thenReturn(Optional.of(primary));
        when(petRepository.findAllByUserOrderByIsPrimaryDescIdAsc(user))
                .thenReturn(List.of(remaining));

        petService.deletePet("owner@petway.test", 1L);

        verify(petRepository).delete(primary);
        verify(petRepository).flush();
        assertThat(remaining.getIsPrimary()).isTrue();
    }

    @Test
    void persistsSizeTypeAsNullableStringEnum() throws NoSuchFieldException {
        var field = Pet.class.getDeclaredField("sizeType");

        assertThat(field.getAnnotation(Enumerated.class).value())
                .isEqualTo(EnumType.STRING);
        assertThat(field.getAnnotation(Column.class).name())
                .isEqualTo("size_type");
        assertThat(field.getAnnotation(Column.class).nullable()).isTrue();
    }

    private PetCreateRequest createRequest(PetSizeType sizeType) {
        return new PetCreateRequest(
                "두부",
                "믹스견",
                null,
                null,
                null,
                new BigDecimal("8.5"),
                sizeType,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private void stubUser() {
        when(userRepository.findByEmail("owner@petway.test"))
                .thenReturn(Optional.of(user));
    }

    private PetUpdateRequest updateRequest(PetSizeType sizeType) {
        return new PetUpdateRequest(
                "두부",
                "믹스견",
                null,
                null,
                null,
                new BigDecimal("8.5"),
                sizeType,
                null,
                null,
                null,
                null,
                null,
                null
        );
    }

    private Pet pet(PetSizeType sizeType) {
        return pet(sizeType, true);
    }

    private Pet pet(PetSizeType sizeType, boolean isPrimary) {
        return Pet.create(
                user,
                "두부",
                "믹스견",
                null,
                null,
                null,
                new BigDecimal("8.5"),
                sizeType,
                null,
                null,
                null,
                null,
                null,
                null,
                isPrimary
        );
    }
}

package com.petway.pet.service;

import com.petway.pet.entity.Pet;
import com.petway.pet.enums.PetSizeType;
import com.petway.pet.repository.PetRepository;
import com.petway.user.entity.User;
import com.petway.user.repository.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PrimaryPetQueryServiceTest {

    @Mock
    private UserRepository userRepository;
    @Mock
    private PetRepository petRepository;

    @Test
    void returnsOnlyPrimaryPetOwnedByAuthenticatedUser() {
        User owner = User.create("owner@petway.test", "보호자");
        User other = User.create("other@petway.test", "다른 사용자");
        Pet ownerPet = pet(owner, "보리", true);
        when(userRepository.findByEmail("owner@petway.test"))
                .thenReturn(Optional.of(owner));
        when(petRepository.findByUserAndIsPrimaryTrue(owner))
                .thenReturn(Optional.of(ownerPet));

        Pet result = new PrimaryPetQueryService(userRepository, petRepository)
                .findByUserEmail("owner@petway.test");

        assertThat(result).isSameAs(ownerPet);
        verify(petRepository, never()).findByUserAndIsPrimaryTrue(other);
    }

    @Test
    void rejectsUserWithoutPetBeforeRecommendationStarts() {
        User owner = User.create("owner@petway.test", "보호자");
        when(userRepository.findByEmail("owner@petway.test"))
                .thenReturn(Optional.of(owner));
        when(petRepository.findByUserAndIsPrimaryTrue(owner))
                .thenReturn(Optional.empty());
        when(petRepository.findAllByUserOrderByIsPrimaryDescIdAsc(owner))
                .thenReturn(List.of());

        assertThatThrownBy(() ->
                new PrimaryPetQueryService(userRepository, petRepository)
                        .findByUserEmail("owner@petway.test")
        )
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("대표 반려견이 없습니다");
    }

    private Pet pet(User user, String name, boolean primary) {
        return Pet.create(
                user,
                name,
                "믹스견",
                null,
                null,
                null,
                new BigDecimal("7"),
                PetSizeType.SMALL,
                null,
                null,
                null,
                null,
                null,
                null,
                primary
        );
    }
}

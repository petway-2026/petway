package com.petway.pet.service;

import com.petway.pet.dto.PetCreateRequest;
import com.petway.pet.dto.PetResponse;
import com.petway.pet.dto.PetUpdateRequest;
import com.petway.pet.entity.Pet;
import com.petway.pet.repository.PetRepository;
import com.petway.user.entity.User;
import com.petway.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class PetService {

    private final PetRepository petRepository;
    private final UserRepository userRepository;


    /**
     * 반려견 등록
     */
    @Transactional
    public PetResponse createPet(
            String email,
            PetCreateRequest request
    ) {

        User user =
                getUser(email);


        /*
         * 등록된 대표견이 하나도 없다면
         * 첫 번째 반려견을 자동 대표견으로 지정
         */
        boolean hasPrimary =
                petRepository
                        .existsByUserAndIsPrimaryTrue(
                                user
                        );


        Pet pet =
                Pet.create(
                        user,
                        request.petName(),
                        request.breed(),
                        request.birthDate(),
                        request.weightKg(),
                        request.activityLevel(),
                        request.crowdTolerance(),
                        request.placePreference(),
                        request.socialityLevel(),
                        request.vaccinated(),
                        request.neutered(),
                        !hasPrimary
                );


        petRepository.save(pet);


        return PetResponse.from(pet);
    }


    /**
     * 내 반려견 전체 조회
     */
    @Transactional(readOnly = true)
    public List<PetResponse> getMyPets(
            String email
    ) {

        User user =
                getUser(email);


        return petRepository
                .findAllByUserOrderByIsPrimaryDescIdAsc(
                        user
                )
                .stream()
                .map(PetResponse::from)
                .toList();
    }


    /**
     * 반려견 상세 조회
     */
    @Transactional(readOnly = true)
    public PetResponse getPet(
            String email,
            Long petId
    ) {

        User user =
                getUser(email);


        Pet pet =
                getMyPet(
                        user,
                        petId
                );


        return PetResponse.from(pet);
    }


    /**
     * 반려견 수정
     */
    @Transactional
    public PetResponse updatePet(
            String email,
            Long petId,
            PetUpdateRequest request
    ) {

        User user =
                getUser(email);


        Pet pet =
                getMyPet(
                        user,
                        petId
                );


        pet.update(
                request.petName(),
                request.breed(),
                request.birthDate(),
                request.weightKg(),
                request.activityLevel(),
                request.crowdTolerance(),
                request.placePreference(),
                request.socialityLevel(),
                request.vaccinated(),
                request.neutered()
        );


        return PetResponse.from(pet);
    }


    /**
     * 대표 반려견 지정
     */
    @Transactional
    public PetResponse setPrimaryPet(
            String email,
            Long petId
    ) {

        User user =
                getUser(email);


        Pet targetPet =
                getMyPet(
                        user,
                        petId
                );


        /*
         * 기존 대표견 해제
         */
        petRepository
                .findByUserAndIsPrimaryTrue(
                        user
                )
                .ifPresent(primaryPet -> {

                    if (!primaryPet
                            .getId()
                            .equals(targetPet.getId())) {

                        primaryPet.setPrimary(false);
                    }
                });


        /*
         * 새 대표견 지정
         */
        targetPet.setPrimary(true);


        return PetResponse.from(
                targetPet
        );
    }


    /**
     * 반려견 삭제
     */
    @Transactional
    public void deletePet(
            String email,
            Long petId
    ) {

        User user =
                getUser(email);


        Pet pet =
                getMyPet(
                        user,
                        petId
                );


        boolean wasPrimary =
                Boolean.TRUE.equals(
                        pet.getIsPrimary()
                );


        petRepository.delete(pet);

        petRepository.flush();


        /*
         * 대표견을 삭제한 경우,
         * 남아있는 반려견 중 첫 번째를 새 대표견으로 지정
         */
        if (wasPrimary) {

            List<Pet> remainingPets =
                    petRepository
                            .findAllByUserOrderByIsPrimaryDescIdAsc(
                                    user
                            );


            if (!remainingPets.isEmpty()) {

                remainingPets
                        .get(0)
                        .setPrimary(true);
            }
        }
    }


    /**
     * 현재 로그인 회원 조회
     */
    private User getUser(
            String email
    ) {

        return userRepository
                .findByEmail(email)
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "회원 정보를 찾을 수 없습니다."
                        )
                );
    }


    /**
     * 내 반려견인지 검증하며 조회
     */
    private Pet getMyPet(
            User user,
            Long petId
    ) {

        return petRepository
                .findByIdAndUser(
                        petId,
                        user
                )
                .orElseThrow(() ->
                        new IllegalArgumentException(
                                "반려견 정보를 찾을 수 없습니다."
                        )
                );
    }
}
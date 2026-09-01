package com.petway.pet.service;

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
public class PrimaryPetQueryService {

    private final UserRepository userRepository;
    private final PetRepository petRepository;

    @Transactional(readOnly = true)
    public Pet findByUserEmail(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new IllegalArgumentException(
                        "회원 정보를 찾을 수 없습니다."
                ));

        return petRepository.findByUserAndIsPrimaryTrue(user)
                .orElseThrow(() -> primaryPetNotFound(user));
    }

    private IllegalStateException primaryPetNotFound(User user) {
        List<Pet> pets = petRepository.findAllByUserOrderByIsPrimaryDescIdAsc(user);
        if (pets.isEmpty()) {
            return new IllegalStateException(
                    "대표 반려견이 없습니다. 먼저 반려견을 등록해 주세요."
            );
        }
        return new IllegalStateException(
                "대표 반려견 설정을 확인해 주세요."
        );
    }
}

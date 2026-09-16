/* 반려견을 소유자와 함께 조회하고 사용자별 대표견 및 목록을 찾는다. */
package com.petway.pet.repository;

import com.petway.pet.entity.Pet;
import com.petway.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface PetRepository
        extends JpaRepository<Pet, Long> {

    List<Pet> findAllByUserOrderByIsPrimaryDescIdAsc(
            User user
    );


    Optional<Pet> findByIdAndUser(
            Long petId,
            User user
    );


    boolean existsByUserAndIsPrimaryTrue(
            User user
    );


    Optional<Pet> findByUserAndIsPrimaryTrue(
            User user
    );

}
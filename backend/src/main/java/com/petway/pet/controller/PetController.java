/* 로그인 사용자의 반려견 CRUD, 대표견 변경, 동물등록정보 조회 요청을 연결한다. */
package com.petway.pet.controller;

import com.petway.pet.dto.PetCreateRequest;
import com.petway.pet.dto.PetResponse;
import com.petway.pet.dto.PetRegistrationLookupRequest;
import com.petway.pet.dto.PetRegistrationLookupResponse;
import com.petway.pet.dto.PetUpdateRequest;
import com.petway.pet.service.PetRegistrationLookupService;
import com.petway.pet.service.PetService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/pets")
@RequiredArgsConstructor
public class PetController {

    private final PetService petService;
    private final PetRegistrationLookupService petRegistrationLookupService;


    /**
     * 국가동물등록정보를 조회해 반려견 등록 폼에 사용할 기본 정보를 반환한다.
     */
    @PostMapping("/registration-lookup")
    public ResponseEntity<PetRegistrationLookupResponse> lookupRegistration(
            @RequestBody PetRegistrationLookupRequest request
    ) {
        return ResponseEntity.ok(
                petRegistrationLookupService.lookup(request)
        );
    }


    /**
     * 반려견 등록
     */
    @PostMapping
    public ResponseEntity<PetResponse> createPet(
            Authentication authentication,

            @Valid
            @RequestBody
            PetCreateRequest request
    ) {

        PetResponse response =
                petService.createPet(
                        authentication.getName(),
                        request
                );


        return ResponseEntity.ok(
                response
        );
    }


    /**
     * 내 반려견 전체 조회
     */
    @GetMapping
    public ResponseEntity<List<PetResponse>> getMyPets(
            Authentication authentication
    ) {

        List<PetResponse> response =
                petService.getMyPets(
                        authentication.getName()
                );


        return ResponseEntity.ok(
                response
        );
    }


    /**
     * 반려견 상세 조회
     */
    @GetMapping("/{petId}")
    public ResponseEntity<PetResponse> getPet(
            Authentication authentication,

            @PathVariable
            Long petId
    ) {

        PetResponse response =
                petService.getPet(
                        authentication.getName(),
                        petId
                );


        return ResponseEntity.ok(
                response
        );
    }


    /**
     * 반려견 수정
     */
    @PutMapping("/{petId}")
    public ResponseEntity<PetResponse> updatePet(
            Authentication authentication,

            @PathVariable
            Long petId,

            @Valid
            @RequestBody
            PetUpdateRequest request
    ) {

        PetResponse response =
                petService.updatePet(
                        authentication.getName(),
                        petId,
                        request
                );


        return ResponseEntity.ok(
                response
        );
    }


    /**
     * 대표 반려견 지정
     */
    @PatchMapping("/{petId}/primary")
    public ResponseEntity<PetResponse> setPrimaryPet(
            Authentication authentication,

            @PathVariable
            Long petId
    ) {

        PetResponse response =
                petService.setPrimaryPet(
                        authentication.getName(),
                        petId
                );


        return ResponseEntity.ok(
                response
        );
    }


    /**
     * 반려견 삭제
     */
    @DeleteMapping("/{petId}")
    public ResponseEntity<?> deletePet(
            Authentication authentication,

            @PathVariable
            Long petId
    ) {

        petService.deletePet(
                authentication.getName(),
                petId
        );


        return ResponseEntity.ok(
                Map.of(
                        "message",
                        "반려견 정보가 삭제되었습니다."
                )
        );
    }
}

/* 동물등록정보 조회의 내부 계약으로 외부 HTTP 구현을 서비스에서 분리한다. */
package com.petway.pet.port;

import com.petway.pet.model.AnimalRegistrationLookupCondition;
import com.petway.pet.model.RegisteredPetProfile;

public interface AnimalRegistrationGateway {

    RegisteredPetProfile lookup(AnimalRegistrationLookupCondition condition);
}

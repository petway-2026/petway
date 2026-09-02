package com.petway.pet.port;

import com.petway.pet.model.AnimalRegistrationLookupCondition;
import com.petway.pet.model.RegisteredPetProfile;

public interface AnimalRegistrationGateway {

    RegisteredPetProfile lookup(AnimalRegistrationLookupCondition condition);
}

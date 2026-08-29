package com.petway.pet.enums;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class DogBreedTest {

    @Test
    void classifiesLegalDangerousBreedGroups() {
        assertThat(DogBreed.TOSA.isDangerous()).isTrue();
        assertThat(DogBreed.PIT_BULL_TERRIER.isDangerous()).isTrue();
        assertThat(DogBreed.AMERICAN_STAFFORDSHIRE_TERRIER.isDangerous()).isTrue();
        assertThat(DogBreed.STAFFORDSHIRE_BULL_TERRIER.isDangerous()).isTrue();
        assertThat(DogBreed.ROTTWEILER.isDangerous()).isTrue();
        assertThat(DogBreed.DANGEROUS_BREED_MIX.isDangerous()).isTrue();
    }

    @Test
    void doesNotClassifyOrdinaryOrGeneralMixedBreedAsDangerous() {
        assertThat(DogBreed.JINDO.isDangerous()).isFalse();
        assertThat(DogBreed.MIXED.isDangerous()).isFalse();
    }
}

package com.petway.pet.enums;

public enum DogBreed {
    MALTESE(false), POODLE(false), POMERANIAN(false), SHIH_TZU(false),
    CHIHUAHUA(false), BICHON_FRISE(false), YORKSHIRE_TERRIER(false),
    GOLDEN_RETRIEVER(false), LABRADOR_RETRIEVER(false), JINDO(false),
    SHIBA_INU(false), WELSH_CORGI(false), FRENCH_BULLDOG(false),
    BEAGLE(false), DACHSHUND(false), SCHNAUZER(false), BORDER_COLLIE(false),
    GERMAN_SHEPHERD(false), SAMOYED(false), SIBERIAN_HUSKY(false),
    DOBERMAN(false), GREAT_DANE(false),
    AKITA(false), ALASKAN_MALAMUTE(false), BASENJI(false), BOSTON_TERRIER(false),
    BOXER(false), BULLDOG(false), BULL_TERRIER(false), CANE_CORSO(false),
    CAVALIER_KING_CHARLES_SPANIEL(false), CHOW_CHOW(false), COCKER_SPANIEL(false),
    DALMATIAN(false), ENGLISH_SPRINGER_SPANIEL(false), ITALIAN_GREYHOUND(false),
    JACK_RUSSELL_TERRIER(false), LHASA_APSO(false), MINIATURE_PINSCHER(false),
    PAPILLON(false), PEKINGESE(false), PUG(false), ROUGH_COLLIE(false),
    SAINT_BERNARD(false), SHETLAND_SHEEPDOG(false), WHIPPET(false),
    AFGHAN_HOUND(false), BORZOI(false), NEWFOUNDLAND(false), BERNESE_MOUNTAIN_DOG(false),
    GREAT_PYRENEES(false), OLD_ENGLISH_SHEEPDOG(false), WEST_HIGHLAND_WHITE_TERRIER(false),
    AIREDALE_TERRIER(false), MIXED(false),

    TOSA(true), PIT_BULL_TERRIER(true), AMERICAN_STAFFORDSHIRE_TERRIER(true),
    STAFFORDSHIRE_BULL_TERRIER(true), ROTTWEILER(true), DANGEROUS_BREED_MIX(true);

    private final boolean dangerous;

    DogBreed(boolean dangerous) {
        this.dangerous = dangerous;
    }

    public boolean isDangerous() {
        return dangerous;
    }
}

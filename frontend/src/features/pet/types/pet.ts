export type ActivityLevel =
    | "LOW"
    | "MEDIUM"
    | "HIGH";

export type CrowdTolerance =
    | "LOW"
    | "MEDIUM"
    | "HIGH";

export type PlacePreference =
    | "INDOOR"
    | "OUTDOOR"
    | "BOTH";

export type SocialityLevel =
    | "LOW"
    | "MEDIUM"
    | "HIGH";

export type PetSizeType =
    | "SMALL"
    | "MEDIUM"
    | "LARGE";

export type DogBreed =
    | "MALTESE" | "POODLE" | "POMERANIAN" | "SHIH_TZU"
    | "CHIHUAHUA" | "BICHON_FRISE" | "YORKSHIRE_TERRIER"
    | "GOLDEN_RETRIEVER" | "LABRADOR_RETRIEVER" | "JINDO"
    | "SHIBA_INU" | "WELSH_CORGI" | "FRENCH_BULLDOG" | "BEAGLE"
    | "DACHSHUND" | "SCHNAUZER" | "BORDER_COLLIE" | "GERMAN_SHEPHERD"
    | "SAMOYED" | "SIBERIAN_HUSKY" | "DOBERMAN" | "GREAT_DANE"
    | "AKITA" | "ALASKAN_MALAMUTE" | "BASENJI" | "BOSTON_TERRIER"
    | "BOXER" | "BULLDOG" | "BULL_TERRIER" | "CANE_CORSO"
    | "CAVALIER_KING_CHARLES_SPANIEL" | "CHOW_CHOW" | "COCKER_SPANIEL"
    | "DALMATIAN" | "ENGLISH_SPRINGER_SPANIEL" | "ITALIAN_GREYHOUND"
    | "JACK_RUSSELL_TERRIER" | "LHASA_APSO" | "MINIATURE_PINSCHER"
    | "PAPILLON" | "PEKINGESE" | "PUG" | "ROUGH_COLLIE" | "SAINT_BERNARD"
    | "SHETLAND_SHEEPDOG" | "WHIPPET" | "AFGHAN_HOUND" | "BORZOI"
    | "NEWFOUNDLAND" | "BERNESE_MOUNTAIN_DOG" | "GREAT_PYRENEES"
    | "OLD_ENGLISH_SHEEPDOG" | "WEST_HIGHLAND_WHITE_TERRIER" | "AIREDALE_TERRIER"
    | "MIXED" | "TOSA" | "PIT_BULL_TERRIER"
    | "AMERICAN_STAFFORDSHIRE_TERRIER" | "STAFFORDSHIRE_BULL_TERRIER"
    | "ROTTWEILER" | "DANGEROUS_BREED_MIX";

export interface Pet {
    petId: number;
    petName: string;
    breedDetail: string | null;
    breed: DogBreed | null;
    dangerousBreed: boolean | null;
    animalRegistered: boolean | null;
    birthDate: string | null;
    weightKg: number;
    sizeType: PetSizeType | null;

    activityLevel: ActivityLevel | null;
    crowdTolerance: CrowdTolerance | null;
    placePreference: PlacePreference | null;
    socialityLevel: SocialityLevel | null;

    vaccinated: boolean | null;
    neutered: boolean | null;

    isPrimary: boolean;
}

export interface PetCreateRequest {
    petName: string;
    breedDetail: string;
    breed: DogBreed | null;
    animalRegistered: boolean | null;
    birthDate: string | null;
    weightKg: number;
    sizeType: PetSizeType | null;

    activityLevel: ActivityLevel | null;
    crowdTolerance: CrowdTolerance | null;
    placePreference: PlacePreference | null;
    socialityLevel: SocialityLevel | null;

    vaccinated: boolean | null;
    neutered: boolean | null;
}

export type PetUpdateRequest = PetCreateRequest;

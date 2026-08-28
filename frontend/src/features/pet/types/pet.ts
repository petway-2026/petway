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

export interface Pet {
    petId: number;
    petName: string;
    breed: string | null;
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
    breed: string;
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

/* 반려견 CRUD·대표견 지정·동물등록 조회 API를 제공하고 조회 오류 상태를 구분한다. */
import { apiFetch } from "../../../shared/api/client";
import type {
    Pet,
    PetCreateRequest,
    PetRegistrationLookupRequest,
    PetRegistrationLookupResponse,
    PetUpdateRequest,
} from "../types/pet";

import { API_URL } from "../../../shared/api/config";

async function parseError(
    response: Response
): Promise<string> {
    try {
        const data = await response.json();

        return (
            data.message ??
            "요청 처리 중 오류가 발생했습니다."
        );
    } catch {
        return "요청 처리 중 오류가 발생했습니다.";
    }
}

export class PetRegistrationLookupApiError extends Error {
    constructor(
        public readonly status: number,
        message: string
    ) {
        super(message);
        this.name = "PetRegistrationLookupApiError";
    }
}

export async function lookupPetRegistration(
    data: PetRegistrationLookupRequest,
    signal?: AbortSignal
): Promise<PetRegistrationLookupResponse> {
    const response = await apiFetch(
        `${API_URL}/api/pets/registration-lookup`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            credentials: "include",
            body: JSON.stringify(data),
            signal,
        }
    );

    if (!response.ok) {
        throw new PetRegistrationLookupApiError(
            response.status,
            await parseError(response)
        );
    }

    return response.json();
}

export async function getPets(): Promise<Pet[]> {
    const response = await apiFetch(
        `${API_URL}/api/pets`,
        {
            method: "GET",
            credentials: "include",
        }
    );

    if (!response.ok) {
        throw new Error(
            await parseError(response)
        );
    }

    return response.json();
}

export async function getPet(
    petId: number
): Promise<Pet> {
    const response = await apiFetch(
        `${API_URL}/api/pets/${petId}`,
        {
            method: "GET",
            credentials: "include",
        }
    );

    if (!response.ok) {
        throw new Error(
            await parseError(response)
        );
    }

    return response.json();
}

export async function createPet(
    data: PetCreateRequest
): Promise<Pet> {
    const response = await apiFetch(
        `${API_URL}/api/pets`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            credentials: "include",
            body: JSON.stringify(data),
        }
    );

    if (!response.ok) {
        throw new Error(
            await parseError(response)
        );
    }

    return response.json();
}

export async function updatePet(
    petId: number,
    data: PetUpdateRequest
): Promise<Pet> {
    const response = await apiFetch(
        `${API_URL}/api/pets/${petId}`,
        {
            method: "PUT",
            headers: {
                "Content-Type": "application/json",
            },
            credentials: "include",
            body: JSON.stringify(data),
        }
    );

    if (!response.ok) {
        throw new Error(
            await parseError(response)
        );
    }

    return response.json();
}

export async function setPrimaryPet(
    petId: number
): Promise<Pet> {
    const response = await apiFetch(
        `${API_URL}/api/pets/${petId}/primary`,
        {
            method: "PATCH",
            credentials: "include",
        }
    );

    if (!response.ok) {
        throw new Error(
            await parseError(response)
        );
    }

    return response.json();
}

export async function deletePet(
    petId: number
): Promise<void> {
    const response = await apiFetch(
        `${API_URL}/api/pets/${petId}`,
        {
            method: "DELETE",
            credentials: "include",
        }
    );

    if (!response.ok) {
        throw new Error(
            await parseError(response)
        );
    }
}

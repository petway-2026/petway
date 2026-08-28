import {
    useEffect,
    useState,
} from "react";

import {
    useNavigate,
    useParams,
} from "react-router-dom";

import {
    getPet,
    updatePet,
} from "../api/petApi";

import PetForm from "../components/PetForm";

import type { PetCreateRequest } from "../types/pet";

export default function PetEditPage() {
    const { petId } =
        useParams();

    const navigate =
        useNavigate();

    const [
        initialData,
        setInitialData,
    ] =
        useState<
            PetCreateRequest | null
        >(null);

    const [error, setError] =
        useState("");

    useEffect(() => {
        const loadPet =
            async () => {
                try {
                    const id =
                        Number(petId);

                    if (
                        Number.isNaN(id)
                    ) {
                        throw new Error(
                            "잘못된 반려견 ID입니다."
                        );
                    }

                    const pet =
                        await getPet(id);

                    setInitialData({
                        petName:
                        pet.petName,

                        breed:
                            pet.breed ?? "",

                        birthDate:
                        pet.birthDate,

                        weightKg:
                        pet.weightKg,

                        sizeType:
                        pet.sizeType,

                        activityLevel:
                        pet.activityLevel,

                        crowdTolerance:
                        pet.crowdTolerance,

                        placePreference:
                        pet.placePreference,

                        socialityLevel:
                        pet.socialityLevel,

                        vaccinated:
                        pet.vaccinated,

                        neutered:
                        pet.neutered,
                    });
                } catch (error) {
                    if (
                        error instanceof Error
                    ) {
                        setError(
                            error.message
                        );
                    }
                }
            };

        loadPet();
    }, [petId]);

    const handleUpdate =
        async (
            data: PetCreateRequest
        ) => {
            const id =
                Number(petId);

            await updatePet(
                id,
                data
            );

            navigate("/pets");
        };

    if (error) {
        return (
            <main className="app-page">
                <div className="app-container">
                    <p className="message message-error">{error}</p>
                </div>
            </main>
        );
    }

    if (!initialData) {
        return (
            <main className="app-page">
                <div className="app-container loading">
                    반려견 정보를 불러오는 중입니다...
                </div>
            </main>
        );
    }

    return (
        <main className="app-page">
            <div className="app-container">
                <section className="pet-form-page">
                    <header className="pet-form-header">
                        <span className="section-label">MY PET</span>
                        <h1 className="section-title">반려견 정보 수정</h1>
                        <p className="section-description">
                            변경된 반려견 정보를 업데이트해주세요.
                        </p>
                    </header>

                    <PetForm
                        initialData={initialData}
                        submitText="수정 완료"
                        onSubmit={handleUpdate}
                    />
                </section>
            </div>
        </main>
    );
}

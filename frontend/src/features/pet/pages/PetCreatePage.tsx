/* 공통 반려견 폼에서 받은 입력을 신규 등록 API로 보내고 결과에 따라 이동·안내한다. */
import { useNavigate } from "react-router-dom";
import { useState } from "react";

import { createPet } from "../api/petApi";
import PetForm from "../components/PetForm";
import PetRegistrationLookup from "../components/PetRegistrationLookup";
import type {
    PetCreateRequest,
    PetRegistrationLookupResponse,
} from "../types/pet";

export default function PetCreatePage() {
    const navigate = useNavigate();
    const [registrationLookupResult, setRegistrationLookupResult] =
        useState<PetRegistrationLookupResponse | null>(null);

    const handleCreate = async (data: PetCreateRequest) => {
        await createPet(data);
        navigate("/pets");
    };

    return (
        <main className="app-page">
            <div className="app-container">
                <section className="pet-form-page">
                    <header className="pet-form-header">
                        <span className="section-label">MY PET</span>
                        <h1 className="section-title">반려견 등록</h1>
                        <p className="section-description">
                            반려견 정보를 입력하고 맞춤형 PetWay 서비스를 시작해보세요.
                        </p>
                    </header>

                    <PetRegistrationLookup
                        onApply={setRegistrationLookupResult}
                    />

                    <PetForm
                        registrationLookupResult={registrationLookupResult}
                        submitText="등록 완료"
                        onSubmit={handleCreate}
                    />
                </section>
            </div>
        </main>
    );
}

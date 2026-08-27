import { useNavigate } from "react-router-dom";

import { createPet } from "../api/petApi";
import PetForm from "../components/PetForm";
import type { PetCreateRequest } from "../types/pet";

export default function PetCreatePage() {
    const navigate = useNavigate();

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

                    <PetForm
                        submitText="등록 완료"
                        onSubmit={handleCreate}
                    />
                </section>
            </div>
        </main>
    );
}

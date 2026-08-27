import {
    useEffect,
    useState,
} from "react";

import { Link } from "react-router-dom";

import {
    deletePet,
    getPets,
    setPrimaryPet,
} from "../api/petApi";

import PetCard from "../components/PetCard";

import type { Pet } from "../types/pet";

import "../styles/pet.css";

export default function PetListPage() {
    const [pets, setPets] =
        useState<Pet[]>([]);

    const [loading, setLoading] =
        useState(true);

    const [error, setError] =
        useState("");

    const loadPets = async () => {
        try {
            setError("");

            const data =
                await getPets();

            setPets(data);
        } catch (error) {
            if (error instanceof Error) {
                setError(error.message);
            }
        } finally {
            setLoading(false);
        }
    };

    useEffect(() => {
        loadPets();
    }, []);

    const handlePrimary = async (
        petId: number
    ) => {
        try {
            await setPrimaryPet(petId);
            await loadPets();
        } catch (error) {
            if (error instanceof Error) {
                alert(error.message);
            }
        }
    };

    const handleDelete = async (
        petId: number
    ) => {
        const confirmed =
            window.confirm(
                "반려견 정보를 삭제하시겠습니까?"
            );

        if (!confirmed) return;

        try {
            await deletePet(petId);
            await loadPets();
        } catch (error) {
            if (error instanceof Error) {
                alert(error.message);
            }
        }
    };

    if (loading) {
        return (
            <div className="app-page">
                <div className="loading">
                    반려견 정보를 불러오는 중입니다...
                </div>
            </div>
        );
    }

    return (
        <div className="app-page">
            <div className="app-container">
                <div className="page-header">
                    <div className="page-title-group">
            <span className="pet-eyebrow">
              MY PET
            </span>

                        <h1 className="page-title">
                            함께할 반려견
                        </h1>

                        <p className="page-description">
                            반려견 정보를 등록하면
                            장소 이용 가능 여부를 더 정확하게
                            확인할 수 있어요.
                        </p>
                    </div>

                    <Link
                        to="/pets/new"
                        className="btn btn-primary"
                    >
                        + 반려견 등록
                    </Link>
                </div>

                {error && (
                    <div className="message message-error">
                        {error}
                    </div>
                )}

                {pets.length === 0 ? (
                    <section className="empty-state">
                        <div className="pet-empty-icon">
                            🐾
                        </div>

                        <h2 className="empty-state-title">
                            아직 등록된 반려견이 없어요
                        </h2>

                        <p className="empty-state-description">
                            첫 반려견을 등록하고
                            PetWay 추천을 시작해보세요.
                        </p>

                        <Link
                            to="/pets/new"
                            className="btn btn-primary"
                        >
                            반려견 등록하기
                        </Link>
                    </section>
                ) : (
                    <section className="pet-grid">
                        {pets.map((pet) => (
                            <PetCard
                                key={pet.petId}
                                pet={pet}
                                onPrimary={
                                    handlePrimary
                                }
                                onDelete={
                                    handleDelete
                                }
                            />
                        ))}
                    </section>
                )}
            </div>
        </div>
    );
}

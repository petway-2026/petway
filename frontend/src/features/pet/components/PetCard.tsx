import { Link } from "react-router-dom";

import type { Pet } from "../types/pet";

interface PetCardProps {
    pet: Pet;

    onPrimary: (
        petId: number
    ) => Promise<void>;

    onDelete: (
        petId: number
    ) => Promise<void>;
}

export default function PetCard({
                                    pet,
                                    onPrimary,
                                    onDelete,
                                }: PetCardProps) {
    return (
        <article className="pet-card">
            <div className="pet-card-top">
                <div className="pet-avatar">
                    🐶
                </div>

                <div className="pet-main-info">
                    <div className="pet-name-row">
                        <h2 className="pet-name">
                            {pet.petName}
                        </h2>

                        {pet.isPrimary && (
                            <span className="badge badge-primary">
                대표견
              </span>
                        )}
                    </div>

                    <p className="pet-summary">
                        {pet.breed ?? "견종 미등록"}
                        {" · "}
                        등록 체형: {getSizeLabel(pet.sizeType)}
                        {" · "}
                        <strong>
                            {pet.weightKg}kg
                        </strong>
                    </p>
                </div>
            </div>

            <div className="pet-info-grid">
                <div className="pet-info-item">
          <span className="pet-info-label">
            생년월일
          </span>

                    <span className="pet-info-value">
            {pet.birthDate ??
                "미등록"}
          </span>
                </div>

                <div className="pet-info-item">
          <span className="pet-info-label">
            활동량
          </span>

                    <span className="pet-info-value">
            {getActivityLabel(
                pet.activityLevel
            )}
          </span>
                </div>

                <div className="pet-info-item">
          <span className="pet-info-label">
            사회성
          </span>

                    <span className="pet-info-value">
            {getSocialityLabel(
                pet.socialityLevel
            )}
          </span>
                </div>

                <div className="pet-info-item">
          <span className="pet-info-label">
            선호 공간
          </span>

                    <span className="pet-info-value">
            {getPreferenceLabel(
                pet.placePreference
            )}
          </span>
                </div>
            </div>

            <div className="pet-card-actions">
                {!pet.isPrimary && (
                    <button
                        type="button"
                        className="btn btn-soft btn-sm"
                        onClick={() =>
                            onPrimary(pet.petId)
                        }
                    >
                        대표견 지정
                    </button>
                )}

                <Link
                    to={`/pets/${pet.petId}/edit`}
                    className="btn btn-secondary btn-sm"
                >
                    정보 수정
                </Link>

                <button
                    type="button"
                    className="btn btn-danger btn-sm"
                    onClick={() =>
                        onDelete(pet.petId)
                    }
                >
                    삭제
                </button>
            </div>
        </article>
    );
}

function getActivityLabel(
    value: Pet["activityLevel"]
) {
    switch (value) {
        case "LOW":
            return "느긋해요";
        case "MEDIUM":
            return "보통이에요";
        case "HIGH":
            return "활발해요";
        default:
            return "미등록";
    }
}

function getSizeLabel(
    value: Pet["sizeType"]
) {
    switch (value) {
        case "SMALL":
            return "소형";
        case "MEDIUM":
            return "중형";
        case "LARGE":
            return "대형";
        default:
            return "크기 미등록";
    }
}

function getSocialityLabel(
    value: Pet["socialityLevel"]
) {
    switch (value) {
        case "LOW":
            return "낯가림 있어요";
        case "MEDIUM":
            return "보통이에요";
        case "HIGH":
            return "친화적이에요";
        default:
            return "미등록";
    }
}

function getPreferenceLabel(
    value: Pet["placePreference"]
) {
    switch (value) {
        case "INDOOR":
            return "실내";
        case "OUTDOOR":
            return "실외";
        case "BOTH":
            return "상관없어요";
        default:
            return "미등록";
    }
}

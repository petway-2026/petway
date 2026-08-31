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
                        {pet.breedDetail || getBreedLabel(pet.breed)}
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
                    <span className="pet-info-label">견종 분류</span>
                    <span className="pet-info-value">
                        {pet.dangerousBreed === null ? "미등록" : pet.dangerousBreed ? "법정 맹견" : "일반 견종"}
                    </span>
                </div>

                <div className="pet-info-item">
                    <span className="pet-info-label">동물등록</span>
                    <span className="pet-info-value">
                        {pet.animalRegistered === null ? "미등록" : pet.animalRegistered ? "등록 완료" : "등록 안 됨"}
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

function getBreedLabel(value: Pet["breed"]) {
    if (!value) return "견종 미등록";
    const labels: Partial<Record<NonNullable<Pet["breed"]>, string>> = {
        MALTESE: "말티즈", POODLE: "푸들", POMERANIAN: "포메라니안", SHIH_TZU: "시추",
        CHIHUAHUA: "치와와", BICHON_FRISE: "비숑 프리제", YORKSHIRE_TERRIER: "요크셔테리어",
        GOLDEN_RETRIEVER: "골든리트리버", LABRADOR_RETRIEVER: "래브라도리트리버", JINDO: "진돗개",
        SHIBA_INU: "시바견", WELSH_CORGI: "웰시코기", FRENCH_BULLDOG: "프렌치불도그",
        BEAGLE: "비글", DACHSHUND: "닥스훈트", SCHNAUZER: "슈나우저", BORDER_COLLIE: "보더콜리",
        GERMAN_SHEPHERD: "저먼 셰퍼드", SAMOYED: "사모예드", SIBERIAN_HUSKY: "시베리안 허스키",
        DOBERMAN: "도베르만", GREAT_DANE: "그레이트데인", MIXED: "믹스견",
        AKITA: "아키타", ALASKAN_MALAMUTE: "알래스칸 말라뮤트", BASENJI: "바센지",
        BOSTON_TERRIER: "보스턴테리어", BOXER: "복서", BULLDOG: "불도그", BULL_TERRIER: "불테리어",
        CANE_CORSO: "카네코르소", CAVALIER_KING_CHARLES_SPANIEL: "캐벌리어 킹 찰스 스패니얼",
        CHOW_CHOW: "차우차우", COCKER_SPANIEL: "코커스패니얼", DALMATIAN: "달마시안",
        ENGLISH_SPRINGER_SPANIEL: "잉글리시 스프링어 스패니얼", ITALIAN_GREYHOUND: "이탈리안 그레이하운드",
        JACK_RUSSELL_TERRIER: "잭 러셀 테리어", LHASA_APSO: "라사압소", MINIATURE_PINSCHER: "미니어처 핀셔",
        PAPILLON: "파피용", PEKINGESE: "페키니즈", PUG: "퍼그", ROUGH_COLLIE: "러프콜리",
        SAINT_BERNARD: "세인트버나드", SHETLAND_SHEEPDOG: "셔틀랜드 쉽독", WHIPPET: "휘핏",
        AFGHAN_HOUND: "아프간하운드", BORZOI: "보르조이", NEWFOUNDLAND: "뉴펀들랜드",
        BERNESE_MOUNTAIN_DOG: "버니즈 마운틴 독", GREAT_PYRENEES: "그레이트 피레니즈",
        OLD_ENGLISH_SHEEPDOG: "올드 잉글리시 쉽독", WEST_HIGHLAND_WHITE_TERRIER: "웨스트 하이랜드 화이트 테리어",
        AIREDALE_TERRIER: "에어데일테리어",
        TOSA: "도사견", PIT_BULL_TERRIER: "핏불테리어 계열",
        AMERICAN_STAFFORDSHIRE_TERRIER: "아메리칸 스태퍼드셔 테리어",
        STAFFORDSHIRE_BULL_TERRIER: "스태퍼드셔 불테리어", ROTTWEILER: "로트와일러",
        DANGEROUS_BREED_MIX: "맹견 계열 믹스견",
    };
    return labels[value] ?? "견종 미등록";
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

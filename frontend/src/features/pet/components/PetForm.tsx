import {
    useState,
    type FormEvent,
} from "react";

import type {
    ActivityLevel,
    CrowdTolerance,
    PetCreateRequest,
    PlacePreference,
    SocialityLevel,
} from "../types/pet";

import "../styles/pet.css";

interface PetFormProps {
    initialData?: PetCreateRequest;
    submitText: string;
    onSubmit: (
        data: PetCreateRequest
    ) => Promise<void>;
}

const defaultData: PetCreateRequest = {
    petName: "",
    breed: "",
    birthDate: null,
    weightKg: 0,

    activityLevel: null,
    crowdTolerance: null,
    placePreference: null,
    socialityLevel: null,

    vaccinated: null,
    neutered: null,
};

export default function PetForm({
                                    initialData = defaultData,
                                    submitText,
                                    onSubmit,
                                }: PetFormProps) {
    const [form, setForm] =
        useState<PetCreateRequest>(
            initialData
        );

    const [error, setError] =
        useState("");

    const [loading, setLoading] =
        useState(false);

    const handleSubmit = async (
        event: FormEvent<HTMLFormElement>
    ) => {
        event.preventDefault();

        setError("");

        if (!form.petName.trim()) {
            setError(
                "반려견 이름을 입력해주세요."
            );
            return;
        }

        if (form.weightKg <= 0) {
            setError(
                "체중은 0kg보다 커야 합니다."
            );
            return;
        }

        try {
            setLoading(true);

            await onSubmit(form);
        } catch (error) {
            if (error instanceof Error) {
                setError(error.message);
            } else {
                setError(
                    "처리에 실패했습니다."
                );
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <form className="card pet-form-card" onSubmit={handleSubmit}>
            <div>
                <label htmlFor="petName">
                    이름 *
                </label>

                <input
                    id="petName"
                    type="text"
                    value={form.petName}
                    onChange={(event) =>
                        setForm({
                            ...form,
                            petName:
                            event.target.value,
                        })
                    }
                    required
                />
            </div>

            <div>
                <label htmlFor="breed">
                    견종
                </label>

                <input
                    id="breed"
                    type="text"
                    value={form.breed}
                    onChange={(event) =>
                        setForm({
                            ...form,
                            breed:
                            event.target.value,
                        })
                    }
                />
            </div>

            <div>
                <label htmlFor="birthDate">
                    생년월일
                </label>

                <input
                    id="birthDate"
                    type="date"
                    value={
                        form.birthDate ?? ""
                    }
                    onChange={(event) =>
                        setForm({
                            ...form,
                            birthDate:
                                event.target.value ||
                                null,
                        })
                    }
                />
            </div>

            <div>
                <label htmlFor="weightKg">
                    체중(kg) *
                </label>

                <input
                    id="weightKg"
                    type="number"
                    step="0.01"
                    min="0.01"
                    value={
                        form.weightKg || ""
                    }
                    onChange={(event) =>
                        setForm({
                            ...form,
                            weightKg: Number(
                                event.target.value
                            ),
                        })
                    }
                    required
                />
            </div>

            <div>
                <label htmlFor="activityLevel">
                    활동량
                </label>

                <select
                    id="activityLevel"
                    value={
                        form.activityLevel ?? ""
                    }
                    onChange={(event) =>
                        setForm({
                            ...form,
                            activityLevel:
                                event.target.value
                                    ? (event.target
                                        .value as ActivityLevel)
                                    : null,
                        })
                    }
                >
                    <option value="">
                        선택 안 함
                    </option>
                    <option value="LOW">
                        낮음
                    </option>
                    <option value="MEDIUM">
                        보통
                    </option>
                    <option value="HIGH">
                        높음
                    </option>
                </select>
            </div>

            <div>
                <label htmlFor="crowdTolerance">
                    혼잡한 장소 적응도
                </label>

                <select
                    id="crowdTolerance"
                    value={
                        form.crowdTolerance ??
                        ""
                    }
                    onChange={(event) =>
                        setForm({
                            ...form,
                            crowdTolerance:
                                event.target.value
                                    ? (event.target
                                        .value as CrowdTolerance)
                                    : null,
                        })
                    }
                >
                    <option value="">
                        선택 안 함
                    </option>
                    <option value="LOW">
                        낮음
                    </option>
                    <option value="MEDIUM">
                        보통
                    </option>
                    <option value="HIGH">
                        높음
                    </option>
                </select>
            </div>

            <div>
                <label htmlFor="placePreference">
                    선호 장소
                </label>

                <select
                    id="placePreference"
                    value={
                        form.placePreference ??
                        ""
                    }
                    onChange={(event) =>
                        setForm({
                            ...form,
                            placePreference:
                                event.target.value
                                    ? (event.target
                                        .value as PlacePreference)
                                    : null,
                        })
                    }
                >
                    <option value="">
                        선택 안 함
                    </option>
                    <option value="INDOOR">
                        실내
                    </option>
                    <option value="OUTDOOR">
                        실외
                    </option>
                    <option value="BOTH">
                        둘 다
                    </option>
                </select>
            </div>

            <div>
                <label htmlFor="socialityLevel">
                    사회성
                </label>

                <select
                    id="socialityLevel"
                    value={
                        form.socialityLevel ??
                        ""
                    }
                    onChange={(event) =>
                        setForm({
                            ...form,
                            socialityLevel:
                                event.target.value
                                    ? (event.target
                                        .value as SocialityLevel)
                                    : null,
                        })
                    }
                >
                    <option value="">
                        선택 안 함
                    </option>
                    <option value="LOW">
                        낮음
                    </option>
                    <option value="MEDIUM">
                        보통
                    </option>
                    <option value="HIGH">
                        높음
                    </option>
                </select>
            </div>

            <div>
                <label htmlFor="vaccinated">
                    예방접종 여부
                </label>

                <select
                    id="vaccinated"
                    value={
                        form.vaccinated === null
                            ? ""
                            : String(
                                form.vaccinated
                            )
                    }
                    onChange={(event) => {
                        const value =
                            event.target.value;

                        setForm({
                            ...form,
                            vaccinated:
                                value === ""
                                    ? null
                                    : value ===
                                    "true",
                        });
                    }}
                >
                    <option value="">
                        선택 안 함
                    </option>
                    <option value="true">
                        완료
                    </option>
                    <option value="false">
                        미완료
                    </option>
                </select>
            </div>

            <div>
                <label htmlFor="neutered">
                    중성화 여부
                </label>

                <select
                    id="neutered"
                    value={
                        form.neutered === null
                            ? ""
                            : String(
                                form.neutered
                            )
                    }
                    onChange={(event) => {
                        const value =
                            event.target.value;

                        setForm({
                            ...form,
                            neutered:
                                value === ""
                                    ? null
                                    : value ===
                                    "true",
                        });
                    }}
                >
                    <option value="">
                        선택 안 함
                    </option>
                    <option value="true">
                        완료
                    </option>
                    <option value="false">
                        미완료
                    </option>
                </select>
            </div>

            {error && <p>{error}</p>}

            <button
                type="submit"
                disabled={loading}
            >
                {loading
                    ? "저장 중..."
                    : submitText}
            </button>
        </form>
    );
}

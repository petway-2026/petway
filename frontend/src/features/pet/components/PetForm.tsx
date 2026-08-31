import {
    useState,
    type FormEvent,
} from "react";

import type {
    ActivityLevel,
    CrowdTolerance,
    PetCreateRequest,
    PetSizeType,
    DogBreed,
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

const DOG_BREEDS = ([
    { value: "MALTESE", label: "말티즈" }, { value: "POODLE", label: "푸들" },
    { value: "POMERANIAN", label: "포메라니안" }, { value: "SHIH_TZU", label: "시추" },
    { value: "CHIHUAHUA", label: "치와와" }, { value: "BICHON_FRISE", label: "비숑 프리제" },
    { value: "YORKSHIRE_TERRIER", label: "요크셔테리어" }, { value: "GOLDEN_RETRIEVER", label: "골든리트리버" },
    { value: "LABRADOR_RETRIEVER", label: "래브라도리트리버" }, { value: "JINDO", label: "진돗개" },
    { value: "SHIBA_INU", label: "시바견" }, { value: "WELSH_CORGI", label: "웰시코기" },
    { value: "FRENCH_BULLDOG", label: "프렌치불도그" }, { value: "BEAGLE", label: "비글" },
    { value: "DACHSHUND", label: "닥스훈트" }, { value: "SCHNAUZER", label: "슈나우저" },
    { value: "BORDER_COLLIE", label: "보더콜리" }, { value: "GERMAN_SHEPHERD", label: "저먼 셰퍼드" },
    { value: "SAMOYED", label: "사모예드" }, { value: "SIBERIAN_HUSKY", label: "시베리안 허스키" },
    { value: "DOBERMAN", label: "도베르만" }, { value: "GREAT_DANE", label: "그레이트데인" },
    { value: "AKITA", label: "아키타" }, { value: "ALASKAN_MALAMUTE", label: "알래스칸 말라뮤트" },
    { value: "BASENJI", label: "바센지" }, { value: "BOSTON_TERRIER", label: "보스턴테리어" },
    { value: "BOXER", label: "복서" }, { value: "BULLDOG", label: "불도그" },
    { value: "BULL_TERRIER", label: "불테리어" }, { value: "CANE_CORSO", label: "카네코르소" },
    { value: "CAVALIER_KING_CHARLES_SPANIEL", label: "캐벌리어 킹 찰스 스패니얼" },
    { value: "CHOW_CHOW", label: "차우차우" }, { value: "COCKER_SPANIEL", label: "코커스패니얼" },
    { value: "DALMATIAN", label: "달마시안" }, { value: "ENGLISH_SPRINGER_SPANIEL", label: "잉글리시 스프링어 스패니얼" },
    { value: "ITALIAN_GREYHOUND", label: "이탈리안 그레이하운드" }, { value: "JACK_RUSSELL_TERRIER", label: "잭 러셀 테리어" },
    { value: "LHASA_APSO", label: "라사압소" }, { value: "MINIATURE_PINSCHER", label: "미니어처 핀셔" },
    { value: "PAPILLON", label: "파피용" }, { value: "PEKINGESE", label: "페키니즈" },
    { value: "PUG", label: "퍼그" }, { value: "ROUGH_COLLIE", label: "러프콜리" },
    { value: "SAINT_BERNARD", label: "세인트버나드" }, { value: "SHETLAND_SHEEPDOG", label: "셔틀랜드 쉽독" },
    { value: "WHIPPET", label: "휘핏" }, { value: "AFGHAN_HOUND", label: "아프간하운드" },
    { value: "BORZOI", label: "보르조이" }, { value: "NEWFOUNDLAND", label: "뉴펀들랜드" },
    { value: "BERNESE_MOUNTAIN_DOG", label: "버니즈 마운틴 독" }, { value: "GREAT_PYRENEES", label: "그레이트 피레니즈" },
    { value: "OLD_ENGLISH_SHEEPDOG", label: "올드 잉글리시 쉽독" },
    { value: "WEST_HIGHLAND_WHITE_TERRIER", label: "웨스트 하이랜드 화이트 테리어" },
    { value: "AIREDALE_TERRIER", label: "에어데일테리어" }, { value: "MIXED", label: "믹스견" },
    { value: "TOSA", label: "도사견" }, { value: "PIT_BULL_TERRIER", label: "핏불테리어" },
    { value: "AMERICAN_STAFFORDSHIRE_TERRIER", label: "아메리칸 스태퍼드셔 테리어" },
    { value: "STAFFORDSHIRE_BULL_TERRIER", label: "스태퍼드셔 불테리어" },
    { value: "ROTTWEILER", label: "로트와일러" }, { value: "DANGEROUS_BREED_MIX", label: "해당 견종이 포함된 믹스견" },
] satisfies Array<{ value: DogBreed; label: string }>).sort(
    (left, right) => left.label.localeCompare(right.label, "ko")
);

const defaultData: PetCreateRequest = {
    petName: "",
    breedDetail: "",
    breed: null,
    animalRegistered: null,
    birthDate: null,
    weightKg: 0,
    sizeType: null,

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

    const [breedSearch, setBreedSearch] = useState(
        DOG_BREEDS.find((breed) => breed.value === initialData.breed)?.label ?? ""
    );
    const [breedSelectionOpen, setBreedSelectionOpen] = useState(!initialData.breed);

    const filteredBreeds = DOG_BREEDS.filter((breed) =>
        breed.label.replace(/\s/g, "").includes(breedSearch.replace(/\s/g, ""))
    );

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

        if (!form.breed) {
            setError("검색 결과에서 견종을 선택해주세요.");
            return;
        }

        if (!Number.isInteger(form.weightKg)) {
            setError("체중은 소수점 없이 정수로 입력해주세요.");
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
                <label htmlFor="breed">견종</label>
                {breedSelectionOpen ? (
                    <>
                        <input
                            id="breedSearch"
                            type="search"
                            placeholder="견종명을 입력해 빠르게 찾아보세요"
                            value={breedSearch}
                            onChange={(event) => {
                                setBreedSearch(event.target.value);
                                setForm({ ...form, breed: null, breedDetail: "" });
                            }}
                            autoFocus
                        />
                        {filteredBreeds.length > 0 ? (
                            <div className="pet-breed-results" role="listbox" aria-label="견종 검색 결과">
                                {filteredBreeds.map((breed) => (
                                    <button
                                        type="button"
                                        role="option"
                                        aria-selected={form.breed === breed.value}
                                        key={breed.value}
                                        onClick={() => {
                                            setForm({
                                                ...form,
                                                breed: breed.value,
                                                breedDetail: breed.value === "MIXED" ? form.breedDetail : "",
                                            });
                                            setBreedSearch(breed.label);
                                            setBreedSelectionOpen(false);
                                        }}
                                    >
                                        {breed.label}
                                    </button>
                                ))}
                            </div>
                        ) : (
                            <p className="pet-size-guidance">일치하는 견종이 없습니다. 다른 이름으로 검색해주세요.</p>
                        )}
                    </>
                ) : (
                    <div className="pet-breed-selected">
                        <strong>{breedSearch}</strong>
                        <button
                            type="button"
                            onClick={() => {
                                setBreedSearch("");
                                setForm({ ...form, breed: null, breedDetail: "" });
                                setBreedSelectionOpen(true);
                            }}
                        >
                            변경
                        </button>
                    </div>
                )}
                {form.breed === "MIXED" && (
                    <input
                        id="breedDetail"
                        type="text"
                        placeholder="견종명 또는 믹스 정보를 입력해주세요"
                        value={form.breedDetail}
                        onChange={(event) => setForm({ ...form, breedDetail: event.target.value })}
                    />
                )}
            </div>

            <div>
                <label htmlFor="animalRegistered">동물등록 여부</label>
                <select
                    id="animalRegistered"
                    value={form.animalRegistered === null ? "" : String(form.animalRegistered)}
                    onChange={(event) => setForm({
                        ...form,
                        animalRegistered: event.target.value === "" ? null : event.target.value === "true",
                    })}
                >
                    <option value="">선택 안 함</option>
                    <option value="true">등록 완료</option>
                    <option value="false">미등록</option>
                </select>
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
                    type="text"
                    inputMode="numeric"
                    pattern="[0-9]*"
                    value={
                        form.weightKg || ""
                    }
                    onChange={(event) => {
                        const digits = event.target.value.replace(/\D/g, "");
                        setForm({ ...form, weightKg: digits ? Number(digits) : 0 });
                    }}
                    onWheel={(event) => event.currentTarget.blur()}
                    required
                />
            </div>

            <div>
                <label htmlFor="sizeType">
                    체형 분류 (선택)
                </label>

                <select
                    id="sizeType"
                    value={form.sizeType ?? ""}
                    onChange={(event) =>
                        setForm({
                            ...form,
                            sizeType:
                                event.target.value
                                    ? (event.target.value as PetSizeType)
                                    : null,
                        })
                    }
                >
                    <option value="">
                        잘 모르겠어요
                    </option>
                    <option value="SMALL">
                        소형으로 등록
                    </option>
                    <option value="MEDIUM">
                        중형으로 등록
                    </option>
                    <option value="LARGE">
                        대형으로 등록
                    </option>
                </select>

                <p className="pet-size-guidance">
                    체중만으로 자동 분류하지 않습니다. 견종과 체형을 고려해 선택하고,
                    애매하면 비워두세요. 실제 이용 기준은 장소마다 다를 수 있습니다.
                </p>
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

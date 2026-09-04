import {
    useEffect,
    useRef,
    useState,
    type FormEvent,
} from "react";

import {
    lookupPetRegistration,
    PetRegistrationLookupApiError,
} from "../api/petApi";
import type {
    PetRegistrationLookupRequest,
    PetRegistrationLookupResponse,
} from "../types/pet";

type AnimalIdentifierType = "REGISTRATION_NUMBER" | "RFID";
type OwnerIdentifierType = "NAME" | "BIRTH_DATE";

interface PetRegistrationLookupProps {
    onApply: (result: PetRegistrationLookupResponse) => void;
}

const ERROR_MESSAGES: Record<number, string> = {
    400: "입력한 조회 조건을 다시 확인해주세요.",
    401: "로그인이 필요합니다. 다시 로그인한 뒤 이용해주세요.",
    403: "로그인 정보가 만료되었습니다. 다시 로그인해주세요.",
    404: "일치하는 동물등록정보를 찾을 수 없습니다.",
    502: "동물등록정보 제공기관 호출에 실패했습니다. 잠시 후 다시 시도해주세요.",
    503: "현재 동물등록정보 조회 기능을 사용할 수 없습니다.",
    504: "동물등록정보 제공기관의 응답이 지연되고 있습니다. 다시 시도해주세요.",
};

export default function PetRegistrationLookup({
    onApply,
}: PetRegistrationLookupProps) {
    const [open, setOpen] = useState(false);
    const [animalIdentifierType, setAnimalIdentifierType] =
        useState<AnimalIdentifierType>("REGISTRATION_NUMBER");
    const [ownerIdentifierType, setOwnerIdentifierType] =
        useState<OwnerIdentifierType>("NAME");
    const [animalIdentifier, setAnimalIdentifier] = useState("");
    const [ownerIdentifier, setOwnerIdentifier] = useState("");
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState("");
    const [notice, setNotice] = useState("");
    const firstInputRef = useRef<HTMLInputElement>(null);
    const requestControllerRef = useRef<AbortController | null>(null);

    const clearSensitiveInputs = () => {
        setAnimalIdentifier("");
        setOwnerIdentifier("");
        setError("");
    };

    const closeModal = () => {
        requestControllerRef.current?.abort();
        requestControllerRef.current = null;
        setLoading(false);
        clearSensitiveInputs();
        setOpen(false);
    };

    useEffect(() => {
        if (!open) {
            return;
        }

        firstInputRef.current?.focus();

        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") {
                closeModal();
            }
        };

        window.addEventListener("keydown", handleKeyDown);
        return () => window.removeEventListener("keydown", handleKeyDown);
    }, [open]);

    useEffect(() => () => requestControllerRef.current?.abort(), []);

    const handleSubmit = async (event: FormEvent<HTMLFormElement>) => {
        event.preventDefault();
        setError("");
        setNotice("");

        const trimmedAnimalIdentifier = animalIdentifier.trim();
        const trimmedOwnerIdentifier = ownerIdentifier.trim();

        if (!trimmedAnimalIdentifier) {
            setError(
                animalIdentifierType === "REGISTRATION_NUMBER"
                    ? "동물등록번호를 입력해주세요."
                    : "RFID 번호를 입력해주세요."
            );
            return;
        }

        if (!trimmedOwnerIdentifier) {
            setError(
                ownerIdentifierType === "NAME"
                    ? "소유자명을 입력해주세요."
                    : "소유자 생년월일을 입력해주세요."
            );
            return;
        }

        const request: PetRegistrationLookupRequest = {
            animalRegistrationNumber:
                animalIdentifierType === "REGISTRATION_NUMBER"
                    ? trimmedAnimalIdentifier
                    : null,
            rfidCode:
                animalIdentifierType === "RFID"
                    ? trimmedAnimalIdentifier
                    : null,
            ownerName:
                ownerIdentifierType === "NAME"
                    ? trimmedOwnerIdentifier
                    : null,
            ownerBirthDate:
                ownerIdentifierType === "BIRTH_DATE"
                    ? trimmedOwnerIdentifier.replaceAll("-", "")
                    : null,
        };

        const controller = new AbortController();
        requestControllerRef.current = controller;

        try {
            setLoading(true);
            const result = await lookupPetRegistration(request, controller.signal);
            onApply(result);
            setNotice("동물등록정보를 불러왔습니다. 내용을 확인하고 부족한 정보를 입력해주세요.");
            closeModal();
        } catch (caughtError) {
            if (caughtError instanceof DOMException && caughtError.name === "AbortError") {
                return;
            }

            if (caughtError instanceof PetRegistrationLookupApiError) {
                setError(
                    ERROR_MESSAGES[caughtError.status] ??
                    "동물등록정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요."
                );
                return;
            }

            setError("동물등록정보를 불러오지 못했습니다. 잠시 후 다시 시도해주세요.");
        } finally {
            if (requestControllerRef.current === controller) {
                requestControllerRef.current = null;
                setLoading(false);
            }
        }
    };

    return (
        <section className="pet-registration-lookup" aria-labelledby="pet-registration-lookup-title">
            <div>
                <h2 id="pet-registration-lookup-title">동물등록정보가 있나요?</h2>
                <p>등록정보를 불러오면 이름, 생년월일, 견종과 중성화 여부를 자동으로 채워드려요.</p>
            </div>
            <button
                type="button"
                className="pet-registration-lookup-button"
                onClick={() => {
                    setNotice("");
                    setOpen(true);
                }}
            >
                동물등록정보 불러오기
            </button>

            {notice && (
                <p className="pet-registration-lookup-notice" role="status">
                    {notice}
                </p>
            )}

            {open && (
                <div className="pet-registration-modal-backdrop" onMouseDown={closeModal}>
                    <section
                        className="pet-registration-modal"
                        role="dialog"
                        aria-modal="true"
                        aria-labelledby="pet-registration-modal-title"
                        onMouseDown={(event) => event.stopPropagation()}
                    >
                        <div className="pet-registration-modal-header">
                            <div>
                                <span>REGISTRATION LOOKUP</span>
                                <h2 id="pet-registration-modal-title">동물등록정보 불러오기</h2>
                            </div>
                            <button
                                type="button"
                                className="pet-registration-modal-close"
                                onClick={closeModal}
                                aria-label="동물등록정보 조회 창 닫기"
                            >
                                ×
                            </button>
                        </div>

                        <p className="pet-registration-modal-description">
                            조회정보는 등록 여부 확인에만 사용하며 Pet 정보로 저장하지 않습니다.
                        </p>

                        <form onSubmit={handleSubmit}>
                            <fieldset>
                                <legend>동물 식별 방법</legend>
                                <div className="pet-registration-methods">
                                    <label>
                                        <input
                                            type="radio"
                                            name="animalIdentifierType"
                                            value="REGISTRATION_NUMBER"
                                            checked={animalIdentifierType === "REGISTRATION_NUMBER"}
                                            onChange={() => {
                                                setAnimalIdentifierType("REGISTRATION_NUMBER");
                                                setAnimalIdentifier("");
                                                setError("");
                                            }}
                                        />
                                        동물등록번호
                                    </label>
                                    <label>
                                        <input
                                            type="radio"
                                            name="animalIdentifierType"
                                            value="RFID"
                                            checked={animalIdentifierType === "RFID"}
                                            onChange={() => {
                                                setAnimalIdentifierType("RFID");
                                                setAnimalIdentifier("");
                                                setError("");
                                            }}
                                        />
                                        RFID
                                    </label>
                                </div>
                                <input
                                    ref={firstInputRef}
                                    type="text"
                                    inputMode="numeric"
                                    autoComplete="off"
                                    value={animalIdentifier}
                                    onChange={(event) => setAnimalIdentifier(event.target.value)}
                                    placeholder={
                                        animalIdentifierType === "REGISTRATION_NUMBER"
                                            ? "동물등록번호를 입력해주세요"
                                            : "RFID 번호를 입력해주세요"
                                    }
                                    aria-label={
                                        animalIdentifierType === "REGISTRATION_NUMBER"
                                            ? "동물등록번호"
                                            : "RFID 번호"
                                    }
                                />
                            </fieldset>

                            <fieldset>
                                <legend>소유자 확인 방법</legend>
                                <div className="pet-registration-methods">
                                    <label>
                                        <input
                                            type="radio"
                                            name="ownerIdentifierType"
                                            value="NAME"
                                            checked={ownerIdentifierType === "NAME"}
                                            onChange={() => {
                                                setOwnerIdentifierType("NAME");
                                                setOwnerIdentifier("");
                                                setError("");
                                            }}
                                        />
                                        소유자명
                                    </label>
                                    <label>
                                        <input
                                            type="radio"
                                            name="ownerIdentifierType"
                                            value="BIRTH_DATE"
                                            checked={ownerIdentifierType === "BIRTH_DATE"}
                                            onChange={() => {
                                                setOwnerIdentifierType("BIRTH_DATE");
                                                setOwnerIdentifier("");
                                                setError("");
                                            }}
                                        />
                                        생년월일
                                    </label>
                                </div>
                                <input
                                    type={ownerIdentifierType === "BIRTH_DATE" ? "date" : "text"}
                                    autoComplete="off"
                                    value={ownerIdentifier}
                                    onChange={(event) => setOwnerIdentifier(event.target.value)}
                                    placeholder={ownerIdentifierType === "NAME" ? "등록된 소유자명을 입력해주세요" : undefined}
                                    aria-label={ownerIdentifierType === "NAME" ? "소유자명" : "소유자 생년월일"}
                                />
                            </fieldset>

                            {error && (
                                <p className="pet-registration-modal-error" role="alert">
                                    {error}
                                </p>
                            )}

                            <div className="pet-registration-modal-actions">
                                <button type="button" onClick={closeModal} disabled={loading}>
                                    취소
                                </button>
                                <button type="submit" disabled={loading}>
                                    {loading ? "조회 중..." : "등록정보 조회"}
                                </button>
                            </div>
                        </form>
                    </section>
                </div>
            )}
        </section>
    );
}

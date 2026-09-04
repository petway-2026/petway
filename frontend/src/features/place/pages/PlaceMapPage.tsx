import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { getPets } from "../../pet/api/petApi";
import NearbyPlacesSection from "../components/NearbyPlacesSection";
import WalkFitRecommendationsSection from "../../walk/components/WalkFitRecommendationsSection";
import { useAuth } from "../../auth/context/AuthContext";

type PetRegistrationStatus =
    | "LOADING"
    | "REGISTERED"
    | "NOT_REGISTERED"
    | "UNAVAILABLE";

export default function PlaceMapPage() {
    const { user, loading: authLoading } = useAuth();
    const [petRegistrationStatus, setPetRegistrationStatus] =
        useState<PetRegistrationStatus>("LOADING");

    useEffect(() => {
        let mounted = true;

        if (authLoading) return () => { mounted = false; };
        if (!user) {
            setPetRegistrationStatus("UNAVAILABLE");
            return () => { mounted = false; };
        }

        getPets()
            .then((pets) => {
                if (mounted) {
                    setPetRegistrationStatus(
                        pets.length > 0 ? "REGISTERED" : "NOT_REGISTERED"
                    );
                }
            })
            .catch(() => {
                if (mounted) setPetRegistrationStatus("UNAVAILABLE");
            });

        return () => {
            mounted = false;
        };
    }, [authLoading, user]);

    return (
        <main className="place-map-page">
            {!authLoading && !user && (
                <aside className="pet-registration-prompt">
                    <div className="pet-registration-prompt-icon" aria-hidden="true">🐾</div>
                    <div className="pet-registration-prompt-copy">
                        <strong>로그인하면 마이펫 맞춤 추천을 이용할 수 있어요.</strong>
                        <span>장소 지도와 날씨는 로그인하지 않아도 계속 확인할 수 있습니다.</span>
                    </div>
                    <Link className="pet-registration-prompt-link" to="/login">로그인하기</Link>
                </aside>
            )}
            {user && petRegistrationStatus === "NOT_REGISTERED" && (
                <aside className="pet-registration-prompt">
                    <div className="pet-registration-prompt-icon" aria-hidden="true">
                        🐾
                    </div>
                    <div className="pet-registration-prompt-copy">
                        <strong>우리 아이 정보를 먼저 등록해 볼까요?</strong>
                        <span>
                            반려견을 등록하면 아이에게 맞는 장소를 찾는 데 활용할 수 있어요.
                            등록하지 않아도 지도와 날씨는 계속 확인할 수 있습니다.
                        </span>
                    </div>
                    <Link className="pet-registration-prompt-link" to="/pets/new">
                        내 펫 등록하기
                    </Link>
                </aside>
            )}

            {user && <WalkFitRecommendationsSection />}

            <NearbyPlacesSection
                showFullMapButton={false}
                enableWeatherSelection
            />
        </main>
    );
}

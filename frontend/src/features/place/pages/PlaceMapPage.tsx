import { useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { getPets } from "../../pet/api/petApi";
import NearbyPlacesSection from "../components/NearbyPlacesSection";
import WalkFitRecommendationsSection from "../../walk/components/WalkFitRecommendationsSection";

type PetRegistrationStatus =
    | "LOADING"
    | "REGISTERED"
    | "NOT_REGISTERED"
    | "UNAVAILABLE";

export default function PlaceMapPage() {
    const [petRegistrationStatus, setPetRegistrationStatus] =
        useState<PetRegistrationStatus>("LOADING");

    useEffect(() => {
        let mounted = true;

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
    }, []);

    return (
        <main className="place-map-page">
            {petRegistrationStatus === "NOT_REGISTERED" && (
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

            <WalkFitRecommendationsSection />

            <NearbyPlacesSection
                showFullMapButton={false}
                enableWeatherSelection
            />
        </main>
    );
}

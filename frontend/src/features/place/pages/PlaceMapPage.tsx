import { useCallback, useEffect, useState } from "react";
import { Link } from "react-router-dom";

import { getPets } from "../../pet/api/petApi";
import KakaoMap from "../components/KakaoMap";
import PlaceWeatherPanel from "../components/PlaceWeatherPanel";
import type { MapMarker } from "../types/map";
import type { SelectedPlace } from "../types/place";

import "../styles/map.css";

// KorPetTourService2 장소 목록 응답을 연결할 자리입니다.
// 운영 화면에는 임의의 장소 마커를 노출하지 않습니다.
const placeMarkers: SelectedPlace[] = [];

type PetRegistrationStatus =
  | "LOADING"
  | "REGISTERED"
  | "NOT_REGISTERED"
  | "UNAVAILABLE";

export default function PlaceMapPage() {
  const [selectedPlace, setSelectedPlace] = useState<SelectedPlace | null>(
    null,
  );
  const [petRegistrationStatus, setPetRegistrationStatus] =
    useState<PetRegistrationStatus>("LOADING");

  useEffect(() => {
    let mounted = true;

    getPets()
      .then((pets) => {
        if (mounted) {
          setPetRegistrationStatus(
            pets.length > 0 ? "REGISTERED" : "NOT_REGISTERED",
          );
        }
      })
      .catch(() => {
        if (mounted) {
          setPetRegistrationStatus("UNAVAILABLE");
        }
      });

    return () => {
      mounted = false;
    };
  }, []);

  const handleMarkerSelect = useCallback((marker: MapMarker | null) => {
    if (!marker) {
      setSelectedPlace(null);
      return;
    }

    const place = placeMarkers.find((candidate) => candidate.id === marker.id);
    setSelectedPlace(place ?? null);
  }, []);

  const handleClosePanel = useCallback(() => {
    setSelectedPlace(null);
  }, []);

  return (
    <main className="place-map-page">
      <div className="place-map-container">
        <section className="place-map-heading">
          <span className="place-map-eyebrow">EXPLORE PETWAY</span>

          <h1>
            우리 아이와
            <br />
            어디로 갈까요?
          </h1>

          <p>
            반려견과 함께 갈 수 있는 관광지를 지도에서 찾아보고, 여행일의
            날씨까지 미리 확인해 보세요.
          </p>
        </section>

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

        <section
          className={
            selectedPlace
              ? "place-map-content has-selection"
              : "place-map-content"
          }
        >
          <div className="place-map-toolbar">
            <div className="place-map-toolbar-left">
              <strong>주변 장소</strong>
              <span>장소 마커를 선택하면 여행일 날씨를 확인할 수 있어요.</span>
            </div>

            <div className="place-map-filter" aria-label="장소 유형 필터">
              <button type="button" className="active">
                전체
              </button>
              <button type="button">관광지</button>
              <button type="button">음식점</button>
              <button type="button">숙박</button>
            </div>
          </div>

          <div className="place-map-workspace">
            <div className="place-map-canvas">
              <KakaoMap
                center={{
                  latitude: 37.5665,
                  longitude: 126.978,
                }}
                level={5}
                markers={placeMarkers}
                selectedMarkerId={selectedPlace?.id ?? null}
                onMarkerSelect={handleMarkerSelect}
              />

              {placeMarkers.length === 0 && (
                <div className="place-map-empty" aria-live="polite">
                  <span aria-hidden="true">🗺️</span>
                  <strong>표시할 장소가 아직 없어요.</strong>
                  <p>
                    장소 검색 데이터가 연결되면 지도에서 선택할 수 있습니다.
                  </p>
                </div>
              )}
            </div>

            {selectedPlace && (
              <PlaceWeatherPanel
                place={selectedPlace}
                onClose={handleClosePanel}
              />
            )}
          </div>
        </section>
      </div>
    </main>
  );
}

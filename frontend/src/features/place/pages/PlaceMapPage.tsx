import KakaoMap from "../components/KakaoMap";

import type {
    MapMarker,
} from "../types/map";

import "../styles/map.css";

const testMarkers: MapMarker[] = [
    {
        id: 1,
        title: "서울시청",
        latitude: 37.5663,
        longitude: 126.9779,
    },
    {
        id: 2,
        title: "덕수궁",
        latitude: 37.5658,
        longitude: 126.9751,
    },
    {
        id: 3,
        title: "광화문",
        latitude: 37.5759,
        longitude: 126.9768,
    },
];

export default function PlaceMapPage() {
    return (
        <main className="place-map-page">
            <div className="place-map-container">

                <section className="place-map-heading">
          <span className="place-map-eyebrow">
            EXPLORE PETWAY
          </span>

                    <h1>
                        우리 아이와
                        <br />
                        어디로 갈까요?
                    </h1>

                    <p>
                        반려견과 함께 갈 수 있는
                        관광지를 지도에서 찾아보세요.
                    </p>
                </section>

                <section className="place-map-content">
                    <div className="place-map-toolbar">

                        <div className="place-map-toolbar-left">
                            <strong>
                                주변 장소
                            </strong>

                            <span>
                지도에서 장소를 확인해보세요.
              </span>
                        </div>

                        <div className="place-map-filter">
                            <button
                                type="button"
                                className="active"
                            >
                                전체
                            </button>

                            <button
                                type="button"
                            >
                                관광지
                            </button>

                            <button
                                type="button"
                            >
                                음식점
                            </button>

                            <button
                                type="button"
                            >
                                숙박
                            </button>
                        </div>
                    </div>

                    <KakaoMap
                        center={{
                            latitude:
                                37.5665,

                            longitude:
                                126.9780,
                        }}
                        level={5}
                        markers={
                            testMarkers
                        }
                    />
                </section>

            </div>
        </main>
    );
}
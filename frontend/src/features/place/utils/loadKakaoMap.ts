/* 카카오 지도·주소 변환 SDK의 로딩 Promise를 공유해 여러 컴포넌트의 중복 스크립트 삽입을 막는다. */
let kakaoMapPromise: Promise<void> | null = null;

export function loadKakaoMap(): Promise<void> {
    if (window.kakao?.maps?.services) {
        return Promise.resolve();
    }

    // 여러 지도 화면이 동시에 마운트돼도 진행 중인 SDK 로딩을 공유한다.
    if (kakaoMapPromise) {
        return kakaoMapPromise;
    }

    kakaoMapPromise = new Promise((resolve, reject) => {
        const appKey =
            import.meta.env.VITE_KAKAO_MAP_KEY;

        if (!appKey) {
            reject(
                new Error(
                    "VITE_KAKAO_MAP_KEY가 설정되어 있지 않습니다."
                )
            );

            return;
        }

        const existingScript =
            document.getElementById(
                "kakao-map-sdk"
            ) as HTMLScriptElement | null;

        if (existingScript) {
            if (window.kakao?.maps?.services) {
                window.kakao.maps.load(() => {
                    resolve();
                });

                return;
            }

            // An older SDK tag may already be on the page without the
            // services library. It cannot be upgraded in place safely.
            if (window.kakao?.maps) {
                reject(new Error("주소 변환용 카카오 지도 서비스를 불러오지 못했습니다. 페이지를 새로고침해 주세요."));
                return;
            }

            existingScript.addEventListener(
                "load",
                () => {
                    window.kakao.maps.load(() => {
                        resolve();
                    });
                }
            );

            existingScript.addEventListener(
                "error",
                () => {
                    reject(
                        new Error(
                            "카카오 지도 SDK를 불러오지 못했습니다."
                        )
                    );
                }
            );

            return;
        }

        const script =
            document.createElement("script");

        script.id =
            "kakao-map-sdk";

        script.src =
            `https://dapi.kakao.com/v2/maps/sdk.js` +
            `?appkey=${appKey}` +
            `&libraries=services` +
            `&autoload=false`;

        script.async =
            true;

        script.onload = () => {
            if (!window.kakao?.maps) {
                reject(
                    new Error(
                        "카카오 지도 객체를 찾을 수 없습니다."
                    )
                );

                return;
            }

            window.kakao.maps.load(() => {
                resolve();
            });
        };

        script.onerror = () => {
            reject(
                new Error(
                    "카카오 지도 SDK를 불러오지 못했습니다."
                )
            );
        };

        document.head.appendChild(
            script
        );
    });

    return kakaoMapPromise;
}

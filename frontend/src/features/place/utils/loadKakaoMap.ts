let kakaoMapPromise: Promise<void> | null = null;

export function loadKakaoMap(): Promise<void> {
    if (window.kakao?.maps) {
        return Promise.resolve();
    }

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
            if (window.kakao?.maps) {
                window.kakao.maps.load(() => {
                    resolve();
                });

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
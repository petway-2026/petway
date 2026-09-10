const configuredUrl = import.meta.env.VITE_API_URL?.trim();

if (!configuredUrl) {
    throw new Error("VITE_API_URL 환경변수에 백엔드 주소를 설정해 주세요.");
}

function resolveApiUrl(): string {
    if (configuredUrl === "/") {
        if (!import.meta.env.PROD) {
            throw new Error("상대 API 경로는 production build에서만 사용할 수 있습니다.");
        }
        return "";
    }

    const parsedUrl = new URL(configuredUrl);
    if (!["http:", "https:"].includes(parsedUrl.protocol)
        || parsedUrl.username || parsedUrl.password
        || parsedUrl.search || parsedUrl.hash
        || parsedUrl.pathname !== "/") {
        throw new Error("VITE_API_URL은 경로가 없는 HTTP(S) origin이어야 합니다.");
    }
    return parsedUrl.origin;
}

export const API_URL = resolveApiUrl();

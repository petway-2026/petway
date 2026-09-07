const configuredUrl = import.meta.env.VITE_API_URL?.trim();

if (!configuredUrl) {
    throw new Error("VITE_API_URL 환경변수에 백엔드 주소를 설정해 주세요.");
}

const parsedUrl = new URL(configuredUrl);
if (!["http:", "https:"].includes(parsedUrl.protocol)
    || parsedUrl.username || parsedUrl.password
    || parsedUrl.search || parsedUrl.hash
    || parsedUrl.pathname !== "/") {
    throw new Error("VITE_API_URL은 경로가 없는 HTTP(S) origin이어야 합니다.");
}

export const API_URL = parsedUrl.origin;

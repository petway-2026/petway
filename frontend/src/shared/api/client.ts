/* 상태 변경 요청에 세션 CSRF 토큰을 넣고 명시적인 CSRF 거부에 한해서 한 번 재시도한다. */
import { API_URL } from "./config";

type Csrf = { token: string; headerName: string };
let pendingToken: Promise<Csrf> | undefined;

function csrfToken(): Promise<Csrf> {
    // 동시 상태 변경 요청들이 토큰 발급 Promise를 공유해 중복 발급을 줄인다. 실패하면 다음 요청에서 다시 발급한다.
    if (!pendingToken) {
        const request = fetch(`${API_URL}/api/auth/csrf`, {
            credentials: "include", cache: "no-store",
        }).then(async (response) => {
            if (!response.ok) throw new Error("보안 토큰을 가져오지 못했습니다. 다시 시도해 주세요.");
            const token = await response.json() as Csrf;
            if (!token.token || token.headerName !== "X-CSRF-TOKEN") {
                throw new Error("보안 토큰 응답이 올바르지 않습니다.");
            }
            return token;
        });
        pendingToken = request;
        request.catch(() => { if (pendingToken === request) pendingToken = undefined; });
    }
    return pendingToken;
}

/** Adds the session CSRF token to mutations; retries only a rejected CSRF check. */
export async function apiFetch(url: string, init: RequestInit = {}): Promise<Response> {
    // 세션 토큰을 붙이기 전에 요청 출처를 확인해 다른 서버로 토큰이 전송되지 않게 한다.
    const allowedOrigin = API_URL || window.location.origin;
    const requestUrl = new URL(url, allowedOrigin);
    if (requestUrl.origin !== allowedOrigin) throw new Error("허용되지 않은 API 주소입니다.");
    const method = (init.method ?? "GET").toUpperCase();
    if (["GET", "HEAD", "OPTIONS"].includes(method)) return fetch(url, init);

    const send = async () => {
        const token = await csrfToken();
        init.signal?.throwIfAborted();
        const headers = new Headers(init.headers);
        headers.set(token.headerName, token.token);
        return fetch(url, { ...init, headers, credentials: "include" });
    };
    let response = await send();
    if (response.status === 403) {
        const error = await response.clone().json().catch(() => null) as { code?: string } | null;
        // 일반적인 403을 재전송하지 않는다. 서버가 CSRF 검증 거부로 명시한 요청만 토큰 갱신 후 한 번 재시도한다.
        if (error?.code === "CSRF_INVALID") {
            pendingToken = undefined;
            response = await send();
        }
    }
    // 인증 전환으로 세션의 토큰이 바뀔 수 있으므로 성공 후 기존 토큰 캐시를 비운다.
    if (response.ok && ["/api/auth/login", "/api/auth/logout", "/api/auth/oauth/kakao/link"]
        .includes(requestUrl.pathname)) pendingToken = undefined;
    return response;
}

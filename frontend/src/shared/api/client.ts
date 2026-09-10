import { API_URL } from "./config";

type Csrf = { token: string; headerName: string };
let pendingToken: Promise<Csrf> | undefined;

function csrfToken(): Promise<Csrf> {
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
    const allowedOrigin = API_URL || window.location.origin;
    if (new URL(url, allowedOrigin).origin !== allowedOrigin) throw new Error("허용되지 않은 API 주소입니다.");
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
        if (error?.code === "CSRF_INVALID") {
            pendingToken = undefined;
            response = await send();
        }
    }
    if (response.ok && ["/api/auth/login", "/api/auth/logout", "/api/auth/oauth/kakao/link"]
        .includes(new URL(url).pathname)) pendingToken = undefined;
    return response;
}

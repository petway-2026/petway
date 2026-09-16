/* 회원가입·세션 로그인·계정 연동·로그아웃 API와 카카오 인증 이동을 제공한다. */
import { apiFetch } from "../../../shared/api/client";
import type {
    ApiError,
    LoginRequest,
    LoginResponse,
    SignupRequest,
    SignupResponse,
    User,
} from "../types/auth";

import { API_URL } from "../../../shared/api/config";

export function loginWithKakao(): void {
    window.location.href = `${API_URL}/oauth2/authorization/kakao`;
}

async function parseError(response: Response): Promise<string> {
    try {
        const data: ApiError = await response.json();

        return data.message ?? "요청 처리 중 오류가 발생했습니다.";
    } catch {
        return "요청 처리 중 오류가 발생했습니다.";
    }
}

export async function signup(
    request: SignupRequest
): Promise<SignupResponse> {
    const response = await apiFetch(
        `${API_URL}/api/auth/signup`,
        {
            method: "POST",

            headers: {
                "Content-Type": "application/json",
            },

            credentials: "include",

            body: JSON.stringify(request),
        }
    );

    if (!response.ok) {
        throw new Error(await parseError(response));
    }

    return response.json();
}

export async function login(
    request: LoginRequest
): Promise<LoginResponse> {
    const response = await apiFetch(
        `${API_URL}/api/auth/login`,
        {
            method: "POST",

            headers: {
                "Content-Type": "application/json",
            },

            credentials: "include",

            body: JSON.stringify(request),
        }
    );

    if (!response.ok) {
        throw new Error(await parseError(response));
    }

    return response.json();
}

export async function linkKakaoAccount(
    request: LoginRequest
): Promise<LoginResponse> {
    const response = await apiFetch(
        `${API_URL}/api/auth/oauth/kakao/link`,
        {
            method: "POST",
            headers: {
                "Content-Type": "application/json",
            },
            credentials: "include",
            body: JSON.stringify(request),
        }
    );

    if (!response.ok) {
        throw new Error(await parseError(response));
    }

    return response.json();
}

export async function getMe(): Promise<User> {
    const response = await apiFetch(
        `${API_URL}/api/auth/me`,
        {
            method: "GET",

            credentials: "include",
        }
    );

    if (!response.ok) {
        throw new Error("로그인되어 있지 않습니다.");
    }

    return response.json();
}

export async function logout(): Promise<void> {
    const response = await apiFetch(
        `${API_URL}/api/auth/logout`,
        {
            method: "POST",

            credentials: "include",
        }
    );

    if (!response.ok) {
        throw new Error(await parseError(response));
    }
}

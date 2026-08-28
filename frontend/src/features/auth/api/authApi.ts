import type {
    ApiError,
    LoginRequest,
    LoginResponse,
    SignupRequest,
    SignupResponse,
    User,
} from "../types/auth";

const API_URL =
    import.meta.env.VITE_API_URL ?? "http://localhost:8080";

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
    const response = await fetch(
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
    const response = await fetch(
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

export async function getMe(): Promise<User> {
    const response = await fetch(
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
    const response = await fetch(
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

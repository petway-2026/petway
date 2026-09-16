/* 현재 사용자와 인증 확인 상태를 공유하고 로그인·로그아웃 후 화면 상태를 갱신한다. */
import {
    createContext,
    useContext,
    useEffect,
    useState,
    type ReactNode,
} from "react";

import {
    getMe,
    login as loginApi,
    logout as logoutApi,
} from "../api/authApi";

import type {
    LoginRequest,
    User,
} from "../types/auth";

interface AuthContextType {
    user: User | null;
    loading: boolean;

    login: (
        request: LoginRequest
    ) => Promise<void>;

    logout: () => Promise<void>;

    refreshUser: () => Promise<void>;
}

const AuthContext =
    createContext<AuthContextType | undefined>(
        undefined
    );

interface AuthProviderProps {
    children: ReactNode;
}

export function AuthProvider({
                                 children,
                             }: AuthProviderProps) {
    const [user, setUser] =
        useState<User | null>(null);

    const [loading, setLoading] =
        useState(true);

    const refreshUser = async () => {
        try {
            const currentUser =
                await getMe();

            setUser(currentUser);
        } catch {
            setUser(null);
        }
    };

    useEffect(() => {
        const initializeAuth = async () => {
            await refreshUser();

            setLoading(false);
        };

        initializeAuth();
    }, []);

    const login = async (
        request: LoginRequest
    ) => {
        const response =
            await loginApi(request);

        setUser(response.user);
    };

    const logout = async () => {
        await logoutApi();

        setUser(null);
    };

    return (
        <AuthContext.Provider
            value={{
                user,
                loading,
                login,
                logout,
                refreshUser,
            }}
        >
            {children}
        </AuthContext.Provider>
    );
}

export function useAuth() {
    const context =
        useContext(AuthContext);

    if (!context) {
        throw new Error(
            "useAuth는 AuthProvider 내부에서 사용해야 합니다."
        );
    }

    return context;
}
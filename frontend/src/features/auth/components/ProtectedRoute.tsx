/* 인증 상태 확인이 끝난 뒤 로그인 사용자에게만 하위 화면을 열어 준다. */
import {
    Navigate,
    Outlet,
} from "react-router-dom";

import { useAuth } from "../context/AuthContext";

export default function ProtectedRoute() {
    const {
        user,
        loading,
    } = useAuth();

    if (loading) {
        return (
            <div>
                로그인 정보를 확인하는 중입니다...
            </div>
        );
    }

    if (!user) {
        return (
            <Navigate
                to="/login"
                replace
            />
        );
    }

    return <Outlet />;
}
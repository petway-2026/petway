import {
    Navigate,
    Outlet,
} from "react-router-dom";

import { useAuth } from "./AuthContext";

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
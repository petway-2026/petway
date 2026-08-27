import { Link, NavLink, useNavigate } from "react-router-dom";
import { useAuth } from "../../features/auth/context/AuthContext";

import "../styles/header.css";

export default function Header() {
    const { user, logout } = useAuth();
    const navigate = useNavigate();

    const handleLogout = async () => {
        await logout();
        navigate("/login");
    };

    return (
        <header className="site-header">
            <div className="site-header-inner">
                <Link to="/" className="site-logo">
                    <div className="site-logo-mark">🐾</div>

                    <div className="site-logo-text">
                        <strong>PetWay</strong>
                        <span>Pet Friendly Travel</span>
                    </div>
                </Link>

                <nav className="site-nav">
                    <NavLink to="/">홈</NavLink>
                    <NavLink to="/pets">마이펫</NavLink>

                    <NavLink to="/places">
                        장소찾기
                    </NavLink>

                    <span className="site-nav-disabled">
            여행코스
          </span>
                </nav>

                <div className="site-header-actions">
                    {user ? (
                        <>
                            <Link
                                to="/mypage"
                                className="site-user-button"
                            >
                <span className="site-user-avatar">
                  {user.nickname.charAt(0)}
                </span>

                                <span>{user.nickname}</span>
                            </Link>

                            <button
                                type="button"
                                className="site-logout-button"
                                onClick={handleLogout}
                            >
                                로그아웃
                            </button>
                        </>
                    ) : (
                        <>
                            <Link
                                to="/login"
                                className="header-login-link"
                            >
                                로그인
                            </Link>

                            <Link
                                to="/signup"
                                className="header-signup-button"
                            >
                                시작하기
                            </Link>
                        </>
                    )}
                </div>
            </div>
        </header>
    );
}

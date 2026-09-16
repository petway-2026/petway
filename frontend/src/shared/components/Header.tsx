/* 공통 내비게이션과 로그인 상태별 사용자 메뉴·로그아웃 동작을 표시한다. */
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
                    <div className="site-logo-mark" aria-label="PetWay 로고">
                        <svg viewBox="0 0 48 48" aria-hidden="true">
                            <ellipse cx="24" cy="31" rx="10" ry="8" fill="white" />
                            <circle cx="11" cy="20" r="4.5" fill="white" />
                            <circle cx="20" cy="13" r="4.5" fill="white" />
                            <circle cx="29" cy="13" r="4.5" fill="white" />
                            <circle cx="38" cy="20" r="4.5" fill="white" />
                        </svg>
                    </div>

                    <div className="site-logo-text">
                        <strong>PetWay</strong>
                        <span>Pet Friendly Travel</span>
                    </div>
                </Link>

                <nav className="site-nav">
                    <NavLink to="/">홈</NavLink>
                    <NavLink to="/pets">마이펫</NavLink>

                    <NavLink to="/courses">
                        여행코스
                    </NavLink>

                    <NavLink to="/my-trips">
                        나의 여행일정
                    </NavLink>

                    <NavLink to="/places">
                        장소찾기
                    </NavLink>

                    <NavLink to="/hospitals">
                        동물병원
                    </NavLink>
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

/* 현재 사용자 정보와 반려견·저장 일정 등 개인 기능의 진입점을 표시한다. */
import {
    Link,
    useNavigate,
} from "react-router-dom";

import { useAuth } from "../features/auth/context/AuthContext";

import "./styles/mypage.css";

export default function MyPage() {
    const navigate = useNavigate();

    const {
        user,
        logout,
    } = useAuth();

    const handleLogout =
        async () => {
            try {
                await logout();

                navigate("/login");
            } catch {
                alert(
                    "로그아웃에 실패했습니다."
                );
            }
        };

    if (!user) {
        return null;
    }

    return (
        <div className="mypage-page">
            <div className="mypage-container">
                <section className="mypage-profile-card">
                    <div className="mypage-profile-main">
                        <div className="mypage-avatar">
                            {user.nickname
                                .charAt(0)
                                .toUpperCase()}
                        </div>

                        <div className="mypage-profile-text">
              <span className="mypage-eyebrow">
                MY PAGE
              </span>

                            <h1>
                                {user.nickname}님,
                                안녕하세요
                            </h1>

                            <p>
                                {user.email}
                            </p>
                        </div>
                    </div>

                    <div className="mypage-role">
                        {user.userRole ===
                        "ADMIN"
                            ? "관리자"
                            : "일반 회원"}
                    </div>
                </section>

                <section className="mypage-section">
                    <div className="mypage-section-heading">
                        <h2>
                            내 PetWay
                        </h2>

                        <p>
                            PetWay에서 사용하는 정보를
                            관리할 수 있어요.
                        </p>
                    </div>

                    <div className="mypage-menu-grid">
                        <Link
                            to="/pets"
                            className="mypage-menu-card"
                        >
                            <div className="mypage-menu-icon green">
                                🐾
                            </div>

                            <div>
                                <h3>
                                    마이펫
                                </h3>

                                <p>
                                    반려견 정보와 대표견을
                                    관리해요.
                                </p>
                            </div>

                            <span className="mypage-menu-arrow">
                →
              </span>
                        </Link>

                        <Link to="/my-favorites" className="mypage-menu-card">
                            <div className="mypage-menu-icon orange">
                                ♡
                            </div>

                            <div>
                                <h3>
                                    즐겨찾기
                                </h3>

                                <p>
                                    마음에 든 장소를
                                    모아볼 수 있어요.
                                </p>
                            </div>

                            <span className="mypage-menu-arrow">
                →
              </span>
                        </Link>

                        <Link to="/my-trips" className="mypage-menu-card">
                            <div className="mypage-menu-icon blue">
                                🗺
                            </div>

                            <div>
                                <h3>
                                    나의 여행일정
                                </h3>

                                <p>
                                    저장한 코스를 일정표로
                                    다시 확인해요.
                                </p>
                            </div>

                            <span className="mypage-menu-arrow">→</span>
                        </Link>

                        <div className="mypage-menu-card disabled">
                            <div className="mypage-menu-icon purple">
                                ✓
                            </div>

                            <div>
                                <h3>
                                    방문 기록
                                </h3>

                                <p>
                                    실제 방문했던 장소를
                                    확인해요.
                                </p>
                            </div>

                            <span className="mypage-coming-soon">
                준비 중
              </span>
                        </div>
                    </div>
                </section>

                <section className="mypage-account-card">
                    <div className="mypage-section-heading compact">
                        <h2>
                            계정 정보
                        </h2>
                    </div>

                    <div className="mypage-account-row">
            <span>
              이메일
            </span>

                        <strong>
                            {user.email}
                        </strong>
                    </div>

                    <div className="mypage-account-row">
            <span>
              닉네임
            </span>

                        <strong>
                            {user.nickname}
                        </strong>
                    </div>

                    <div className="mypage-account-row">
            <span>
              회원 유형
            </span>

                        <strong>
                            {user.userRole ===
                            "ADMIN"
                                ? "관리자"
                                : "일반 회원"}
                        </strong>
                    </div>

                    <div className="mypage-account-actions">
                        <button
                            type="button"
                            className="mypage-logout-button"
                            onClick={handleLogout}
                        >
                            로그아웃
                        </button>
                    </div>
                </section>
            </div>
        </div>
    );
}

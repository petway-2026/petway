/* 서비스 시작 화면에서 반려견 상태와 주요 탐색·추천 기능으로 이어지는 진입점을 제공한다. */
import { Link } from "react-router-dom";

import { useAuth } from "../features/auth/context/AuthContext";

import "./styles/home.css";
import { useNavigate } from "react-router-dom";
export default function HomePage() {
    const { user } = useAuth();
    const navigate = useNavigate();
    return (
        <div className="home-page">

            {/* Hero */}
            <section className="home-hero">
                <div className="home-container home-hero-inner">

                    <div className="home-hero-content">
            <span className="home-eyebrow">
              PET FRIENDLY TRAVEL
            </span>

                        <h1>
                            오늘 산책부터
                            <br />
                            주말 여행까지,
                            <br />
                            <strong>우리 아이와 함께.</strong>
                        </h1>

                        <p>
                            반려견의 실제 체중과 동반 조건을 기준으로
                            갈 수 있는 장소를 확인하고,
                            오늘의 외출부터 여행 코스까지 추천받아보세요.
                        </p>

                        <div className="home-hero-actions">
                            {user ? (
                                <>
                                    <Link
                                        to="/pets"
                                        className="home-btn home-btn-primary"
                                    >
                                        내 반려견 확인하기
                                    </Link>

                                    <button
                                        className="home-btn home-btn-secondary"
                                        type="button"
                                        disabled
                                    >
                                        주변 장소 찾기
                                    </button>
                                </>
                            ) : (
                                <>
                                    <Link
                                        to="/signup"
                                        className="home-btn home-btn-primary"
                                    >
                                        PetWay 시작하기
                                    </Link>

                                    <Link
                                        to="/login"
                                        className="home-btn home-btn-secondary"
                                    >
                                        로그인
                                    </Link>
                                </>
                            )}
                        </div>
                    </div>

                    <div className="home-hero-visual">
                        <div className="hero-main-card">
                            <div className="hero-pet-icon">
                                🐶
                            </div>

                            <div>
                <span className="hero-card-label">
                  오늘의 PetWay
                </span>

                                <h2>
                                    우리 아이에게 맞는
                                    <br />
                                    장소를 찾아볼까요?
                                </h2>
                            </div>

                            <div className="hero-rule-list">
                                <div>
                                    <span>✓</span>
                                    실제 체중 기준
                                </div>

                                <div>
                                    <span>✓</span>
                                    장소별 동반 조건 확인
                                </div>

                                <div>
                                    <span>✓</span>
                                    실내·실외 조건 판정
                                </div>
                            </div>
                        </div>

                        <div className="hero-floating-card hero-floating-top">
              <span className="floating-icon">
                🌤
              </span>

                            <div>
                                <strong>
                                    산책하기 좋은 날씨
                                </strong>

                                <p>
                                    오늘 추천 시간 확인
                                </p>
                            </div>
                        </div>

                        <div className="hero-floating-card hero-floating-bottom">
              <span className="floating-icon">
                📍
              </span>

                            <div>
                                <strong>
                                    내 주변 반려동물 장소
                                </strong>

                                <p>
                                    실제 이용 조건까지 확인
                                </p>
                            </div>
                        </div>
                    </div>

                </div>
            </section>


            {/* Quick Menu */}
            <section className="home-section">
                <div className="home-container">

                    <div className="home-section-heading">
                        <div>
              <span className="home-eyebrow">
                QUICK MENU
              </span>

                            <h2>
                                PetWay에서 무엇을 할까요?
                            </h2>
                        </div>
                    </div>

                    <div className="home-quick-grid">

                        <Link
                            to="/pets"
                            className="home-quick-card"
                        >
                            <div className="home-quick-icon green">
                                🐾
                            </div>

                            <h3>
                                마이펫
                            </h3>

                            <p>
                                반려견 정보를 등록하고
                                대표견을 관리해요.
                            </p>

                            <span className="quick-arrow">
                →
              </span>
                        </Link>

                        <Link to="/places" className="home-quick-card">
                            <div className="home-quick-icon blue">
                                📍
                            </div>

                            <h3>
                                내 주변 장소
                            </h3>

                            <p>
                                현재 위치 주변의
                                반려동물 동반 장소를 찾아요.
                            </p>

                            <span className="home-quick-link">
        지도에서 보기 →
    </span>
                        </Link>

                        <Link to="/courses" className="home-quick-card">
                            <div className="home-quick-icon orange">
                                🗺
                            </div>

                            <h3>
                                여행 코스
                            </h3>

                            <p>
                                반려견에게 맞는 장소를
                                연결해 코스를 만들어요.
                            </p>

                            <span className="home-quick-link">코스 만들기 →</span>
                        </Link>

                        <Link to="/courses" className="home-quick-card">
                            <div className="home-quick-icon purple">
                                ✨
                            </div>

                            <h3>
                                AI 추천
                            </h3>

                            <p>
                                검증된 장소 중에서
                                가장 잘 맞는 곳을 추천해요.
                            </p>

                            <span className="home-quick-link">AI 추천받기 →</span>
                        </Link>

                    </div>
                </div>
            </section>


            {/* Core Value */}
            <section className="home-section home-core-section">
                <div className="home-container">

                    <div className="home-core-layout">

                        <div className="home-core-text">
              <span className="home-eyebrow">
                WHY PETWAY
              </span>

                            <h2>
                                단순히
                                <br />
                                “동반 가능”만 보지 않습니다.
                            </h2>

                            <p>
                                장소마다 다른 체중 제한,
                                보호자 수, 이동장, 실내 이용 조건을
                                구조화해서 실제 방문 가능 여부를 판단합니다.
                            </p>
                        </div>

                        <div className="home-compare-card">

                            <div className="compare-block">
                <span className="compare-label">
                  기존 정보
                </span>

                                <strong>
                                    반려동물 동반 가능
                                </strong>
                            </div>

                            <div className="compare-divider">
                                ↓
                            </div>

                            <div className="compare-block highlight">
                <span className="compare-label">
                  PetWay
                </span>

                                <strong>
                                    현재 조건으로 이용 불가
                                </strong>

                                <p>
                                    체중 조건은 충족하지만
                                    보호자 1인당 1견 규정을 초과했어요.
                                </p>
                            </div>

                        </div>
                    </div>

                </div>
            </section>


            {/* Bottom CTA */}
            <section className="home-bottom-cta">
                <div className="home-container home-bottom-inner">

                    <div>
            <span className="home-eyebrow light">
              START PETWAY
            </span>

                        <h2>
                            우리 아이와 갈 수 있는 곳을
                            <br />
                            더 쉽게 찾아보세요.
                        </h2>
                    </div>

                    <Link
                        to={user ? "/pets" : "/signup"}
                        className="home-btn home-btn-light"
                    >
                        {user
                            ? "마이펫 확인하기"
                            : "무료로 시작하기"}
                    </Link>

                </div>
            </section>

        </div>
    );
}

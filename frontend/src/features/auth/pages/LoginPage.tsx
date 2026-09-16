/* 일반·카카오 로그인과 계정 연동 안내를 표시하고 로그인 결과를 인증 상태에 반영한다. */
import {
    useEffect,
    useState,
    type FormEvent,
} from "react";

import {
    Link,
    useNavigate,
    useSearchParams,
} from "react-router-dom";

import {
    linkKakaoAccount,
    loginWithKakao,
} from "../api/authApi";
import { useAuth } from "../context/AuthContext";

import "../styles/auth.css";

export default function LoginPage() {
    const navigate = useNavigate();
    const [searchParams] = useSearchParams();
    const linkRequired =
        searchParams.get("oauthLinkRequired") === "true";

    const {
        login,
        refreshUser,
        user,
    } = useAuth();

    const [email, setEmail] =
        useState("");

    const [password, setPassword] =
        useState("");

    const [error, setError] =
        useState("");

    const [loading, setLoading] =
        useState(false);

    const handleKakaoLogin = () => {
        loginWithKakao();
    };

    useEffect(() => {
        if (user) {
            navigate("/", {
                replace: true,
            });
        }
    }, [user, navigate]);

    const handleSubmit = async (
        event: FormEvent<HTMLFormElement>
    ) => {
        event.preventDefault();

        setError("");

        try {
            setLoading(true);

            const credentials = { email, password };

            if (linkRequired) {
                await linkKakaoAccount(credentials);
                await refreshUser();
            } else {
                await login(credentials);
            }

            navigate("/");
        } catch (error) {
            if (error instanceof Error) {
                setError(error.message);
            } else {
                setError(
                    "로그인에 실패했습니다."
                );
            }
        } finally {
            setLoading(false);
        }
    };

    return (
        <div className="auth-page">
            <div className="auth-background-shape auth-shape-1" />
            <div className="auth-background-shape auth-shape-2" />

            <section className="auth-card">
                <div className="auth-brand">
                    <div className="auth-logo">
                        🐾
                    </div>

                    <div>
                        <h1 className="auth-brand-title">
                            PetWay
                        </h1>

                        <p className="auth-brand-subtitle">
                            오늘 산책부터 주말 여행까지
                        </p>
                    </div>
                </div>

                <div className="auth-heading">
                    <h2>
                        다시 만나서 반가워요
                    </h2>

                    <p>
                        로그인하고 반려견 맞춤 장소를
                        확인해보세요.
                    </p>
                </div>

                <form
                    className="auth-form"
                    onSubmit={handleSubmit}
                >
                    {linkRequired && (
                        <div className="auth-error">
                            같은 이메일의 기존 PetWay 계정이 있습니다.
                            LOCAL 비밀번호를 입력해 카카오 계정 연결을 확인해 주세요.
                        </div>
                    )}
                    <div className="auth-form-group">
                        <label
                            className="auth-label"
                            htmlFor="email"
                        >
                            이메일
                        </label>

                        <input
                            id="email"
                            className="auth-input"
                            type="email"
                            value={email}
                            onChange={(event) =>
                                setEmail(
                                    event.target.value
                                )
                            }
                            placeholder="petway@example.com"
                            autoComplete="email"
                            required
                        />
                    </div>

                    <div className="auth-form-group">
                        <label
                            className="auth-label"
                            htmlFor="password"
                        >
                            비밀번호
                        </label>

                        <input
                            id="password"
                            className="auth-input"
                            type="password"
                            value={password}
                            onChange={(event) =>
                                setPassword(
                                    event.target.value
                                )
                            }
                            placeholder="비밀번호를 입력해주세요"
                            autoComplete="current-password"
                            required
                        />
                    </div>

                    {error && (
                        <div className="auth-error">
                            {error}
                        </div>
                    )}

                    <button
                        type="submit"
                        className="auth-submit-button"
                        disabled={loading}
                    >
                        {loading
                            ? "로그인 중..."
                            : linkRequired
                                ? "계정 연결 후 로그인"
                                : "로그인"}
                    </button>
                </form>

                <div className="auth-divider">
          <span>
            또는
          </span>
                </div>

                <button
                    type="button"
                    className="kakao-button"
                    onClick={handleKakaoLogin}
                >
          <span className="kakao-icon">
            K
          </span>

                    카카오로 시작하기
                </button>

                <p className="auth-footer-text">
                    아직 PetWay 회원이 아니신가요?

                    <Link
                        to="/signup"
                        className="auth-link"
                    >
                        회원가입
                    </Link>
                </p>
            </section>
        </div>
    );
}

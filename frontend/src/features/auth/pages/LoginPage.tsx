import {
    useEffect,
    useState,
    type FormEvent,
} from "react";

import {
    Link,
    useNavigate,
} from "react-router-dom";

import { useAuth } from "../context/AuthContext";

import "../styles/auth.css";

export default function LoginPage() {
    const navigate = useNavigate();

    const {
        login,
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

            await login({
                email,
                password,
            });

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
                    disabled
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
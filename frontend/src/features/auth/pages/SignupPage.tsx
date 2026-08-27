import {
    useState,
    type FormEvent,
} from "react";

import {
    Link,
    useNavigate,
} from "react-router-dom";

import { signup } from "../api/authApi";

import "../styles/auth.css";

export default function SignupPage() {
    const navigate = useNavigate();

    const [email, setEmail] =
        useState("");

    const [nickname, setNickname] =
        useState("");

    const [password, setPassword] =
        useState("");

    const [
        passwordConfirm,
        setPasswordConfirm,
    ] = useState("");

    const [error, setError] =
        useState("");

    const [loading, setLoading] =
        useState(false);

    const handleSubmit = async (
        event: FormEvent<HTMLFormElement>
    ) => {
        event.preventDefault();

        setError("");

        if (
            password !==
            passwordConfirm
        ) {
            setError(
                "비밀번호가 일치하지 않습니다."
            );

            return;
        }

        try {
            setLoading(true);

            await signup({
                email,
                password,
                nickname,
            });

            alert(
                "회원가입이 완료되었습니다."
            );

            navigate("/login");
        } catch (error) {
            if (error instanceof Error) {
                setError(error.message);
            } else {
                setError(
                    "회원가입에 실패했습니다."
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

            <section className="auth-card auth-card-signup">
                <div className="auth-brand">
                    <div className="auth-logo">
                        🐾
                    </div>

                    <div>
                        <h1 className="auth-brand-title">
                            PetWay
                        </h1>

                        <p className="auth-brand-subtitle">
                            우리 아이와 갈 수 있는 곳을 한 번에
                        </p>
                    </div>
                </div>

                <div className="auth-heading">
                    <h2>
                        PetWay 시작하기
                    </h2>

                    <p>
                        간단한 정보만 입력하면 바로
                        시작할 수 있어요.
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
                            htmlFor="nickname"
                        >
                            닉네임
                        </label>

                        <input
                            id="nickname"
                            className="auth-input"
                            type="text"
                            value={nickname}
                            onChange={(event) =>
                                setNickname(
                                    event.target.value
                                )
                            }
                            placeholder="PetWay에서 사용할 닉네임"
                            maxLength={50}
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
                            placeholder="8자 이상 입력해주세요"
                            minLength={8}
                            maxLength={30}
                            autoComplete="new-password"
                            required
                        />
                    </div>

                    <div className="auth-form-group">
                        <label
                            className="auth-label"
                            htmlFor="passwordConfirm"
                        >
                            비밀번호 확인
                        </label>

                        <input
                            id="passwordConfirm"
                            className="auth-input"
                            type="password"
                            value={passwordConfirm}
                            onChange={(event) =>
                                setPasswordConfirm(
                                    event.target.value
                                )
                            }
                            placeholder="비밀번호를 다시 입력해주세요"
                            minLength={8}
                            maxLength={30}
                            autoComplete="new-password"
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
                            ? "가입 중..."
                            : "회원가입"}
                    </button>
                </form>

                <p className="auth-footer-text">
                    이미 회원이신가요?

                    <Link
                        to="/login"
                        className="auth-link"
                    >
                        로그인
                    </Link>
                </p>
            </section>
        </div>
    );
}
import {
    useState,
    type FormEvent,
} from "react";

import {
    Link,
    useNavigate,
} from "react-router-dom";

import { signup } from "../api/authApi";

export default function SignupPage() {
    const navigate =
        useNavigate();

    const [email, setEmail] =
        useState("");

    const [
        password,
        setPassword,
    ] = useState("");

    const [
        passwordConfirm,
        setPasswordConfirm,
    ] = useState("");

    const [
        nickname,
        setNickname,
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
            if (
                error instanceof Error
            ) {
                setError(
                    error.message
                );
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
        <main>
            <h1>PetWay 회원가입</h1>

            <form
                onSubmit={handleSubmit}
            >
                <div>
                    <label htmlFor="email">
                        이메일
                    </label>

                    <input
                        id="email"
                        type="email"
                        value={email}
                        onChange={(event) =>
                            setEmail(
                                event.target.value
                            )
                        }
                        placeholder="petway@example.com"
                        required
                    />
                </div>

                <div>
                    <label htmlFor="nickname">
                        닉네임
                    </label>

                    <input
                        id="nickname"
                        type="text"
                        value={nickname}
                        onChange={(event) =>
                            setNickname(
                                event.target.value
                            )
                        }
                        placeholder="닉네임"
                        required
                    />
                </div>

                <div>
                    <label htmlFor="password">
                        비밀번호
                    </label>

                    <input
                        id="password"
                        type="password"
                        value={password}
                        onChange={(event) =>
                            setPassword(
                                event.target.value
                            )
                        }
                        placeholder="8자 이상"
                        minLength={8}
                        required
                    />
                </div>

                <div>
                    <label htmlFor="passwordConfirm">
                        비밀번호 확인
                    </label>

                    <input
                        id="passwordConfirm"
                        type="password"
                        value={
                            passwordConfirm
                        }
                        onChange={(event) =>
                            setPasswordConfirm(
                                event.target.value
                            )
                        }
                        placeholder="비밀번호 확인"
                        minLength={8}
                        required
                    />
                </div>

                {error && (
                    <p>{error}</p>
                )}

                <button
                    type="submit"
                    disabled={loading}
                >
                    {loading
                        ? "가입 중..."
                        : "회원가입"}
                </button>
            </form>

            <p>
                이미 회원이신가요?{" "}
                <Link to="/login">
                    로그인
                </Link>
            </p>
        </main>
    );
}
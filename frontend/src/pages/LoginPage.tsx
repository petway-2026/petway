import {
    useEffect,
    useState,
    type FormEvent,
} from "react";

import {
    Link,
    useNavigate,
} from "react-router-dom";

import { useAuth } from "../auth/AuthContext";

export default function LoginPage() {
    const navigate =
        useNavigate();

    const {
        login,
        user,
    } = useAuth();

    const [email, setEmail] =
        useState("");

    const [
        password,
        setPassword,
    ] = useState("");

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
    }, [
        user,
        navigate,
    ]);

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
            if (
                error instanceof Error
            ) {
                setError(
                    error.message
                );
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
        <main>
            <h1>PetWay 로그인</h1>

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
                        placeholder="이메일"
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
                        placeholder="비밀번호"
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
                        ? "로그인 중..."
                        : "로그인"}
                </button>
            </form>

            <p>
                아직 회원이 아니신가요?{" "}
                <Link to="/signup">
                    회원가입
                </Link>
            </p>

            <hr />

            <button
                type="button"
                disabled
            >
                카카오 로그인
                (다음 단계)
            </button>
        </main>
    );
}
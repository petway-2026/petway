import {
    Link,
    useNavigate,
} from "react-router-dom";

import { useAuth } from "../auth/AuthContext";

export default function HomePage() {
    const navigate =
        useNavigate();

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

    return (
        <main>
            <h1>🐾 PetWay</h1>

            {user ? (
                <>
                    <p>
                        안녕하세요,{" "}
                        <strong>
                            {user.nickname}
                        </strong>
                        님
                    </p>

                    <p>
                        {user.email}
                    </p>

                    <button
                        type="button"
                        onClick={
                            handleLogout
                        }
                    >
                        로그아웃
                    </button>

                    <br />
                    <br />

                    <Link to="/mypage">
                        마이페이지 테스트
                    </Link>
                </>
            ) : (
                <>
                    <Link to="/login">
                        로그인
                    </Link>

                    {" | "}

                    <Link to="/signup">
                        회원가입
                    </Link>
                </>
            )}
        </main>
    );
}
import { useAuth } from "../auth/AuthContext";

export default function MyPage() {
    const { user } =
        useAuth();

    return (
        <main>
            <h1>마이페이지</h1>

            <p>
                회원번호:
                {user?.userId}
            </p>

            <p>
                이메일:
                {user?.email}
            </p>

            <p>
                닉네임:
                {user?.nickname}
            </p>
        </main>
    );
}
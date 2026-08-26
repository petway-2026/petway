import {
    BrowserRouter,
    Route,
    Routes,
} from "react-router-dom";

import { AuthProvider } from "./auth/AuthContext";

import ProtectedRoute from "./auth/ProtectedRoute";

import HomePage from "./pages/HomePage";
import LoginPage from "./pages/LoginPage";
import SignupPage from "./pages/SignupPage";
import MyPage from "./pages/MyPage";

function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <Routes>

                    {/* 공개 */}
                    <Route
                        path="/"
                        element={<HomePage />}
                    />

                    <Route
                        path="/login"
                        element={<LoginPage />}
                    />

                    <Route
                        path="/signup"
                        element={<SignupPage />}
                    />


                    {/* 로그인 필요 */}
                    <Route
                        element={
                            <ProtectedRoute />
                        }
                    >
                        <Route
                            path="/mypage"
                            element={<MyPage />}
                        />
                    </Route>

                </Routes>
            </AuthProvider>
        </BrowserRouter>
    );
}

export default App;
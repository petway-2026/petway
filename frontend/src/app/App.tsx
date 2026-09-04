import {
    BrowserRouter,
    Route,
    Routes,
    useLocation,
} from "react-router-dom";

import { AuthProvider } from "../features/auth/context/AuthContext";
import ProtectedRoute from "../features/auth/components/ProtectedRoute";
import PlaceMapPage from "../features/place/pages/PlaceMapPage";
import LoginPage from "../features/auth/pages/LoginPage";
import SignupPage from "../features/auth/pages/SignupPage";

import PetListPage from "../features/pet/pages/PetListPage";
import PetCreatePage from "../features/pet/pages/PetCreatePage";
import PetEditPage from "../features/pet/pages/PetEditPage";
import HospitalPage from "../features/hospital/pages/HospitalPage";
import TravelCoursePage from "../features/course/pages/TravelCoursePage";
import SavedTripsPage from "../features/course/pages/SavedTripsPage";
import SavedTripDetailPage from "../features/course/pages/SavedTripDetailPage";

import HomePage from "../pages/HomePage";
import MyPage from "../pages/MyPage";
import FavoritePage from "../pages/FavoritePage";
import Header from "../shared/components/Header";

function AppLayout() {
    const location = useLocation();
    const hideHeader =
        location.pathname === "/login" ||
        location.pathname === "/signup";

    return (
        <>
            {!hideHeader && <Header />}

            <Routes>

                    {/* 공개 페이지 */}
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

                    <Route
                        path="/places"
                        element={<PlaceMapPage />}
                    />


                    {/* 로그인 필요 */}
                    <Route
                        element={<ProtectedRoute />}
                    >
                        <Route
                            path="/hospitals"
                            element={<HospitalPage />}
                        />
                        <Route
                            path="/courses"
                            element={<TravelCoursePage />}
                        />
                        <Route path="/my-trips" element={<SavedTripsPage />} />
                        <Route path="/my-trips/:tripId" element={<SavedTripDetailPage />} />
                        <Route
                            path="/mypage"
                            element={<MyPage />}
                        />
                        <Route path="/my-favorites" element={<FavoritePage />} />

                        <Route
                            path="/pets"
                            element={<PetListPage />}
                        />

                        <Route
                            path="/pets/new"
                            element={<PetCreatePage />}
                        />

                        <Route
                            path="/pets/:petId/edit"
                            element={<PetEditPage />}
                        />
                    </Route>

            </Routes>
        </>
    );
}

function App() {
    return (
        <BrowserRouter>
            <AuthProvider>
                <AppLayout />
            </AuthProvider>
        </BrowserRouter>
    );
}

export default App;

/* 사용자의 저장 여행 목록을 불러오고 상세 이동과 일정 삭제를 제공한다. */
import { useEffect, useState } from "react";
import { Link } from "react-router-dom";
import { deleteSavedTrip, getSavedTrips, type SavedTrip } from "../api/savedTripApi";
import "../styles/saved-trips.css";

export default function SavedTripsPage() {
    const [trips, setTrips] = useState<SavedTrip[]>([]);
    const [loading, setLoading] = useState(true);
    const [message, setMessage] = useState<string | null>(null);

    useEffect(() => { getSavedTrips().then(setTrips).catch((e: Error) => setMessage(e.message)).finally(() => setLoading(false)); }, []);

    const remove = async (trip: SavedTrip) => {
        if (!window.confirm(`「${trip.title}」 일정을 삭제할까요?`)) return;
        await deleteSavedTrip(trip.id);
        setTrips((current) => current.filter((item) => item.id !== trip.id));
    };

    return <main className="saved-trips-page"><div className="saved-trips-inner">
        <header className="saved-trips-hero"><div><span>MY JOURNEYS</span><h1>나의 여행일정</h1><p>반려견과 함께할 여행을 날짜별 일정표로 다시 확인하세요.</p></div><Link to="/courses">새 코스 만들기</Link></header>
        {loading ? <div className="saved-trips-empty">저장한 일정을 불러오고 있어요…</div>
            : message ? <div className="saved-trips-empty">{message}</div>
                : trips.length === 0 ? <div className="saved-trips-empty"><strong>아직 저장한 여행이 없어요</strong><span>여행 코스를 만든 후 ‘나의 여행일정에 저장’을 눌러보세요.</span><Link to="/courses">첫 일정 만들기</Link></div>
                    : <section className="saved-trip-grid">{trips.map((trip) => {
                        const cover = trip.itinerary.days.flatMap((day) => day.places).find((place) => place.thumbnailUrl)?.thumbnailUrl;
                        return <article className="saved-trip-card" key={trip.id}>
                            <Link to={`/my-trips/${trip.id}`} className="saved-trip-card-main"><div className="saved-trip-cover">{cover ? <img src={cover} alt="" /> : <span>🐾</span>}<em>{trip.transportMode === "TRANSIT" ? "🚌 대중교통" : "🚗 자동차"}</em></div><div className="saved-trip-copy"><span>{trip.regionName}</span><h2>{trip.title}</h2><p>{trip.startDate.replaceAll("-", ".")} — {trip.endDate.replaceAll("-", ".")}</p><div><strong>{trip.itinerary.days.length}일</strong><strong>{trip.placeCount}곳</strong></div></div></Link>
                            <button type="button" onClick={() => void remove(trip)}>삭제</button>
                        </article>;
                    })}</section>}
    </div></main>;
}

/* 저장된 일정의 날짜별 장소 순서와 준비사항을 표시한다. 저장 시점의 스냅샷을 사용한다. */
import { useEffect, useState } from "react";
import { Link, useParams } from "react-router-dom";
import { getSavedTrip, type SavedTrip } from "../api/savedTripApi";
import "../styles/saved-trips.css";

const TYPE_LABELS: Record<string, string> = { "12": "관광지", "14": "문화시설", "15": "축제·행사", "28": "레포츠", "32": "숙박", "38": "쇼핑", "39": "음식점" };

export default function SavedTripDetailPage() {
    const { tripId } = useParams();
    const [trip, setTrip] = useState<SavedTrip | null>(null);
    const [message, setMessage] = useState("일정표를 준비하고 있어요…");
    useEffect(() => { const id = Number(tripId); if (!Number.isFinite(id)) return; getSavedTrip(id).then(setTrip).catch((e: Error) => setMessage(e.message)); }, [tripId]);
    if (!trip) return <main className="saved-trips-page"><div className="saved-trips-empty">{message}</div></main>;

    return <main className="saved-trip-detail"><div className="saved-trip-detail-inner">
        <Link className="saved-trip-back" to="/my-trips">← 나의 여행일정</Link>
        <header className="saved-trip-detail-hero"><span>{trip.regionName} · {trip.transportMode === "TRANSIT" ? "대중교통 여행" : "자동차 여행"}</span><h1>{trip.title}</h1><p>{trip.startDate.replaceAll("-", ".")} — {trip.endDate.replaceAll("-", ".")} · {trip.itinerary.days.length}일 · 총 {trip.placeCount}곳</p></header>
        <div className="saved-trip-days">{trip.itinerary.days.map((day, dayIndex) => <section className="saved-trip-day" key={day.date}>
            <header><div><span>DAY {dayIndex + 1}</span><h2>{day.date.replaceAll("-", ".")}</h2></div><strong>{day.places.length}곳</strong></header>
            {day.places.length === 0 ? <p className="saved-trip-no-place">정해진 장소가 없는 여유로운 날이에요.</p> : <ol>{day.places.map((place, index) => <li key={`${place.contentId}-${index}`}><div className="saved-trip-time">{String(10 + Math.min(index * 2, 10)).padStart(2, "0")}:00</div><div className="saved-trip-line"><i>{index + 1}</i></div><article>{place.thumbnailUrl && <img src={place.thumbnailUrl} alt="" />}<div><span>{TYPE_LABELS[place.contentTypeId] ?? "동반 장소"}</span><h3>{place.title}</h3><p>{place.address ?? "주소 정보 없음"}</p>{place.preparations && place.preparations.length > 0 && <small className="saved-trip-preparations">준비사항 · {place.preparations.join(", ")}</small>}</div></article></li>)}</ol>}
        </section>)}</div>
        <footer className="saved-trip-detail-footer"><span>저장한 날짜 {new Date(trip.createdAt).toLocaleDateString("ko-KR")}</span><Link to="/courses">새 여행 코스 만들기</Link></footer>
    </div></main>;
}

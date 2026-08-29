import NearbyPlacesSection from "../components/NearbyPlacesSection";

export default function PlaceMapPage() {
    return (
        <main className="place-map-page">
            <NearbyPlacesSection showFullMapButton={false} />
        </main>
    );
}

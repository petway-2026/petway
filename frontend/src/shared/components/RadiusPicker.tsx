import "../styles/radius-picker.css";

export const RADIUS_OPTIONS = [
    { km: 1, meters: 1_000, level: 4 },
    { km: 5, meters: 5_000, level: 6 },
    { km: 10, meters: 10_000, level: 7 },
    { km: 20, meters: 20_000, level: 8 },
    { km: 30, meters: 30_000, level: 9 },
] as const;

export type RadiusOption = typeof RADIUS_OPTIONS[number];

export default function RadiusPicker({ open, onSelect, onClose }: {
    open: boolean;
    onSelect: (option: RadiusOption) => void;
    onClose: () => void;
}) {
    if (!open) return null;
    return (
        <div className="radius-picker-backdrop" role="presentation" onMouseDown={onClose}>
            <section className="radius-picker" role="dialog" aria-modal="true" aria-labelledby="radius-picker-title" onMouseDown={(event) => event.stopPropagation()}>
                <button type="button" className="radius-picker-close" onClick={onClose} aria-label="닫기">×</button>
                <span>내 위치 주변 검색</span>
                <h2 id="radius-picker-title">얼마나 멀리까지 찾아볼까요?</h2>
                <p>선택한 반경에 맞춰 지도 배율과 검색 결과를 설정합니다.</p>
                <div className="radius-picker-options">
                    {RADIUS_OPTIONS.map((option) => (
                        <button key={option.km} type="button" onClick={() => onSelect(option)}>{option.km}km 이내</button>
                    ))}
                </div>
            </section>
        </div>
    );
}

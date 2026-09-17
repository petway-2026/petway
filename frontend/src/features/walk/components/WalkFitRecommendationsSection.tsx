/* 위치를 받아 산책 후보를 조회하고 적합도·동반 준비사항·날씨를 표시한다. 점수 계산은 백엔드가 담당한다. */
import { Fragment, useEffect, useMemo, useRef, useState } from "react";

import {
  getWalkFitRecommendations,
  WalkFitApiError,
} from "../api/walkFitApi";
import type {
  WalkFitGrade,
  WalkFitRecommendationResponse,
} from "../types/walkFit";

import "../styles/walk-fit.css";

const GRADE_COPY: Record<WalkFitGrade, { label: string; className: string }> = {
  VERY_GOOD: { label: "오늘 산책하기 좋아요", className: "very-good" },
  GOOD: { label: "산책 후보로 추천해요", className: "good" },
  CAUTION: { label: "주의사항을 확인해 주세요", className: "caution" },
  LOW: { label: "다른 후보도 함께 살펴보세요", className: "low" },
};

const COMPATIBILITY_COPY = {
  AVAILABLE: "동반 조건 충족",
  CONDITIONAL: "준비사항 확인",
  NEED_CONFIRMATION: "방문 전 확인 필요",
  UNAVAILABLE: "동반 어려움",
} as const;

function formatLocalDate(date: Date): string {
  const year = date.getFullYear();
  const month = String(date.getMonth() + 1).padStart(2, "0");
  const day = String(date.getDate()).padStart(2, "0");
  return `${year}-${month}-${day}`;
}

type Coordinates = { latitude: number; longitude: number };

function formatDistance(origin: Coordinates | null, latitude: number | null, longitude: number | null): string {
  if (!origin || latitude == null || longitude == null
    || !Number.isFinite(latitude) || !Number.isFinite(longitude)
    || Math.abs(latitude) > 90 || Math.abs(longitude) > 180) {
    return "거리 정보 없음";
  }
  const radians = Math.PI / 180;
  const latitudeDelta = (latitude - origin.latitude) * radians;
  const longitudeDelta = (longitude - origin.longitude) * radians;
  const a = Math.sin(latitudeDelta / 2) ** 2
    + Math.cos(origin.latitude * radians) * Math.cos(latitude * radians)
    * Math.sin(longitudeDelta / 2) ** 2;
  const distanceKm = 6371 * 2 * Math.asin(Math.sqrt(Math.min(1, Math.max(0, a))));
  return distanceKm < 0.1
    ? "현재 위치에서 0.1 km 미만 · 직선거리"
    : `현재 위치에서 약 ${distanceKm.toFixed(1)} km · 직선거리`;
}

function requestErrorMessage(cause: unknown): string {
  if (cause instanceof WalkFitApiError) {
    if (cause.status === 401 || cause.status === 403) {
      return "로그인 정보가 만료되었습니다. 다시 로그인한 뒤 이용해 주세요.";
    }
    if (cause.code === "WALK_FIT_PROVIDER_RATE_LIMITED") {
      return "주변 장소의 반려견 동반 정보를 확인하지 못했어요. 잠시 후 다시 시도해 주세요.";
    }
    return cause.message;
  }
  return "네트워크 연결을 확인한 뒤 다시 시도해 주세요.";
}

export default function WalkFitRecommendationsSection() {
  const dateRange = useMemo(() => {
    const today = new Date();
    const maximum = new Date(today);
    maximum.setDate(maximum.getDate() + 90);
    return {
      minimum: formatLocalDate(today),
      maximum: formatLocalDate(maximum),
    };
  }, []);
  const [targetDate, setTargetDate] = useState(dateRange.minimum);
  const [result, setResult] =
    useState<WalkFitRecommendationResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [searchOrigin, setSearchOrigin] = useState<Coordinates | null>(null);
  const [error, setError] = useState("");
  const [expandedRecommendationId, setExpandedRecommendationId] = useState<string | null>(null);
  const requestRef = useRef<AbortController | null>(null);

  useEffect(() => () => requestRef.current?.abort(), []);

  // 버튼을 눌렀을 때 위치를 얻어 백엔드의 규칙 기반 추천을 요청한다. 이전 요청은 취소해 결과 경합을 줄인다.
  const requestRecommendations = () => {
    if (!navigator.geolocation) {
      setError("이 브라우저에서는 현재 위치를 사용할 수 없습니다.");
      return;
    }

    requestRef.current?.abort();
    const controller = new AbortController();
    requestRef.current = controller;
    setLoading(true);
    setResult(null);
    setExpandedRecommendationId(null);
    setError("");

    navigator.geolocation.getCurrentPosition(
      (position) => {
        getWalkFitRecommendations(
          position.coords.latitude,
          position.coords.longitude,
          targetDate,
          controller.signal,
        )
          .then((response) => {
            if (!controller.signal.aborted) {
              setSearchOrigin({ latitude: position.coords.latitude, longitude: position.coords.longitude });
              setResult(response);
            }
          })
          .catch((cause: unknown) => {
            if (!controller.signal.aborted) setError(requestErrorMessage(cause));
          })
          .finally(() => {
            if (!controller.signal.aborted) {
              requestRef.current = null;
              setLoading(false);
            }
          });
      },
      (locationError) => {
        if (controller.signal.aborted) return;
        requestRef.current = null;
        setLoading(false);
        setError(
          locationError.code === locationError.PERMISSION_DENIED
            ? "현재 위치 권한을 허용하면 산책 후보를 추천받을 수 있어요."
            : "현재 위치를 확인하지 못했습니다. 잠시 후 다시 시도해 주세요.",
        );
      },
      { enableHighAccuracy: true, timeout: 10000, maximumAge: 60000 },
    );
  };

  return (
    <section className="walk-fit-section" aria-labelledby="walk-fit-title">
      <div className="walk-fit-heading">
        <div>
          <span className="walk-fit-eyebrow">WALKFIT</span>
          <h2 id="walk-fit-title">오늘의 산책 추천</h2>
          <p>
            현재 위치에서 5km 이내의 관광지 중 대표 반려견의 동반 조건과
            선택한 날짜의 날씨를 함께 평가해 추천해요.
          </p>
        </div>
        <div className="walk-fit-controls">
          <label htmlFor="walk-fit-date">산책 날짜</label>
          <input
            id="walk-fit-date"
            type="date"
            min={dateRange.minimum}
            max={dateRange.maximum}
            value={targetDate}
            disabled={loading}
            onChange={(event) => {
              requestRef.current?.abort();
              requestRef.current = null;
              setLoading(false);
              setResult(null);
              setExpandedRecommendationId(null);
              setError("");
              setTargetDate(event.target.value);
            }}
          />
          <button
            type="button"
            onClick={requestRecommendations}
            disabled={loading || !targetDate}
          >
            {loading ? "산책 후보 평가 중…" : "현재 위치로 추천받기"}
          </button>
        </div>
      </div>

      <p className="walk-fit-boundary">
        산책 적합도는 방문 후보 평가이며 실제 산책로나 보행 경로의 존재를
        보장하지 않습니다.
      </p>

      {error && (
        <div className="walk-fit-error" role="alert">
          <span>{error}</span>
          <button type="button" onClick={requestRecommendations}>
            다시 시도
          </button>
        </div>
      )}

      {result && (
        <div className="walk-fit-results" aria-live="polite">
          <p className="walk-fit-message">{result.message}</p>

          {result.recommendations.map((recommendation, index) => {
            const grade = GRADE_COPY[recommendation.walkFit.grade];
            return (
              <Fragment key={recommendation.contentId}>
                {index === 0 && (
                  <div className="walk-fit-group-heading">
                    <h3>오늘의 추천 TOP 3</h3>
                    <span>가장 잘 맞는 순서예요</span>
                  </div>
                )}
                {index === 3 && (
                  <div className="walk-fit-group-heading secondary">
                    <h3>추가로 둘러볼 만한 장소</h3>
                    <span>이미 평가를 마친 적합 후보예요</span>
                  </div>
                )}
              <article
                className={`walk-fit-card ${grade.className}`}
              >
                <div className="walk-fit-card-body">
                  <div className="walk-fit-card-top">
                    <span className="walk-fit-rank">{recommendation.rank}순위</span>
                    <span className="walk-fit-grade">{grade.label}</span>
                  </div>
                  <h3>{recommendation.title}</h3>
                  {recommendation.address && <p>{recommendation.address}</p>}
                  <small className="walk-fit-distance">
                    {formatDistance(searchOrigin, recommendation.latitude, recommendation.longitude)}
                  </small>
                  <strong className="walk-fit-pet-copy">
                    {result.pet.petName}와 산책 후보로 추천해요
                  </strong>
                  <div className="walk-fit-score">
                    <span>산책 적합도</span>
                    <strong>{recommendation.walkFit.score}점</strong>
                  </div>

                  <div className="walk-fit-status-row">
                    <span className={`walk-fit-compatibility ${recommendation.compatibility.status.toLowerCase()}`}>
                      {COMPATIBILITY_COPY[recommendation.compatibility.status]}
                    </span>
                    {recommendation.compatibility.needsConfirmation && (
                      <strong>방문 전 확인이 필요해요</strong>
                    )}
                  </div>

                  <button
                    type="button"
                    className="walk-fit-toggle"
                    aria-expanded={expandedRecommendationId === recommendation.contentId}
                    onClick={() => setExpandedRecommendationId((current) => current === recommendation.contentId ? null : recommendation.contentId)}
                  >
                    {expandedRecommendationId === recommendation.contentId ? "안내사항 접기" : "동반 안내사항 보기"}
                    <span aria-hidden="true">{expandedRecommendationId === recommendation.contentId ? "⌃" : "⌄"}</span>
                  </button>

                  {expandedRecommendationId === recommendation.contentId && <>
                    <section className="walk-fit-detail-group">
                      <h4>추천 이유</h4>
                      <ul>{recommendation.walkFit.reasons.map((reason) => <li key={reason}>{reason}</li>)}</ul>
                    </section>

                    {recommendation.compatibility.requirements.length > 0 && (
                      <section className="walk-fit-detail-group">
                        <h4>준비해주세요</h4>
                        <ul>{recommendation.compatibility.requirements.map((requirement, requirementIndex) => <li key={`${requirement.type}-${requirement.scope}-${requirementIndex}`}>{requirement.scope === "INDOOR" ? "실내 이용 시 " : requirement.scope === "OUTDOOR" ? "실외 이용 시 " : ""}{requirement.label}</li>)}</ul>
                      </section>
                    )}

                    <section className="walk-fit-weather">
                      <h4>날씨</h4>
                      {recommendation.weather.available ? <><>{recommendation.weather.summary && <p>{recommendation.weather.summary}</p>}</><div>{recommendation.weather.temperature && <span>{recommendation.weather.temperature}</span>}{recommendation.weather.precipitation && <span>{recommendation.weather.precipitation}</span>}</div></> : <p>날씨 정보를 확인하지 못해 날씨는 평가에서 제외했어요.</p>}
                    </section>
                  </>}
                </div>
              </article>
              </Fragment>
            );
          })}
        </div>
      )}
    </section>
  );
}

import { useEffect, useId, useMemo, useState } from "react";
import { createPortal } from "react-dom";

import { fetchReviews, saveReview, type Review } from "../api/reviewApi";
import "../styles/review.css";

interface ReviewButtonProps {
    targetType: string;
    targetId: string;
    title: string;
}

export default function ReviewButton({ targetType, targetId, title }: ReviewButtonProps) {
    const [items, setItems] = useState<Review[]>([]);
    const [open, setOpen] = useState(false);
    const [rating, setRating] = useState(5);
    const [content, setContent] = useState("");
    const [busy, setBusy] = useState(false);
    const [error, setError] = useState("");
    const titleId = useId();

    const load = () => fetchReviews(targetType, targetId).then(setItems).catch(() => undefined);

    useEffect(() => {
        void load();
    }, [targetType, targetId]);

    useEffect(() => {
        if (!open) return;
        const handleKeyDown = (event: KeyboardEvent) => {
            if (event.key === "Escape") setOpen(false);
        };
        document.addEventListener("keydown", handleKeyDown);
        return () => document.removeEventListener("keydown", handleKeyDown);
    }, [open]);

    const average = useMemo(
        () => items.length
            ? items.reduce((sum, item) => sum + item.rating, 0) / items.length
            : null,
        [items]
    );

    const submit = async (event: React.FormEvent) => {
        event.preventDefault();
        const value = content.trim();
        if (!value) return;

        setBusy(true);
        setError("");
        try {
            await saveReview(targetType, targetId, rating, value);
            setContent("");
            await load();
            setOpen(false);
        } catch (cause) {
            setError(cause instanceof Error ? cause.message : "리뷰 저장에 실패했습니다.");
        } finally {
            setBusy(false);
        }
    };

    const modal = open ? createPortal(
        <div
            className="review-backdrop"
            onMouseDown={(event) => {
                if (event.target === event.currentTarget) setOpen(false);
            }}
        >
            <section className="review-modal" role="dialog" aria-modal="true" aria-labelledby={titleId}>
                <button type="button" className="review-close" onClick={() => setOpen(false)} aria-label="닫기">
                    ×
                </button>
                <h2 id={titleId}>{title} 리뷰</h2>
                <form onSubmit={submit}>
                    <fieldset>
                        <legend>별점</legend>
                        <div className="review-stars">
                            {[1, 2, 3, 4, 5].map((value) => (
                                <button
                                    type="button"
                                    key={value}
                                    className={value <= rating ? "active" : ""}
                                    onClick={() => setRating(value)}
                                    aria-label={`${value}점`}
                                >
                                    ★
                                </button>
                            ))}
                        </div>
                    </fieldset>
                    <label>
                        한줄 리뷰
                        <textarea
                            value={content}
                            onChange={(event) => setContent(event.target.value.slice(0, 100))}
                            maxLength={100}
                            rows={3}
                            placeholder="방문 경험을 100자 이내로 알려주세요."
                            required
                        />
                        <small>{content.length} / 100</small>
                    </label>
                    {error && <p className="review-error">{error}</p>}
                    <button className="review-submit" type="submit" disabled={busy || !content.trim()}>
                        {busy ? "저장 중…" : "리뷰 저장"}
                    </button>
                </form>
                {items.length > 0 && (
                    <div className="review-list">
                        {items.slice(0, 5).map((item) => (
                            <article key={item.id}>
                                <strong>
                                    {"★".repeat(item.rating)}
                                    <span>{"☆".repeat(5 - item.rating)}</span>
                                </strong>
                                <p>{item.content}</p>
                                <small>{item.nickname}</small>
                            </article>
                        ))}
                    </div>
                )}
            </section>
        </div>,
        document.body
    ) : null;

    return (
        <>
            <button
                type="button"
                className="review-summary-button"
                onClick={(event) => {
                    event.stopPropagation();
                    setOpen(true);
                }}
            >
                {average === null ? "☆ 리뷰" : `★ ${average.toFixed(1)} (${items.length})`}
            </button>
            {modal}
        </>
    );
}

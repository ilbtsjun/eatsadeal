import { useState } from 'react';
import Header from '@/common/components/Header';
import './EventDetailPage.css';
import { usePageTitle } from '@/common/hooks/usePageTitle.js';
import { getFallbackImage } from '@/common/utils/BrandUtil.js';
import { calculateDDay, formatDate } from '@/common/utils/DateUtil.js';
import CommentSection from '@/features/comments/components/CommentSection.jsx';
import { getEventCodeName } from '@/features/events/utils/eventDisplay.js';
import EventEditModal from '@/features/events/components/EventEditModal.jsx';
import { useEventDetail } from '@/features/events/hooks/useEventDetail.js';
import NotFoundPage from '@/pages/NotFoundPage.jsx';

export default function EventDetailPage({ event, user, onLoginClick, onBack }) {
    const {
        detailEvent,
        loaded,
        loadError,
        isFavorite,
        pendingId,
        toggleError,
        handleToggleFavorite,
        reload,
    } = useEventDetail({ event, user, onLoginClick });

    const [editing, setEditing] = useState(false);
    const isAdmin = user?.role === 'ADMIN';

    usePageTitle(detailEvent?.title || '이벤트');

    const renderStatus = (content) => (
        <div className="event-detail-page">
            <Header />
            <main className="event-detail-container">
                <button
                    type="button"
                    className="detail-back-button"
                    onClick={onBack}
                >
                    ← 할인정보 목록으로
                </button>
                <section className="detail-status" role="status">
                    {content}
                </section>
            </main>
        </div>
    );

    if (!loaded) {
        return renderStatus(
            <>
                <span className="detail-spinner" aria-hidden="true" />
                <p>이벤트를 불러오는 중입니다…</p>
            </>
        );
    }

    if (loadError?.status === 404) {
        return <NotFoundPage />;
    }

    if (loadError && !detailEvent?.title) {
        return renderStatus(
            <>
            <span className="detail-status-emoji" aria-hidden="true">
                😵
            </span>
                <h1>이벤트를 불러오지 못했어요</h1>
                <p>{loadError.message}</p>
                <button
                    type="button"
                    className="detail-status-button"
                    onClick={() => window.location.reload()}
                >
                    다시 시도
                </button>
            </>
        );
    }

    return (
        <div className="event-detail-page">
            <Header />

            <main className="event-detail-container">
                <button
                    type="button"
                    className="detail-back-button"
                    onClick={onBack}
                >
                    ← 뒤로 가기
                </button>

                <article className="event-detail-card">
                    <div className="detail-image-box">
                        {detailEvent?.img ? (
                            <img
                                src={detailEvent.img}
                                alt={detailEvent.title}
                                className="detail-image"
                                onError={(e) => {
                                    e.currentTarget.onerror = null;
                                    e.currentTarget.src = getFallbackImage(
                                        detailEvent.brand
                                    );
                                }}
                            />
                        ) : (
                            <span className="detail-emoji">🍗</span>
                        )}

                        <span className="detail-dday">
                        {calculateDDay(detailEvent?.endDate)}
                    </span>

                        <button
                            type="button"
                            className={`detail-favorite ${isFavorite ? 'is-favorite' : ''}`}
                            aria-label={isFavorite ? '찜 취소' : '찜하기'}
                            aria-pressed={isFavorite}
                            disabled={pendingId === event.id}
                            onClick={handleToggleFavorite}
                        >
                            <svg
                                className="favorite-icon"
                                viewBox="0 0 24 24"
                                aria-hidden="true"
                            >
                                <path d="M20.8 8.8c0 5.1-8.8 10.1-8.8 10.1S3.2 13.9 3.2 8.8A4.8 4.8 0 0 1 12 6.1a4.8 4.8 0 0 1 8.8 2.7Z" />
                            </svg>
                            찜하기
                        </button>
                    </div>

                    <div className="detail-content">
                    <span className="detail-brand">
                        {detailEvent?.brand || '이츠어딜'}
                    </span>

                        <h1>{detailEvent?.title}</h1>

                        {isAdmin && (
                            <div className="detail-admin-bar">
                                <button
                                    type="button"
                                    className="detail-edit-button"
                                    onClick={() => setEditing(true)}
                                >
                                    이벤트 수정
                                </button>
                                {detailEvent?.hidden && (
                                    <span className="detail-admin-badge">숨김</span>
                                )}
                                {detailEvent?.adminLocked && (
                                    <span className="detail-admin-badge">크롤러 잠금</span>
                                )}
                            </div>
                        )}

                        <div className="detail-tags">
                        <span>
                            {getEventCodeName(detailEvent?.eventCodes)}
                        </span>
                            <span>
                            {formatDate(detailEvent?.startDate)} ~{' '}
                                {formatDate(detailEvent?.endDate)}
                        </span>
                            <span>
                            조회수{' '}
                                {Number(
                                    detailEvent?.viewCount || 0
                                ).toLocaleString()}
                        </span>
                        </div>

                        {toggleError && (
                            <p className="favorite-error" role="alert">
                                {toggleError}
                            </p>
                        )}

                        <p className="detail-description">
                            {detailEvent?.description ||
                                '이벤트에 대한 자세한 할인 내용을 확인해보세요.'}
                        </p>

                        {detailEvent?.url && (
                            <a
                                className="original-event-link"
                                href={detailEvent.url}
                                target="_blank"
                                rel="noreferrer"
                            >
                                원문 이벤트 보러가기 ↗
                            </a>
                        )}
                    </div>
                </article>

                {editing && (
                    <EventEditModal
                        event={detailEvent}
                        onClose={() => setEditing(false)}
                        onSaved={() => {
                            setEditing(false);
                            reload();
                        }}
                    />
                )}

                <CommentSection
                    eventId={event.id}
                    user={user}
                    onLoginClick={onLoginClick}
                />
            </main>
        </div>
    );
}

import { useState } from 'react';
import Header from '@/common/components/Header';
import './EventDetailPage.css';
import CommentSection from '@/features/comments/components/CommentSection.jsx';
import EventEditModal from '@/features/events/components/EventEditModal.jsx';
import EventDetailImage from '@/features/events/components/EventDetailImage.jsx';
import EventDetailInfo from '@/features/events/components/EventDetailInfo.jsx';
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
        isSubscribed,
        subscribePendingId,
        subscribeError,
        handleToggleSubscribe,
        shareMessage,
        handleShare,
    } = useEventDetail({ event, user, onLoginClick });

    const [editing, setEditing] = useState(false);
    const isAdmin = user?.role === 'ADMIN';

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
                    <EventDetailImage
                        detailEvent={detailEvent}
                        isFavorite={isFavorite}
                        favoriteDisabled={pendingId === event.id}
                        onToggleFavorite={handleToggleFavorite}
                    />
                    <EventDetailInfo
                        detailEvent={detailEvent}
                        isAdmin={isAdmin}
                        onEdit={() => setEditing(true)}
                        isSubscribed={isSubscribed}
                        subscribeDisabled={subscribePendingId != null && subscribePendingId === detailEvent?.brandId}
                        onToggleSubscribe={handleToggleSubscribe}
                        toggleError={toggleError}
                        subscribeError={subscribeError}
                        shareMessage={shareMessage}
                        onShare={handleShare}
                    />
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

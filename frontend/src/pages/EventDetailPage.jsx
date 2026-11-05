import { useState } from 'react';
import Header from '@/common/components/Header';
import StatusMessage from '@/common/components/StatusMessage.jsx';
import './EventDetailPage.css';
import CommentSection from '@/features/comments/components/CommentSection.jsx';
import EventEditModal from '@/features/events/components/EventEditModal.jsx';
import EventDetailImage from '@/features/events/components/EventDetailImage.jsx';
import EventDetailInfo from '@/features/events/components/EventDetailInfo.jsx';
import { useEventDetail } from '@/features/events/hooks/useEventDetail.js';
import ConsentModal from '@/features/notifications/components/ConsentModal.jsx';
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
        consentPrompt,
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
                {content}
            </main>
        </div>
    );

    if (!loaded) {
        return renderStatus(
            <StatusMessage type="loading" message="이벤트를 불러오는 중입니다…" />
        );
    }

    if (loadError?.status === 404) {
        return <NotFoundPage />;
    }

    if (loadError && !detailEvent?.title) {
        return renderStatus(
            <StatusMessage
                type="error"
                title="이벤트를 불러오지 못했어요"
                message={loadError.message}
                onRetry={reload}
            />
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

                {consentPrompt.isOpen && (
                    <ConsentModal
                        draft={consentPrompt.draft}
                        saving={consentPrompt.saving}
                        error={consentPrompt.error}
                        canSave={consentPrompt.canSave}
                        onToggle={consentPrompt.toggleDraft}
                        onSave={consentPrompt.save}
                        onDismiss={consentPrompt.dismiss}
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

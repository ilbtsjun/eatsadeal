import { formatDate } from '@/common/utils/DateUtil.js';
import { getEventCodeName } from '@/features/events/utils/eventDisplay.js';

export default function EventDetailInfo({
    detailEvent,
    isAdmin,
    onEdit,
    isSubscribed,
    subscribeDisabled,
    onToggleSubscribe,
    toggleError,
    subscribeError,
    shareMessage,
    onShare,
}) {
    return (
        <div className="detail-content">
            <div className="detail-brand-row">
                <span className="detail-brand">
                    {detailEvent?.brand || '이츠어딜'}
                </span>
                <button
                    type="button"
                    className={`detail-subscribe ${isSubscribed ? 'is-subscribed' : ''}`}
                    aria-pressed={isSubscribed}
                    disabled={subscribeDisabled}
                    onClick={onToggleSubscribe}
                >
                    {isSubscribed ? '구독 중' : '브랜드 구독'}
                </button>
            </div>

            <h1>{detailEvent?.title}</h1>

            {isAdmin && (
                <div className="detail-admin-bar">
                    <button
                        type="button"
                        className="detail-edit-button"
                        onClick={onEdit}
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
                    {Number(detailEvent?.viewCount || 0).toLocaleString()}
                </span>
            </div>

            {toggleError && (
                <p className="favorite-error" role="alert">
                    {toggleError}
                </p>
            )}
            {subscribeError && (
                <p className="favorite-error" role="alert">
                    {subscribeError}
                </p>
            )}

            <p className="detail-description">
                {detailEvent?.description ||
                    '이벤트에 대한 자세한 할인 내용을 확인해보세요.'}
            </p>

            <div className="detail-actions">
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
                <button
                    type="button"
                    className="detail-share-button"
                    onClick={onShare}
                >
                    공유하기
                </button>
                {shareMessage && (
                    <span className="detail-share-message" role="status">
                        {shareMessage}
                    </span>
                )}
            </div>
        </div>
    );
}

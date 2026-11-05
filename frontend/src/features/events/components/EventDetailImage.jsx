import { getFallbackImage } from '@/common/utils/BrandUtil.js';
import { calculateDDay } from '@/common/utils/DateUtil.js';

export default function EventDetailImage({ detailEvent, isFavorite, favoriteDisabled, onToggleFavorite }) {
    return (
        <div className="detail-image-box">
            {detailEvent?.img ? (
                <img
                    src={detailEvent.img}
                    alt={detailEvent.title}
                    className="detail-image"
                    onError={(e) => {
                        e.currentTarget.onerror = null;
                        e.currentTarget.src = getFallbackImage(detailEvent.brand);
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
                aria-pressed={isFavorite}
                disabled={favoriteDisabled}
                onClick={onToggleFavorite}
            >
                <svg
                    className="favorite-icon"
                    viewBox="0 0 24 24"
                    aria-hidden="true"
                >
                    <path d="M20.8 8.8c0 5.1-8.8 10.1-8.8 10.1S3.2 13.9 3.2 8.8A4.8 4.8 0 0 1 12 6.1a4.8 4.8 0 0 1 8.8 2.7Z" />
                </svg>
                {isFavorite ? '찜 완료' : '찜하기'}
            </button>
        </div>
    );
}

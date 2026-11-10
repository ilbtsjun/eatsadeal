import { getFallbackImage } from '@/common/utils/BrandUtil.js';
import HeartIcon from '@/common/components/HeartIcon.jsx';
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
                <HeartIcon />
                {isFavorite ? '찜 완료' : '찜하기'}
            </button>
        </div>
    );
}

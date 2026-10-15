import { getFallbackImage } from '@/common/utils/BrandUtil.js';
import { calculateDDay } from '@/common/utils/DateUtil.js';
import { getEventCodeName } from '../utils/eventDisplay.js';

export default function EventCard({ event, toggleFavorite, onSelectEvent }) {
    return (
        <div className="event-card-wrapper">
            <button type="button" className="event-card" onClick={() => onSelectEvent?.(event)}>
                <div className="card-image-box">
                    {event.img ? (
                        <img
                            src={event.img}
                            alt={event.title}
                            className="card-image"
                            onLoad={(e) => {
                                if (e.currentTarget.naturalWidth <= 1)
                                    e.currentTarget.src = getFallbackImage(event.brand);
                            }}
                            onError={(e) => {
                                e.currentTarget.onerror = null;
                                e.currentTarget.src = getFallbackImage(event.brand);
                            }}
                        />
                    ) : <span className="card-emoji">🍗</span>}
                    <span className="card-dday">{calculateDDay(event.endDate)}</span>
                </div>
                <div className="card-info">
                    <span className="card-brand">{event.brand || '이츠어딜'}</span>
                    <h3 className="card-title">{event.title}</h3>
                    <div className="card-footer">
                        <span className="card-discount">{getEventCodeName(event.eventCodes)}</span>
                        <span className="card-views" aria-label="조회수">조회수 {Number(event.viewCount || 0).toLocaleString()}</span>
                    </div>
                </div>
            </button>
            <button
                type="button"
                className={`card-favorite ${event.isFavorite ? 'is-favorite' : ''}`}
                aria-label={event.isFavorite ? '찜 취소' : '찜하기'}
                aria-pressed={Boolean(event.isFavorite)}
                onClick={(clickEvent) => {
                    clickEvent.stopPropagation();
                    toggleFavorite(event);
                }}
            >
                <svg className="favorite-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M20.8 8.8c0 5.1-8.8 10.1-8.8 10.1S3.2 13.9 3.2 8.8A4.8 4.8 0 0 1 12 6.1a4.8 4.8 0 0 1 8.8 2.7Z" /></svg>
            </button>
        </div>
    );
}
import './HighlightBanner.css';
import { calculateDDay } from '@/common/utils/DateUtil.js';
import { useHighlightEvent } from '@/features/events/hooks/useHighlightEvent.js';

export default function HighlightBanner({ onSelectEvent }) {
    const event = useHighlightEvent();

    if (!event) return null;

    return (
        <div
            className="banner-container"
            role="button"
            tabIndex={0}
            onClick={() => onSelectEvent?.(event)}
            onKeyDown={(e) => { if (e.key === 'Enter') onSelectEvent?.(event); }}
        >
            <div className="banner-content">
                <span className="banner-tag">🔥 가장 인기 있는 할인</span>
                <h2>{event.brandName ? `[${event.brandName}] ` : ''}{event.title}</h2>
                <p>
                    조회수 {Number(event.viewCount || 0).toLocaleString()}
                    {event.endDate && ` · ${calculateDDay(event.endDate)}`}
                </p>
            </div>
        </div>
    );
}
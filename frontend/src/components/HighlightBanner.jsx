import { useEffect, useState } from 'react';
import { apiRequest } from '../api/client';
import './HighlightBanner.css';
import { calculateDDay } from '../utils/DateUtil.jsx'


export default function HighlightBanner({ onSelectEvent }) {
    const [event, setEvent] = useState(null);

    useEffect(() => {
        let cancelled = false;
        apiRequest('/api/events?page=0&size=1&sort=popular')
            .then((data) => { if (!cancelled) setEvent(data.content?.[0] ?? null); })
            .catch(() => { if (!cancelled) setEvent(null); }); // 실패하면 배너만 숨김
        return () => { cancelled = true; };
    }, []);

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
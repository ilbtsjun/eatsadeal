import { useEffect, useState } from 'react';
import { getHighlight } from '@/api/index.js';
import './HighlightBanner.css';
import { calculateDDay } from '@/common/utils/DateUtil.js'


export default function HighlightBanner({ onSelectEvent }) {
    const [event, setEvent] = useState(null);

    useEffect(() => {
        let cancelled = false;
        getHighlight()
            .then((data) => { if (!cancelled) setEvent(data.content?.[0] ?? null); })
            .catch(() => { if (!cancelled) setEvent(null); });
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
import { useEffect, useState } from 'react';
import { getHighlight } from '@/api/index.js';

export function useHighlightEvent() {
    const [event, setEvent] = useState(null);

    useEffect(() => {
        let cancelled = false;
        getHighlight()
            .then((data) => { if (!cancelled) setEvent(data.content?.[0] ?? null); })
            .catch(() => { if (!cancelled) setEvent(null); });
        return () => { cancelled = true; };
    }, []);

    return event;
}

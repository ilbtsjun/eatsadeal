import { getHighlight } from '@/api/index.js';
import { useFetch } from '@/common/hooks/useFetch.js';

const fetchHighlight = () => getHighlight().then((data) => data.content?.[0] ?? null);

export function useHighlightEvent() {
    const { data: event } = useFetch(fetchHighlight);
    return event;
}

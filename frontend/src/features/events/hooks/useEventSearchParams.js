import { useSearchParams } from 'react-router';

const DEFAULTS = { category: 'all', eventCode: 'all', sort: 'latest', q: '', page: 1 };

export function useEventSearchParams(){
    const [params, setParams] = useSearchParams();
    const category = params.get('category') ?? DEFAULTS.category;
    const eventCode = params.get('eventCode') ?? DEFAULTS.eventCode;
    const sort = params.get('sort') ?? DEFAULTS.sort;
    const keyword = params.get('q') ?? DEFAULTS.q;
    const page = Math.max(1, Number(params.get('page')) || 1) - 1;

    const updateSearchParams = (patch, { keepPage = false, replace = false } = {}) => {
        const next = new URLSearchParams(params);
        Object.entries(patch).forEach(([key, value]) => {
            if (value == null || value === '' || value === DEFAULTS[key])
                next.delete(key);
            else
                next.set(key, String(value));
        });
        if (!keepPage)
            next.delete('page');
        if (next.toString() === params.toString())
            return;
        setParams(next, { replace });
    };
    return{
        category,
        eventCode,
        sort,
        keyword,
        page,
        updateSearchParams
    }
}
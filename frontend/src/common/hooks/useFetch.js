import { useCallback, useEffect, useRef, useState } from 'react';

export function useFetch(request, { initialData = null, fallbackMessage = '정보를 불러오지 못했습니다.' } = {}) {
    const [data, setData] = useState(initialData);
    const [loading, setLoading] = useState(Boolean(request));
    const [loadError, setLoadError] = useState('');
    const [loadErrorStatus, setLoadErrorStatus] = useState(null);
    const [reloadKey, setReloadKey] = useState(0);
    const [prevRequest, setPrevRequest] = useState(() => request);
    const requestRef = useRef(request);

    if (prevRequest !== request) {
        setPrevRequest(() => request);
        setLoading(Boolean(request));
        setLoadError('');
        setLoadErrorStatus(null);
    }

    useEffect(() => {
        requestRef.current = request;
    }, [request]);

    useEffect(() => {
        if (!request) return undefined;
        let cancelled = false;

        request()
            .then((result) => {
                if (!cancelled) setData(result);
            })
            .catch((error) => {
                if (cancelled) return;
                setLoadError(error.message || fallbackMessage);
                setLoadErrorStatus(error.status ?? null);
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => { cancelled = true; };
    }, [request, reloadKey, fallbackMessage]);

    const reload = () => {
        if (!request) return;
        setLoading(true);
        setLoadError('');
        setLoadErrorStatus(null);
        setReloadKey((key) => key + 1);
    };

    const refresh = useCallback(async () => {
        if (!request) return;
        const result = await request();
        if (requestRef.current === request) setData(result);
    }, [request]);

    return { data, setData, loading, loadError, loadErrorStatus, reload, refresh };
}

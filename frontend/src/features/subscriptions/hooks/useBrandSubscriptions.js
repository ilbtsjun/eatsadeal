import { useEffect, useState } from 'react';
import { getListBrands } from '@/api/index.js';
import { useSubscribedBrandIds } from './useSubscribedBrandIds.js';

export function useBrandSubscriptions(user) {
    const [brands, setBrands] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [reloadKey, setReloadKey] = useState(0);

    const {
        subscribedIds,
        pendingId,
        toggleError,
        toggleBrandSubscribe,
        consentPrompt,
    } = useSubscribedBrandIds(user);

    useEffect(() => {
        let cancelled = false;

        getListBrands()
            .then((data) => {
                if (cancelled) return;
                setBrands((Array.isArray(data) ? data : [])
                    .filter((brand) => brand.id != null && brand.name && brand.isActive)
                    .map((brand) => ({ id: brand.id, name: brand.name, url: brand.url, img: brand.img })));
                setError('');
            })
            .catch((requestError) => {
                if (!cancelled) setError(requestError.message);
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => { cancelled = true; };
    }, [reloadKey]);

    const reload = () => {
        setLoading(true);
        setError('');
        setReloadKey((key) => key + 1);
    };

    return { brands, loading, error, reload, subscribedIds, pendingId, toggleError, toggleBrandSubscribe, consentPrompt };
}

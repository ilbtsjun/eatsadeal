import { useEffect, useState } from 'react';
import { getListBrandSubscriptions } from '@/api/index.js';
import { useBrandSubscribeToggle } from './useBrandSubscribeToggle.js';

const EMPTY_IDS = new Set();

export function useSubscribedBrandIds(user) {
    const [subscribedIds, setSubscribedIds] = useState(() => new Set());

    const {
        pendingId,
        toggleError,
        toggleBrandSubscribe: toggleBrandSubscribeRequest,
    } = useBrandSubscribeToggle();

    useEffect(() => {
        if (!user) return undefined;
        let cancelled = false;

        getListBrandSubscriptions()
            .then((ids) => {
                if (!cancelled) setSubscribedIds(new Set(Array.isArray(ids) ? ids : []));
            })
            .catch(() => {
                if (!cancelled) setSubscribedIds(new Set());
            });

        return () => { cancelled = true; };
    }, [user]);

    const toggleBrandSubscribe = (brandId) => {
        return toggleBrandSubscribeRequest(
            { id: brandId, isSubscribe: subscribedIds.has(brandId) },
            (nextSubscribe) => setSubscribedIds((current) => {
                const updated = new Set(current);
                if (nextSubscribe) updated.add(brandId);
                else updated.delete(brandId);
                return updated;
            })
        );
    };

    return {
        subscribedIds: user ? subscribedIds : EMPTY_IDS,
        pendingId,
        toggleError,
        toggleBrandSubscribe,
    };
}

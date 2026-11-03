import { useEffect, useState } from 'react';
import { getEvent } from '@/api/index.js';
import { useFavoriteToggle } from '@/features/favorites/hooks/useFavoriteToggle.js';
import { useSubscribedBrandIds } from '@/features/account/hooks/useSubscribedBrandIds.js';

export function useEventDetail({ event, user, onLoginClick }) {
    const [detailEvent, setDetailEvent] = useState(event);
    const [loaded, setLoaded] = useState(Boolean(event?.title));
    const [loadError, setLoadError] = useState(null);
    const [isFavorite, setIsFavorite] = useState(Boolean(event?.isFavorite));
    const [reloadKey, setReloadKey] = useState(0);
    const requestKey = `${event.id}|${reloadKey}`;
    const [prevRequestKey, setPrevRequestKey] = useState(requestKey);

    if (prevRequestKey !== requestKey) {
        setPrevRequestKey(requestKey);
        setLoaded(false);
        setLoadError(null);
    }

    const {
        pendingId,
        toggleError,
        toggleFavorite: toggleFavoriteRequest,
    } = useFavoriteToggle();

    const {
        subscribedIds,
        pendingId: subscribePendingId,
        toggleError: subscribeError,
        toggleBrandSubscribe,
    } = useSubscribedBrandIds(user);

    const brandId = detailEvent?.brandId;
    const isSubscribed = brandId != null && subscribedIds.has(brandId);

    useEffect(() => {
        let cancelled = false;

        getEvent(event.id)
            .then((data) => {
                if (cancelled) return;

                setDetailEvent({
                    ...data,
                    brand: data.brandName,
                });
                setIsFavorite(Boolean(data.isFavorite));
                setLoaded(true);
            })
            .catch((error) => {
                if (cancelled) return;

                setLoadError(error);
                setLoaded(true);
            });

        return () => {
            cancelled = true;
        };
    }, [event.id, reloadKey]);

    const reload = () => setReloadKey((key) => key + 1);

    const handleToggleFavorite = () => {
        if (!user) {
            onLoginClick?.();
            return;
        }

        return toggleFavoriteRequest(
            { id: event.id, isFavorite },
            setIsFavorite
        );
    };

    const handleToggleSubscribe = () => {
        if (!user) {
            onLoginClick?.();
            return;
        }
        if (brandId == null) return;

        return toggleBrandSubscribe(brandId);
    };

    return {
        detailEvent,
        loaded,
        loadError,
        isFavorite,
        pendingId,
        toggleError,
        handleToggleFavorite,
        reload,
        isSubscribed,
        subscribePendingId,
        subscribeError,
        handleToggleSubscribe,
    };
}
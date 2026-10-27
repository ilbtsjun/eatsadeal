import { useEffect, useState } from 'react';
import { getEvent } from '@/api/index.js';
import { useFavoriteToggle } from '@/features/favorites/hooks/useFavoriteToggle.js';

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

    return {
        detailEvent,
        loaded,
        loadError,
        isFavorite,
        pendingId,
        toggleError,
        handleToggleFavorite,
        reload,
    };
}
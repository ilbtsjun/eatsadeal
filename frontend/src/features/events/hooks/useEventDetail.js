import { useEffect, useState } from 'react';
import { getEvent } from '@/api/index.js';
import { usePageTitle } from '@/common/hooks/usePageTitle.js';
import { useFavoriteToggle } from '@/features/favorites/hooks/useFavoriteToggle.js';
import { useSubscribedBrandIds } from '@/features/subscriptions/hooks/useSubscribedBrandIds.js';

export function useEventDetail({ event, user, onLoginClick }) {
    const [detailEvent, setDetailEvent] = useState(event);
    const [loaded, setLoaded] = useState(Boolean(event?.title));
    const [loadError, setLoadError] = useState(null);
    const [isFavorite, setIsFavorite] = useState(Boolean(event?.isFavorite));
    const [reloadKey, setReloadKey] = useState(0);
    const [shareMessage, setShareMessage] = useState('');
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

    useEffect(() => {
        if (!shareMessage) return;
        const timer = setTimeout(() => setShareMessage(''), 2000);
        return () => clearTimeout(timer);
    }, [shareMessage]);

    usePageTitle(detailEvent?.title || '이벤트', {
        description: detailEvent?.description,
        path: `/events/${event.id}`,
    });

    const reload = () => setReloadKey((key) => key + 1);

    const handleShare = () => {
        const url = `${window.location.origin}/events/${event.id}`;
        if (navigator.share && window.matchMedia('(pointer: coarse)').matches) {
            navigator.share({ title: detailEvent?.title, url }).catch(() => {});
            return;
        }
        navigator.clipboard
            .writeText(url)
            .then(() => setShareMessage('링크를 복사했어요'))
            .catch(() => setShareMessage('링크를 복사하지 못했어요'));
    };

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
        shareMessage,
        handleShare,
    };
}
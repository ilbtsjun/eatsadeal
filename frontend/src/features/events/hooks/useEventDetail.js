import { useEffect, useMemo, useState } from 'react';
import { getEvent } from '@/api/index.js';
import { useFetch } from '@/common/hooks/useFetch.js';
import { usePageTitle } from '@/common/hooks/usePageTitle.js';
import { useFavoriteToggle } from '@/features/favorites/hooks/useFavoriteToggle.js';
import { useSubscribedBrandIds } from '@/features/subscriptions/hooks/useSubscribedBrandIds.js';

const toDetailEvent = (data) => ({ ...data, brand: data.brandName });

export function useEventDetail({ event, user, onLoginClick }) {
    const [shareMessage, setShareMessage] = useState('');

    const request = useMemo(() => () => getEvent(event.id).then(toDetailEvent), [event.id]);
    const {
        data: fetchedEvent,
        setData: setFetchedEvent,
        loading,
        loadError,
        loadErrorStatus,
        reload,
    } = useFetch(request, { fallbackMessage: '이벤트를 불러오지 못했습니다.' });

    const isCurrent = (data) => data != null && Number(data.id) === Number(event.id);
    const detailEvent = isCurrent(fetchedEvent) ? fetchedEvent : event;
    const loaded = !loading || (detailEvent === event && Boolean(event.title));
    const isFavorite = Boolean(detailEvent?.isFavorite);

    const {
        pendingId,
        toggleError,
        toggleFavorite: toggleFavoriteRequest,
    } = useFavoriteToggle();

    const {
        subscribedIds,
        loading: subscriptionsLoading,
        loadError: subscriptionsError,
        pendingId: subscribePendingId,
        toggleError: subscribeToggleError,
        toggleBrandSubscribe,
        consentPrompt,
    } = useSubscribedBrandIds(user);

    const brandId = detailEvent?.brandId;
    const isSubscribed = brandId != null && subscribedIds.has(brandId);
    const subscribeDisabled = subscriptionsLoading
        || Boolean(subscriptionsError)
        || (subscribePendingId != null && subscribePendingId === brandId);
    const subscribeError = subscribeToggleError || subscriptionsError;

    useEffect(() => {
        if (!shareMessage) return;
        const timer = setTimeout(() => setShareMessage(''), 2000);
        return () => clearTimeout(timer);
    }, [shareMessage]);

    usePageTitle(detailEvent?.title || '이벤트', {
        description: detailEvent?.description,
        path: `/events/${event.id}`,
        noindex: loadErrorStatus === 404,
    });

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
            (nextFavorite) => setFetchedEvent((current) => ({
                ...(isCurrent(current) ? current : event),
                isFavorite: nextFavorite,
            }))
        );
    };

    const handleToggleSubscribe = () => {
        if (!user) {
            onLoginClick?.();
            return;
        }
        if (brandId == null || subscribeDisabled) return;

        return toggleBrandSubscribe(brandId);
    };

    return {
        detailEvent,
        loaded,
        loadError,
        notFound: loadErrorStatus === 404,
        isFavorite,
        pendingId,
        toggleError,
        handleToggleFavorite,
        reload,
        isSubscribed,
        subscribeDisabled,
        subscribeError,
        handleToggleSubscribe,
        consentPrompt,
        shareMessage,
        handleShare,
    };
}

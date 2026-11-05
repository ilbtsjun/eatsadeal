import { useMemo } from 'react';
import { getEvent, getListFavoriteIds } from '@/api/index.js';
import { useFetch } from '@/common/hooks/useFetch.js';
import { useFavoriteToggle } from '@/features/favorites/hooks/useFavoriteToggle.js';

const fetchFavorites = async () => {
    const data = await getListFavoriteIds();
    const rawFavorites = Array.isArray(data)
        ? data
        : data?.favorites || data?.content || [];

    const ids = rawFavorites
        .map((item) => (
            typeof item === 'object'
                ? item.id || item.eventId
                : item
        ))
        .filter(Boolean);

    const details = await Promise.allSettled(ids.map((id) => getEvent(id)));

    return details
        .filter((result) => result.status === 'fulfilled')
        .map((result) => ({ ...result.value, isFavorite: true }));
};

export function useFavoriteList(user) {
    const request = useMemo(() => (user ? () => fetchFavorites() : null), [user]);
    const {
        data: favorites,
        setData: setFavorites,
        loading,
        loadError,
        reload,
    } = useFetch(request, { initialData: [], fallbackMessage: '찜한 목록을 불러오지 못했습니다.' });

    const {
        pendingId,
        toggleError,
        toggleFavorite: toggleFavoriteRequest,
    } = useFavoriteToggle();

    const toggleFavorite = (event) => {
        return toggleFavoriteRequest(event, (nextFavorite) => {
            setFavorites((current) =>
                current.map((item) =>
                    item.id === event.id
                        ? { ...item, isFavorite: nextFavorite }
                        : item
                )
            );
        });
    };

    return {
        favorites,
        loading,
        loadError,
        toggleError,
        pendingId,
        toggleFavorite,
        reload,
    };
}

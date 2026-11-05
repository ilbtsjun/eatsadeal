import { useMemo } from 'react';
import { getListFavoriteEvents } from '@/api/index.js';
import { useFetch } from '@/common/hooks/useFetch.js';
import { useFavoriteToggle } from '@/features/favorites/hooks/useFavoriteToggle.js';

const fetchFavorites = async () => {
    const events = await getListFavoriteEvents();
    return events.map((event) => ({ ...event, isFavorite: true }));
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

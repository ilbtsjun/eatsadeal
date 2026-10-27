import { useEffect, useState } from 'react';
import { getEvent, getListFavoriteIds } from '@/api/index.js';
import { useFavoriteToggle } from '@/features/favorites/hooks/useFavoriteToggle.js';

export function useFavoriteList(user) {
    const [favorites, setFavorites] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [prevUser, setPrevUser] = useState(user);

    if (prevUser !== user) {
        setPrevUser(user);
        setLoading(true);
        setError('');
    }

    const {
        pendingId,
        toggleError,
        toggleFavorite: toggleFavoriteRequest,
    } = useFavoriteToggle();

    useEffect(() => {
        let cancelled = false;

        getListFavoriteIds()
            .then(async (data) => {
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

                const details = await Promise.allSettled(
                    ids.map((id) => getEvent(id))
                );

                if (!cancelled) {
                    setFavorites(
                        details
                            .filter((result) => result.status === 'fulfilled')
                            .map((result) => ({
                                ...result.value,
                                isFavorite: true,
                            }))
                    );
                    setError('');
                }
            })
            .catch((requestError) => {
                if (!cancelled) {
                    setError(requestError.message);
                }
            })
            .finally(() => {
                if (!cancelled) {
                    setLoading(false);
                }
            });

        return () => {
            cancelled = true;
        };
    }, [user]);

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
        error,
        toggleError,
        pendingId,
        toggleFavorite,
    };
}
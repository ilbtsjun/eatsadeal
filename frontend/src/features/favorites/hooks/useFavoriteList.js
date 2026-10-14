import { useEffect, useState } from 'react';
import { getEvent, getListFavoriteIds, setFavorite } from '@/api/index.js';

export function useFavoriteList(user) {
    const [favorites, setFavorites] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [toggleError, setToggleError] = useState('');
    const [pendingId, setPendingId] = useState(null);

    useEffect(() => {
        let cancelled = false;
        setLoading(true);
        setError('');

        getListFavoriteIds()
            .then(async (data) => {
                const rawFavorites = Array.isArray(data) ? data : data?.favorites || data?.content || [];
                const ids = rawFavorites
                    .map((item) => (typeof item === 'object' ? item.id || item.eventId : item))
                    .filter(Boolean);
                const details = await Promise.allSettled(ids.map((id) => getEvent(id)));

                if (!cancelled) {
                    setFavorites(details
                        .filter((result) => result.status === 'fulfilled')
                        .map((result) => ({ ...result.value, isFavorite: true })));
                    setError('');
                }
            })
            .catch((requestError) => {
                if (!cancelled) setError(requestError.message);
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => { cancelled = true; };
    }, [user]);

    const toggleFavorite = async (event) => {
        if (pendingId === event.id) return;

        const nextFavorite = !event.isFavorite;
        setPendingId(event.id);
        setToggleError('');

        try {
            await setFavorite(event.id, nextFavorite);
            setFavorites((current) => current.map((item) => (
                item.id === event.id ? { ...item, isFavorite: nextFavorite } : item
            )));
        } catch (requestError) {
            setToggleError(requestError.message);
        } finally {
            setPendingId(null);
        }
    };

    return { favorites, setFavorites, loading, error, toggleError, pendingId, toggleFavorite };
}

import { useState } from 'react';
import { setFavorite } from '@/api/index.js';

export function useFavoriteToggle() {
    const [pendingId, setPendingId] = useState(null);
    const [toggleError, setToggleError] = useState('');

    const toggleFavorite = async (event, onSuccess) => {
        if (pendingId === event.id) return;

        const nextFavorite = !event.isFavorite;
        setPendingId(event.id);
        setToggleError('');

        try {
            await setFavorite(event.id, nextFavorite);
            onSuccess?.(nextFavorite);
        } catch (error) {
            setToggleError(error.message);
        } finally {
            setPendingId(null);
        }
    };

    return {
        pendingId,
        toggleError,
        toggleFavorite,
        setToggleError,
    };
}
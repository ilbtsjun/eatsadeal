import { useState } from 'react';
import { setBrandSubscribe } from '@/api/index.js';

export function useBrandSubscribeToggle() {
    const [pendingId, setPendingId] = useState(null);
    const [toggleError, setToggleError] = useState('');

    const toggleBrandSubscribe = async (brand, onSuccess) => {
        if (pendingId === brand.id) return;

        const nextSubscribe = !brand.isSubscribe;
        setPendingId(brand.id);
        setToggleError('');

        try {
            await setBrandSubscribe(brand.id, nextSubscribe);
            onSuccess?.(nextSubscribe);
        } catch (error) {
            setToggleError(error.message);
        } finally {
            setPendingId(null);
        }
    };

    return {
        pendingId,
        toggleError,
        toggleBrandSubscribe,
        setToggleError,
    };
}
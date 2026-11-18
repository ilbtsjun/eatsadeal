import { useState } from 'react';
import { deactivateEvent } from '@/api/index.js';
import { usePending } from '@/common/hooks/usePending.js';

export function useEventDeactivate({ eventId, onDone }) {
    const [confirmOpen, setConfirmOpen] = useState(false);
    const [error, setError] = useState('');
    const [pending, withPending] = usePending();

    const open = () => {
        setError('');
        setConfirmOpen(true);
    };

    const close = () => {
        if (pending) return;
        setConfirmOpen(false);
        setError('');
    };

    const confirm = () => withPending(async () => {
        setError('');
        try {
            await deactivateEvent(eventId);
            setConfirmOpen(false);
            onDone?.();
        } catch (err) {
            setError(err.message || '이벤트를 종료하지 못했습니다.');
        }
    });

    return { confirmOpen, open, close, confirm, pending, error };
}

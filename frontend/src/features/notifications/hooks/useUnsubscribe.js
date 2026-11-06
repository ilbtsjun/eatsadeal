import { useState } from 'react';
import { unsubscribeByToken } from '@/api/index.js';

export function useUnsubscribe(token) {
    const [status, setStatus] = useState(token ? 'idle' : 'invalid');
    const [error, setError] = useState('');

    const unsubscribe = async () => {
        if (!token || status === 'submitting') return;

        setStatus('submitting');
        setError('');

        try {
            await unsubscribeByToken(token);
            setStatus('done');
        } catch (requestError) {
            if (requestError.status === 404) {
                setStatus('invalid');
                return;
            }
            setError(requestError.message || '수신거부를 처리하지 못했습니다.');
            setStatus('idle');
        }
    };

    return { status, error, unsubscribe };
}

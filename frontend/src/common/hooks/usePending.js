import { useCallback, useRef, useState } from 'react';

export function usePending() {
    const [pending, setPending] = useState(false);
    const pendingRef = useRef(false);

    const run = useCallback(async (task) => {
        if (pendingRef.current) return undefined;
        pendingRef.current = true;
        setPending(true);
        try {
            return await task();
        } finally {
            pendingRef.current = false;
            setPending(false);
        }
    }, []);

    return [pending, run];
}

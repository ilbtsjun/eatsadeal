import { useCallback, useEffect, useState } from 'react';
import { getAdminDashboard } from '@/api/index.js';

export function useAdminDashboard() {
    const [dashboard, setDashboard] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    const fetchDashboard = useCallback(() => (
        getAdminDashboard()
            .then((data) => {
                setDashboard(data);
                setError('');
            })
            .catch((err) => setError(err.message))
            .finally(() => setLoading(false))
    ), []);

    const loadDashboard = useCallback(() => {
        setLoading(true);
        return fetchDashboard();
    }, [fetchDashboard]);

    useEffect(() => {
        fetchDashboard();
    }, [fetchDashboard]);

    return { dashboard, loading, error, loadDashboard };
}

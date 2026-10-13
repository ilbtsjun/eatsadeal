import { useState, useEffect } from 'react';
import { getToken } from '@/auth/Token.jsx';
import { getListEvents, getListFavoriteIds } from '@/api/index.js';

const PAGE_SIZE = 20;

export function useEventList({ activeCategory, activeSort, searchKeyword, page, user, onPageChange }) {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);

    useEffect(() => {
        let cancelled = false;

        const params = new URLSearchParams({
            page: String(page),
            size: String(PAGE_SIZE),
            sort: activeSort === 'deadline' ? 'endingSoon' : activeSort === 'popular' ? 'popular' : 'latest',
        });

        if (activeCategory !== 'all' && /^\d+$/.test(String(activeCategory)))
            params.set('categoryId', activeCategory);
        if (searchKeyword.trim())
            params.set('keyword', searchKeyword.trim());

        setLoading(true);
        setError('');

        const favoriteRequest = user ? getListFavoriteIds() : Promise.resolve([]);

        Promise.all([getListEvents(params), favoriteRequest])
            .then(([data, favoriteIds]) => {
                if (cancelled)
                    return;
                const favoriteSet = new Set(Array.isArray(favoriteIds) ? favoriteIds.map(Number) : []);
                setTotalPages(Number(data.totalPages || 0));
                setTotalElements(Number(data.totalElements || data.content?.length || 0));
                setEvents((data.content || []).map((event) => ({
                    ...event,
                    brand: event.brandName,
                    isFavorite: favoriteSet.has(Number(event.id)),
                })));
            })
            .catch((requestError) => {
                if (!cancelled)
                    setError(requestError.message);
            })
            .finally(() => {
                if (!cancelled)
                    setLoading(false);
            });

        return () => { cancelled = true; };
    }, [user, activeCategory, activeSort, searchKeyword, page]);

    useEffect(() => {
        if (!loading && totalPages > 0 && page >= totalPages) {
            onPageChange?.(totalPages - 1, true);
        }
    }, [loading, totalPages, page, onPageChange]);

    return {
        events,
        setEvents,
        loading,
        error,
        setError,
        totalPages,
        totalElements,
    };
}
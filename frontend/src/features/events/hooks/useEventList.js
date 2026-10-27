import { useState, useEffect } from 'react';
import { getListEvents, getListFavoriteIds, setFavorite } from '@/api/index.js';
import { EVENT_CODE_NAMES } from '../utils/eventDisplay.js';

const PAGE_SIZE = 20;

export function useEventList({ activeCategory, activeEventCode = 'all', activeSort, searchKeyword, page, user, onPageChange }) {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const requestKey = [user?.id, activeCategory, activeEventCode, activeSort, searchKeyword, page].join('|');
    const [prevRequestKey, setPrevRequestKey] = useState(requestKey);

    if (prevRequestKey !== requestKey) {
        setPrevRequestKey(requestKey);
        setLoading(true);
        setError('');
    }

    useEffect(() => {
        let cancelled = false;

        const params = new URLSearchParams({
            page: String(page),
            size: String(PAGE_SIZE),
            sort: activeSort === 'deadline' ? 'endingSoon' : activeSort === 'popular' ? 'popular' : 'latest',
        });

        if (activeCategory !== 'all' && /^\d+$/.test(String(activeCategory)))
            params.set('categoryId', activeCategory);
        if (EVENT_CODE_NAMES[activeEventCode])
            params.set('eventCode', activeEventCode);
        if (searchKeyword.trim())
            params.set('keyword', searchKeyword.trim());

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
    }, [user, activeCategory, activeEventCode, activeSort, searchKeyword, page]);

    useEffect(() => {
        if (!loading && totalPages > 0 && page >= totalPages) {
            onPageChange?.(totalPages - 1, true);
        }
    }, [loading, totalPages, page, onPageChange]);

    const toggleFavorite = async (event) => {
        if (!user) {
            window.alert('찜하기 기능을 사용하려면 로그인해주세요.');
            return;
        }

        const nextFavorite = !event.isFavorite;

        try {
            await setFavorite(event.id, nextFavorite);
            setEvents((current) => current.map((item) => (
                item.id === event.id
                    ? { ...item, isFavorite: nextFavorite }
                    : item
            )));
        } catch (requestError) {
            setError(requestError.message);
        }
    };

    return {
        events,
        loading,
        error,
        totalPages,
        totalElements,
        toggleFavorite,
    };
}
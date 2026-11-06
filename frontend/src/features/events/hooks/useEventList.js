import { useEffect, useMemo, useState } from 'react';
import { getListEvents, getListFavoriteIds, setFavorite } from '@/api/index.js';
import { useFetch } from '@/common/hooks/useFetch.js';
import { EVENT_CODE_NAMES } from '../utils/eventDisplay.js';

const PAGE_SIZE = 20;
const EMPTY_RESULT = { events: [], totalPages: 0, totalElements: 0 };

export function useEventList({ activeCategory, activeBrand = 'all', activeEventCode = 'all', activeSort, searchKeyword, page, user, onPageChange }) {
    const [actionError, setActionError] = useState('');

    const request = useMemo(() => () => {
        const params = new URLSearchParams({
            page: String(page),
            size: String(PAGE_SIZE),
            sort: activeSort === 'deadline' ? 'endingSoon' : activeSort === 'popular' ? 'popular' : 'latest',
        });

        if (activeCategory !== 'all' && /^\d+$/.test(String(activeCategory)))
            params.set('categoryId', activeCategory);
        if (activeBrand !== 'all' && /^\d+$/.test(String(activeBrand)))
            params.set('brandId', activeBrand);
        if (EVENT_CODE_NAMES[activeEventCode])
            params.set('eventCode', activeEventCode);
        if (searchKeyword.trim())
            params.set('keyword', searchKeyword.trim());

        const favoriteRequest = user ? getListFavoriteIds() : Promise.resolve([]);

        return Promise.all([getListEvents(params), favoriteRequest])
            .then(([data, favoriteIds]) => {
                const favoriteSet = new Set(Array.isArray(favoriteIds) ? favoriteIds.map(Number) : []);
                return {
                    totalPages: Number(data.totalPages || 0),
                    totalElements: Number(data.totalElements || data.content?.length || 0),
                    events: (data.content || []).map((event) => ({
                        ...event,
                        brand: event.brandName,
                        isFavorite: favoriteSet.has(Number(event.id)),
                    })),
                };
            });
    }, [user, activeCategory, activeBrand, activeEventCode, activeSort, searchKeyword, page]);

    const {
        data: { events, totalPages, totalElements },
        setData,
        loading,
        loadError,
        reload,
    } = useFetch(request, { initialData: EMPTY_RESULT, fallbackMessage: '이벤트를 불러오지 못했습니다.' });

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
        setActionError('');

        try {
            await setFavorite(event.id, nextFavorite);
            setData((current) => ({
                ...current,
                events: current.events.map((item) => (
                    item.id === event.id
                        ? { ...item, isFavorite: nextFavorite }
                        : item
                )),
            }));
        } catch (requestError) {
            setActionError(requestError.message || '찜하기에 실패했습니다.');
        }
    };

    return {
        events,
        loading,
        loadError,
        actionError,
        totalPages,
        totalElements,
        toggleFavorite,
        reload,
    };
}
import { useMemo } from 'react';
import { getListBrandSubscriptions } from '@/api/index.js';
import { useFetch } from '@/common/hooks/useFetch.js';
import { useBrandSubscribeToggle } from './useBrandSubscribeToggle.js';
import { useConsentPrompt } from '@/features/notifications/hooks/useConsentPrompt.js';

const EMPTY_IDS = new Set();

const fetchSubscribedIds = async () => {
    const ids = await getListBrandSubscriptions();
    return new Set(Array.isArray(ids) ? ids : []);
};

export function useSubscribedBrandIds(user) {
    const request = useMemo(() => (user ? () => fetchSubscribedIds() : null), [user]);
    const {
        data: subscribedIds,
        setData: setSubscribedIds,
        loading,
        loadError,
        reload,
    } = useFetch(request, { initialData: EMPTY_IDS, fallbackMessage: '구독 정보를 불러오지 못했습니다.' });

    const {
        pendingId,
        toggleError,
        toggleBrandSubscribe: toggleBrandSubscribeRequest,
    } = useBrandSubscribeToggle();

    const consentPrompt = useConsentPrompt(user);

    const toggleBrandSubscribe = (brandId) => {
        if (loading || loadError) return;

        return toggleBrandSubscribeRequest(
            { id: brandId, isSubscribe: subscribedIds.has(brandId) },
            (nextSubscribe) => {
                setSubscribedIds((current) => {
                    const updated = new Set(current);
                    if (nextSubscribe) updated.add(brandId);
                    else updated.delete(brandId);
                    return updated;
                });
                if (nextSubscribe) consentPrompt.promptAfterSubscribe();
            }
        );
    };

    return {
        subscribedIds: user ? subscribedIds : EMPTY_IDS,
        loading,
        loadError,
        reload,
        pendingId,
        toggleError,
        toggleBrandSubscribe,
        consentPrompt,
    };
}

import { getListBrands } from '@/api/index.js';
import { useFetch } from '@/common/hooks/useFetch.js';
import { useSubscribedBrandIds } from './useSubscribedBrandIds.js';

const fetchActiveBrands = async () => {
    const data = await getListBrands();
    return (Array.isArray(data) ? data : [])
        .filter((brand) => brand.id != null && brand.name && brand.isActive)
        .map((brand) => ({
            id: brand.id,
            name: brand.name,
            url: brand.url,
            img: brand.img,
            eventsPath: `/?brand=${brand.id}`,
        }));
};

export function useBrandSubscriptions(user) {
    const {
        data: brands,
        loading: brandsLoading,
        loadError: brandsError,
        reload: reloadBrands,
    } = useFetch(fetchActiveBrands, { initialData: [], fallbackMessage: '브랜드 목록을 불러오지 못했습니다.' });

    const {
        subscribedIds,
        loading: subscriptionsLoading,
        loadError: subscriptionsError,
        reload: reloadSubscriptions,
        pendingId,
        toggleError,
        toggleBrandSubscribe,
        consentPrompt,
    } = useSubscribedBrandIds(user);

    const reload = () => {
        if (brandsError) reloadBrands();
        if (subscriptionsError) reloadSubscriptions();
    };

    return {
        brands,
        loading: brandsLoading || subscriptionsLoading,
        loadError: brandsError || subscriptionsError,
        reload,
        subscribedIds,
        pendingId,
        toggleError,
        toggleBrandSubscribe,
        consentPrompt,
    };
}

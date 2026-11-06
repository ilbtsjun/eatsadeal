import { getAdminDashboard } from '@/api/index.js';
import { useFetch } from '@/common/hooks/useFetch.js';

export function useAdminDashboard() {
    const { data: dashboard, loading, loadError, reload } = useFetch(getAdminDashboard, {
        fallbackMessage: '대시보드를 불러오지 못했습니다.',
    });

    return { dashboard, loading, loadError, reload };
}

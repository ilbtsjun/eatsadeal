import {api} from "./client.js";

export const getUserInfo = (query) => api.get(`/api/admin/users?${query}`);
export const getUserComments = (id) => api.get(`/api/admin/users/${id}/comments`);
export const suspendUser = (id, suspendTime, reason, release) =>
    api.patch(`/api/admin/users/${id}/suspension`, {
            suspendTime,
            suspendReason: reason,
            status: !release,
        },
    );
export const runCrawler = (path) => api.post(`${path}`);
export const getLatestCrawlRuns = () => api.get('/api/crawl/runs/latest');
export const getCrawlRuns = (limit = 50) => api.get(`/api/crawl/runs?limit=${limit}`);
export const backfillEventCodes = (limit = 50, afterId) =>
    api.post(`/api/events/event-codes/backfill?limit=${limit}${afterId ? `&afterId=${afterId}` : ''}`);
export const getAdminDashboard = () => api.get('/api/admin/dashboard');

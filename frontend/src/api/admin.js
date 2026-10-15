import {api} from "./client.js";

export const getUserInfo = (query) => api.get(`/api/user/admin/user?${query}`);
export const getUserComments = (id) => api.get(`/api/comments/user/${id}`);
export const suspendUser = (id, suspendTime, reason, release) =>
    api.patch(`/api/user/admin/${id}/suspension`, {
            suspendTime,
            suspendReason: reason,
            status: !release,
        },
    );
export const runCrawler = (path) => api.post(`${path}`);
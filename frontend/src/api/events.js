import {api} from './client.js';

export const getListEvents = (params) => api.get(`/api/events?${new URLSearchParams(params)}`);
export const getEvent = (id) => api.get(`/api/events/${id}`);
export const getListFavoriteIds = () => api.get('/api/user/me/favorites');
export const setFavorite = (id, on) => {
    const url = `/api/events/${id}/favorite`;
    return on ? api.post(url) : api.del(url);
};
export const getHighlight = () => api.get('/api/events?page=0&size=1&sort=popular');
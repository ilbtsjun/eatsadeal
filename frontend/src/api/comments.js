import { api } from './client.js'

export const listComments = (eventId) => api.get(`/api/events/${eventId}/comments`);
export const createComment = (eventId, content, parentId) =>
    api.post(`/api/events/${eventId}/comments`, parentId != null ? { content, parentId } : { content });
export const updateComment = (id, content) => api.patch(`/api/comments/${id}`, { content });
export const deleteComment = (id) => api.del(`/api/comments/${id}`);
export const hideComment = (id) => api.patch(`/api/comments/${id}/hide`);
export const unhideComment = (id) => api.patch(`/api/comments/${id}/unhide`);
export const listMyComments = () => api.get('/api/comments');
export const listUserComments = (userId) => api.get(`/api/comments/user/${userId}`);
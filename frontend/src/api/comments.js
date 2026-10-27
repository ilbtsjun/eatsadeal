import { api } from '@/api/client.js';

export const getListComments = (eventId) => api.get(`/api/events/${eventId}/comments`);
export const createComment = (eventId, content, parentId) =>
    api.post(`/api/events/${eventId}/comments`, parentId != null ? { content, parentId } : { content },);
export const updateComment = (commentId, content) => api.patch(`/api/comments/${commentId}`, { content });
export const deleteComment = (commentId) => api.del(`/api/comments/${commentId}`);
export const hideComment = (commentId) => api.patch(`/api/admin/comments/${commentId}/hide`);
export const unhideComment = (commentId) => api.patch(`/api/admin/comments/${commentId}/unhide`);
export const getMyComments = () => api.get('/api/comments');
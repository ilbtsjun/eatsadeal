import { api } from './client.js';

export const getCurrentUser = (options) => api.get('/api/user/me', options);
export const updateCurrentUser = (profile) => api.patch('/api/user/me', profile);
export const updatePassword = (passwordData) => api.patch('/api/user/me/password', passwordData);
export const deleteCurrentUser = (password) => api.del('/api/user/me', { password });

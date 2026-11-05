import { api } from './client.js';

export const getCurrentUser = (options) => api.get('/api/user/me', options);
export const updateCurrentUser = (profile) => api.patch('/api/user/me', profile);
export const updatePassword = (passwordData) => api.patch('/api/user/me/password', passwordData);
export const deleteCurrentUser = (password) => api.del('/api/user/me', { password });
export const getListBrandSubscriptions = () => api.get('/api/user/me/subscriptions');
export const getMyConsents = () => api.get('/api/user/me/consents');
export const updateMyConsents = (consents) => api.patch('/api/user/me/consents', consents);

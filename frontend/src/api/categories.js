import { api } from './client.js';

export const getListCategories = () => api.get('/api/categories');
export const deleteCategory = (id) => api.del(`/api/categories/${id}`);
export const createCategory = (body) => api.post(`/api/categories`, body);
export const updateCategory = (id, body) => api.patch(`/api/categories/${id}`, body);
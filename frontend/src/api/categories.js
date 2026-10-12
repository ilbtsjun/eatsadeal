import { api } from './client.js';

export const getListCategories = () => api.get('/api/categories');
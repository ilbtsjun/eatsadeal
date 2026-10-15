import { api } from './client.js';

export const createReport = (data) => api.post('/api/reports', data);
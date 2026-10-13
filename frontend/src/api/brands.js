import {api} from "./client.js";

export const getListBrands = () => api.get('/api/brands');
export const activateBrand = (id) => api.patch(`/api/brands/${id}/active`)
export const deactivateBrand = (id) => api.del(`/api/brands/${id}`)
export const getBrand = (id) => api.get(`/api/brands/${id}`);
export const createBrand = (body) => api.post(`/api/brands`, body);
export const updateBrand = (id, body) => api.patch(`/api/brands/${id}`, body);
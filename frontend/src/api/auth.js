import {api} from "./client.js";

export const login = (credentials) => api.post('/api/auth/login', credentials, { skipAuth: true });
export const signup = (userData) => api.post('/api/auth/signup', userData, { skipAuth: true });
export const verifyEmail = (data) => api.post('/api/auth/email-verification', data, { skipAuth: true });
export const checkEmailAvailability = (email) => api.get(
        `/api/user/email-availability?emailID=${encodeURIComponent(email)}`,
        { skipAuth: true }
    );
export const checkNicknameAvailability = (nickname) => api.get(
        `/api/user/nickname-availability?nickname=${encodeURIComponent(nickname)}`,
        { skipAuth: true }
    );
export const logout = () => api.post('/api/auth/logout');
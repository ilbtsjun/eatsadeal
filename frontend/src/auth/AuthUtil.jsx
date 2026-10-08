import {apiRequest} from "../api/client.js"
import { getToken, setToken, removeToken, getTokenExp } from "./Token.jsx"

const SAVED_USER_KEY = 'eats-a-deal-user';

export function getSavedUser() {
    try {
        const u = JSON.parse(localStorage.getItem(SAVED_USER_KEY));
        const token = getToken();

        if (u && token && getTokenExp(token) > Date.now())
            return { ...u, token };
    } catch { /* */ }
    clearSession();
    return null;
}

export function clearSession() {
    removeToken();
    localStorage.removeItem(SAVED_USER_KEY);
}

export function expireSession(){
    clearSession();
    window.dispatchEvent(new Event('auth:expired'));
}

export const handleLogin = (u) => {
    const { token, ...user } = u;
    localStorage.setItem(SAVED_USER_KEY, JSON.stringify(user));
    setToken(token);
};

export const handleUpdateUser = (patch, setUser) => {
    setUser((cur) => {
        if (!cur) return cur;
        const next = { ...cur, ...patch };
        localStorage.setItem(SAVED_USER_KEY, JSON.stringify(next));
        return next;
    });
};

export const handleLogout = (setUser) => {
    const token = getToken();
    if (token) {
        apiRequest('/api/auth/logout', { method: 'POST' }).catch(() => {});
    }
    clearSession();
    setUser(null);
};

export function setupTokenTimer() {
    const token = getToken();
    if (!token) return undefined;

    const remain = getTokenExp(token) - Date.now();
    if (remain <= 0) {
        clearSession();
        return undefined;
    }
    const timer = setTimeout(expireSession, Math.min(remain, 2 ** 31 - 1));
    return () => clearTimeout(timer);
}
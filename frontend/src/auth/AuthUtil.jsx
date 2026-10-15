import { logout } from '@/api/index.js';
import { getToken, setToken, removeToken, getTokenExp } from "./Token.jsx"

const SAVED_USER_KEY = 'eats-a-deal-user';

function withoutToken(user) {
    if (!user || typeof user !== 'object')
        return user;
    const profile = { ...user };
    delete profile.token;
    return profile;
}

export function getSavedUser() {
    try {
        const savedUser = JSON.parse(localStorage.getItem(SAVED_USER_KEY));
        const token = getToken();
        if (savedUser && token && getTokenExp(token) > Date.now()) {
            const profile = withoutToken(savedUser);
            localStorage.setItem(SAVED_USER_KEY, JSON.stringify(profile));
            return profile;
        }
    } catch {/* */ }
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

export function handleLogin(userWithToken) {
    const { token, ...profile } = userWithToken;
    if (!token)
        throw new Error('로그인 응답에 인증 토큰이 없습니다.');
    localStorage.setItem(SAVED_USER_KEY, JSON.stringify(profile));
    setToken(token);
    return profile;
}

export function handleUpdateUser(patch, setUser) {
    setUser((current) => {
        if (!current) return current;
        const next = withoutToken({ ...current, ...patch });
        localStorage.setItem(SAVED_USER_KEY, JSON.stringify(next));
        return next;
    });
}

export function handleLogout(setUser) {
    if (getToken()) {
        logout().catch(() => {});
    }
    clearSession();
    setUser(null);
}

export function setupTokenTimer() {
    const token = getToken();
    if (!token)
        return undefined;

    const remain = getTokenExp(token) - Date.now();
    if (remain <= 0) {
        expireSession();
        return undefined;
    }
    const timer = setTimeout(expireSession, Math.min(remain, 2 ** 31 - 1));
    return () => clearTimeout(timer);
}
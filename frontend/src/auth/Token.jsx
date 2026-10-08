const TOKEN_KEY = 'eats-a-deal-token';

export function getToken() {
    return localStorage.getItem(TOKEN_KEY);
}

export function setToken(token) {
    localStorage.setItem(TOKEN_KEY, token);
}

export function removeToken(){
    localStorage.removeItem(TOKEN_KEY);
}

export function getTokenExp(token) {
    try {
        const b64 = token.split('.')[1].replace(/-/g, '+').replace(/_/g, '/');
        return JSON.parse(atob(b64)).exp * 1000;
    } catch {
        return 0;
    }
}

export function isTokenExpired(token) {
    return !token || getTokenExp(token) <= Date.now();
}
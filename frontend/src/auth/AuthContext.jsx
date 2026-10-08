import { createContext, useCallback, useContext, useMemo, useState, useEffect } from 'react';
import { getSavedUser, handleLogin, handleLogout, handleUpdateUser, setupTokenTimer, clearSession } from './AuthUtil.jsx'

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(getSavedUser);

    const login = useCallback((u) => {
        handleLogin(u);
        setUser(u);
    }, []);
    const updateUser = useCallback((patch) => handleUpdateUser(patch, setUser), []);
    const logout = useCallback(() => handleLogout(setUser), []);

    useEffect(() => {
        const onExpired = () => {
            clearSession();
            setUser(null);
        }
        window.addEventListener('auth:expired', onExpired);
        return () => window.removeEventListener('auth:expired', onExpired);
    }, []);
    useEffect(() => setupTokenTimer(), [user?.token]);

    const value = useMemo(() => ({ user, login, logout, updateUser }), [user, login, logout, updateUser]);
    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
import { createContext, useCallback, useContext, useMemo, useState, useEffect } from 'react';
import { getSavedUser, handleLogin, handleLogout, handleUpdateUser, setupTokenTimer, clearSession } from './AuthUtil.jsx'
import { useLocation } from 'react-router';

const AuthContext = createContext(null);

export function AuthProvider({ children }) {
    const [user, setUser] = useState(getSavedUser);
    const [loggedOut, setLoggedOut] = useState(false);
    const location = useLocation();

    const login = useCallback((u) => {
        setLoggedOut(false);
        handleLogin(u);
        setUser(u);
        }, []);
    const logout = useCallback(() => {
        setLoggedOut(true);
        handleLogout(setUser);
        }, []);
    const updateUser = useCallback((patch) => handleUpdateUser(patch, setUser), []);

    useEffect(() => {
        const onExpired = () => {
            clearSession();
            setUser(null);
        }
        window.addEventListener('auth:expired', onExpired);
        return () => window.removeEventListener('auth:expired', onExpired);
    }, []);
    useEffect(() => setupTokenTimer(), [user?.token]);
    useEffect(() => { setLoggedOut(false); }, [location.pathname]);

    const value = useMemo(() => ({ user, loggedOut, login, logout, updateUser }), [user, loggedOut, login, logout, updateUser]);
    return <AuthContext.Provider value={value}>{children}</AuthContext.Provider>;
}

export const useAuth = () => useContext(AuthContext);
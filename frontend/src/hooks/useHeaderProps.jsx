import { useLocation, useNavigate } from 'react-router';
import { useAuth } from '../auth/AuthContext.jsx';

export function useHeaderProps() {
    const navigate = useNavigate();
    const location = useLocation();
    const { user, logout } = useAuth();
    return {
        user,
        onLoginClick: () => navigate('/login', { state: { from: location.pathname + location.search } }),
        onLogout: logout,
        onOpenMyPage: () => navigate('/mypage'),
        onOpenFavorites: () => navigate('/favorites'),
        onOpenAdminPage: () => navigate('/admin'),
    };
}
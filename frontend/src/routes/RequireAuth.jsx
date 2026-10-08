import { Navigate, Outlet, useLocation } from 'react-router';
import { useAuth } from '../auth/AuthContext.jsx';

export default function RequireAuth() {
    const { user, loggedOut } = useAuth();
    const location = useLocation();

    if (!user) {
        return loggedOut
            ? <Navigate to="/" replace />
            : <Navigate to="/login" replace state={{ from: location.pathname + location.search }} />;
    }
    return <Outlet />;
}
import { useLocation, useNavigate } from 'react-router';

export function useGoLogin() {
    const navigate = useNavigate();
    const location = useLocation();
    return () => navigate('/login', { state: { from: location.pathname + location.search } });
}
import { useLocation, useNavigate } from 'react-router';

export function useGoBack(fallback = '/') {
    const navigate = useNavigate();
    const location = useLocation();
    return () => {
        if (location.key !== 'default')
            navigate(-1);
        else
            navigate(fallback, { replace: true });
    };
}
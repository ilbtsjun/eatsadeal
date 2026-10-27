import { useLocation, useNavigate } from 'react-router';
import LoginPage from '../pages/LoginPage.jsx';
import { useAuth } from '../auth/useAuth.js';
import { useGoBack } from '@/common/hooks/useGoBack.js';
import { usePageTitle } from '@/common/hooks/usePageTitle.js';

export default function LoginRoute() {
    const navigate = useNavigate();
    const location = useLocation();
    const from = location.state?.from ?? '/';
    const { login } = useAuth();
    const goBack = useGoBack('/');
    usePageTitle('로그인');

    return (
        <LoginPage
            onLogin={(u) => {
                login(u);
                navigate(from, { replace: true });
            }}
            onBack={goBack}
            onSignupClick={() => navigate('/signup', { replace: true })}
        />
    );
}
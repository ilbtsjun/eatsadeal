import { useNavigate } from 'react-router';
import LoginPage from '../pages/LoginPage.jsx';
import { useAuth } from '../auth/AuthContext.jsx';
import { useGoBack } from '../hooks/useGoBack.jsx';
import { usePageTitle } from '../hooks/usePageTitle.jsx';

export default function LoginRoute() {
    const navigate = useNavigate();
    const { login } = useAuth();
    const goBack = useGoBack('/');
    usePageTitle('로그인');

    return (
        <LoginPage
            onLogin={(u) => {
                login(u);
                navigate('/', { replace: true });
            }}
            onBack={goBack}
            onSignupClick={() => navigate('/signup', { replace: true })}
        />
    );
}
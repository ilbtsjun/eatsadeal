import { useLocation, useNavigate } from 'react-router';
import LoginPage from '../pages/LoginPage.jsx';
import { useAuth } from '../auth/useAuth.js';
import { useGoBack } from '@/common/hooks/useGoBack.js';
import { usePageTitle } from '@/common/hooks/usePageTitle.js';

export default function LoginRoute() {
    const navigate = useNavigate();
    const location = useLocation();
    const from = location.state?.from ?? '/';
    const passwordReset = location.state?.passwordReset;
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
            notice={passwordReset ? '비밀번호가 변경되었습니다. 새 비밀번호로 로그인해주세요.' : ''}
            onPasswordResetClick={() => navigate('/password-reset', { replace: true })}
        />
    );
}
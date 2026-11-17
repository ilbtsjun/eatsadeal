import { useNavigate } from 'react-router';
import PasswordResetPage from '../pages/PasswordResetPage.jsx';
import { useGoBack } from '@/common/hooks/useGoBack.js';
import { usePageTitle } from '@/common/hooks/usePageTitle.js';

export default function PasswordResetRoute() {
    const navigate = useNavigate();
    const goBack = useGoBack('/');
    usePageTitle('비밀번호 재설정');

    return (
        <PasswordResetPage
            onBack={goBack}
            onLoginClick={() => navigate('/login', { replace: true })}
            onResetSuccess={() => navigate('/login', { replace: true, state: { passwordReset: true } })}
        />
    );
}

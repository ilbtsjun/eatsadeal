import { Outlet, useNavigate } from 'react-router';
import SignupPage from '../pages/SignupPage.jsx';
import { useGoBack } from '../hooks/useGoBack.jsx';
import { usePageTitle } from '../hooks/usePageTitle.jsx';

export default function SignupRoute() {
    const navigate = useNavigate();
    const goBack = useGoBack('/');
    usePageTitle('회원가입');

    return (
        <>
            <SignupPage
                onBack={goBack}
                onLoginClick={() => navigate('/login', { replace: true })}
                onSignupSuccess={() => navigate('/login', { replace: true })}
                onOpenTerms={() => navigate('terms')}
                onOpenPrivacy={() => navigate('privacy')}
            />
            <Outlet />
        </>
    );
}
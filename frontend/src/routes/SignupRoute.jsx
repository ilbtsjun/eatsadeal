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
            {/* 하위 라우트(약관)가 바뀌어도 이 요소는 unmount되지 않아 입력값과 체크 상태가 유지됨 */}
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
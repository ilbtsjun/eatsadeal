import { useNavigate } from 'react-router';
import TermsPage from '../pages/TermsPage.jsx';
import PrivacyPage from '../pages/PrivacyPage.jsx';
import { useGoBack } from '../hooks/useGoBack.jsx';
import { usePageTitle } from '../hooks/usePageTitle.jsx';

export default function LegalOverlay({ kind }) {
    const navigate = useNavigate();
    const goBack = useGoBack('/signup');
    usePageTitle(kind === 'terms' ? '이용약관' : '개인정보처리방침');

    return (
        <div key={kind} className="legal-overlay">
            {kind === 'terms'
                ? <TermsPage onBack={goBack} backLabel="회원가입으로"
                             onPrivacy={() => navigate('/signup/privacy', { replace: true })} />
                : <PrivacyPage onBack={goBack} backLabel="회원가입으로"
                               onTerms={() => navigate('/signup/terms', { replace: true })} />}
        </div>
    );
}
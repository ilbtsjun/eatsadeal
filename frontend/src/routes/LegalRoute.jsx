import { useNavigate } from 'react-router';
import TermsPage from '../pages/TermsPage.jsx';
import PrivacyPage from '../pages/PrivacyPage.jsx';
import { useGoBack } from '@/common/hooks/useGoBack.js';
import { usePageTitle } from '@/common/hooks/usePageTitle.js';

export default function LegalRoute({ kind }) {
    const navigate = useNavigate();
    const goBack = useGoBack('/');
    usePageTitle(kind === 'terms' ? '이용약관' : '개인정보처리방침');
    return kind === 'terms'
        ? <TermsPage onBack={goBack}
                     onPrivacy={() => navigate('/privacy', { replace: true })} />
        : <PrivacyPage onBack={goBack}
                       onTerms={() => navigate('/terms', { replace: true })} />;
}
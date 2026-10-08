import { useNavigate } from 'react-router';
import MainPage from '../pages/MainPage.jsx';
import { useAuth} from '../auth/AuthContext.jsx';
import { usePageTitle } from '../hooks/usePageTitle.jsx';

export default function MainRoute() {
    const navigate = useNavigate();
    const { user } = useAuth();
    usePageTitle();
    return <MainPage user = {user}
                     onSelectEvent={(event) => navigate(`/events/${event.id}`, { state: { event } })}
                     onOpenTerms={() => navigate('/terms')}
                     onOpenPrivacy={() => navigate('/privacy')} />;
}
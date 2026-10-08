import { useNavigate } from 'react-router';
import MainPage from '../pages/MainPage.jsx';
import { useHeaderProps } from '../hooks/useHeaderProps.jsx';
import { usePageTitle } from '../hooks/usePageTitle.jsx';

export default function MainRoute() {
    const navigate = useNavigate();
    const header = useHeaderProps();
    usePageTitle();
    return <MainPage {...header}
                     onSelectEvent={(event) => navigate(`/events/${event.id}`, { state: { event } })}
                     onOpenTerms={() => navigate('/terms')}
                     onOpenPrivacy={() => navigate('/privacy')} />;
}
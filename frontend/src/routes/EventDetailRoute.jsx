import { Navigate, useLocation, useNavigate, useParams } from 'react-router';
import EventDetailPage from '../pages/EventDetailPage.jsx';
import { useAuth } from '../auth/AuthContext.jsx';
import { useGoBack } from '../hooks/useGoBack.jsx';
import { useHeaderProps } from '../hooks/useHeaderProps.jsx'

export default function EventDetailRoute() {
    const { eventId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const { user, logout } = useAuth();
    const goBack = useGoBack('/');
    const header = useHeaderProps();

    const id = Number(eventId);
    if (!Number.isInteger(id))
        return <Navigate to="/" replace />;

    const passed = location.state?.event;
    const event = passed && Number(passed.id) === id ? passed : { id };

    return (
        <EventDetailPage
            key={id}
            event={event}
            {...header}
            onBack={goBack}
        />
    );
}
import { Navigate, useLocation, useNavigate, useParams } from 'react-router';
import EventDetailPage from '../pages/EventDetailPage.jsx';
import { useAuth } from '../auth/AuthContext.jsx';
import { useGoBack } from '../hooks/useGoBack.jsx';

export default function EventDetailRoute() {
    const { eventId } = useParams();
    const location = useLocation();
    const navigate = useNavigate();
    const { user, logout } = useAuth();
    const goBack = useGoBack('/');

    const id = Number(eventId);
    if (!Number.isInteger(id))
        return <Navigate to="/" replace />;

    const passed = location.state?.event;
    const event = passed && Number(passed.id) === id ? passed : { id };

    return (
        <EventDetailPage
            key={id}
            event={event}
            user={user}
            onLoginClick={() => navigate('/login', { state: { from: location.pathname } })}
            onLogout={logout}
            onBack={goBack}
            onOpenMyPage={() => navigate('/', { state: { view: 'mypage' } })}
            onOpenFavorites={() => navigate('/', { state: { view: 'favorites' } })}
            onOpenAdminPage={() => navigate('/', { state: { view: 'admin' } })}
        />
    );
}
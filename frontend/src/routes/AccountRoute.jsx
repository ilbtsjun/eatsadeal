import { useNavigate } from 'react-router';
import MyPage from '../pages/MyPage.jsx';
import FavoritesPage from '../pages/FavoritesPage.jsx';
import AdminPage from '../pages/AdminPage.jsx';
import { useAuth } from '../auth/AuthContext.jsx';
import { useHeaderProps } from '../hooks/useHeaderProps.jsx';
import { usePageTitle } from '../hooks/usePageTitle.jsx';

const openEvent = (navigate) => (event) => navigate(`/events/${event.id}`, { state: { event } });

export function MyPageRoute() {
    const navigate = useNavigate();
    const header = useHeaderProps();
    const { updateUser } = useAuth();
    usePageTitle('마이페이지');
    return <MyPage {...header}
                   onUserUpdate={updateUser}
                   onBack={() => navigate('/')}
                   onOpenEvent={openEvent(navigate)} />;
}

export function FavoritesRoute() {
    const navigate = useNavigate();
    const header = useHeaderProps();
    usePageTitle('즐겨찾기');
    return <FavoritesPage {...header}
                          onBack={() => navigate('/')}
                          onOpenEvent={openEvent(navigate)} />;
}

export function AdminRoute() {
    const navigate = useNavigate();
    const header = useHeaderProps();
    usePageTitle('관리자 페이지');
    return <AdminPage {...header}
                      onBack={() => navigate('/')} />;
}
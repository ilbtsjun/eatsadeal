import { useNavigate } from 'react-router';
import MyPage from '../pages/MyPage.jsx';
import FavoritesPage from '../pages/FavoritesPage.jsx';
import AdminPage from '../pages/AdminPage.jsx';
import { usePageTitle } from '../hooks/usePageTitle.jsx';
import { useAuth } from '../auth/AuthContext.jsx'

const openEvent = (navigate) => (event) => navigate(`/events/${event.id}`, { state: { event } });

export function MyPageRoute() {
    const navigate = useNavigate();
    const { user, logout, updateUser } = useAuth();
    usePageTitle('마이페이지');
    return <MyPage user={user} onLogout={logout} onUserUpdate={updateUser}
                   onBack={() => navigate('/')} onOpenEvent={openEvent(navigate)} />;
}

export function FavoritesRoute() {
    const navigate = useNavigate();
    const { user } = useAuth();
    usePageTitle('즐겨찾기');
    return <FavoritesPage user={user}
                          onBack={() => navigate('/')}
                          onOpenEvent={openEvent(navigate)} />;
    return <AdminPage user={user} onBack={() => navigate('/')} />
}

export function AdminRoute() {
    const navigate = useNavigate();
    const { user } = useAuth();
    usePageTitle('관리자 페이지');
    return <AdminPage user={user}
                      onBack={() => navigate('/')} />

}
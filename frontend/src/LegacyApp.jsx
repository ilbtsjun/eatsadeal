import { useEffect, useState } from 'react';
import { useAuth } from './auth/AuthContext.jsx';
import MainPage from './pages/MainPage.jsx';
import AdminPage from './pages/AdminPage.jsx';
import MyPage from './pages/MyPage.jsx';
import FavoritesPage from './pages/FavoritesPage.jsx';
import TermsPage from './pages/TermsPage.jsx';
import PrivacyPage from './pages/PrivacyPage.jsx';
import { useLocation, useNavigate } from 'react-router'

function LegacyApp(){
    const navigate = useNavigate();
    const initialView = useLocation().state?.view;
    const { user, updateUser, logout, } = useAuth();
    const [showAdminPage, setShowAdminPage] = useState(initialView === 'admin');
    const [showMyPage, setShowMyPage] = useState(initialView === 'mypage');
    const [showFavorites, setShowFavorites] = useState(initialView === 'favorites');
    const [myPageTarget, setMyPageTarget] = useState('profile');
    const [legalPage, setLegalPage] = useState(null);

    const applyView = (view, event = null) => {
        setShowMyPage(view === 'mypage');
        setShowFavorites(view === 'favorites');
        setShowAdminPage(view === 'admin');
        if (view !== 'terms' && view !== 'privacy') setLegalPage(null);
    };
    const openLegalPage = (page) => {
        window.history.pushState({ view: page }, '', window.location.href);
        setLegalPage(page);
    };
    const closeLegalPage = () => {
        window.history.pushState({ view: 'home' }, '', window.location.href);
        setLegalPage(null);
    };
    const navigateTo = (view, action) => {
        window.history.pushState({ view }, '', window.location.href);
        action();
    };

    useEffect(() => {
        window.history.replaceState({ ...window.history.state, view: 'home'}, '', window.location.href);
        const handlePopState = (event) => {
            const view = event.state?.view || 'home';
            if (view === 'terms' || view === 'privacy') setLegalPage(view);
            else applyView(view, event.state?.event || null);
        };
        window.addEventListener('popstate', handlePopState);
        return () => window.removeEventListener('popstate', handlePopState);
    }, []);

    useEffect(() => {
        let pageTitle = 'Eats a Deal'; // 기본 타이틀

        if (legalPage === 'terms') {
            pageTitle = '이용약관 - Eats a Deal';
        } else if (legalPage === 'privacy') {
            pageTitle = '개인정보처리방침 - Eats a Deal';
        } else if (showMyPage) {
            pageTitle = '마이페이지 - Eats a Deal';
        } else if (showFavorites) {
            pageTitle = '즐겨찾기 - Eats a Deal';
        } else if (showAdminPage) {
            pageTitle = '관리자 페이지 - Eats a Deal';
            document.title = pageTitle;
        }
    }, [legalPage, showMyPage, showFavorites, showAdminPage]);

    const openMyPage = (target = 'profile') => navigateTo('mypage', () => {
        setShowMyPage(true);
        setMyPageTarget(target);
    });
    const openFavorites = () => navigateTo('favorites', () => {
        setShowMyPage(false);
        setShowAdminPage(false);
        setShowFavorites(true);
    });

    const handleUserUpdate = updateUser;
    const handleLogout = logout;

    const legalContent = legalPage === 'terms'
        ? <TermsPage onBack={closeLegalPage} onPrivacy={() => openLegalPage('privacy')} />
        : legalPage === 'privacy'
            ? <PrivacyPage onBack={closeLegalPage} onTerms={() => openLegalPage('terms')} />
            : null;

    if (legalContent) return legalContent;

    if (showMyPage) {
        return <MyPage user={user}
                       onUserUpdate={handleUserUpdate}
                       initialSection={myPageTarget}
                       onLoginClick={() => navigate('/login')}
                       onLogout={handleLogout}
                       onBack={() => navigateTo('home', () => setShowMyPage(false))}
                       onOpenMyPage={() => openMyPage('profile')}
                       onOpenFavorites={openFavorites}
                       onOpenAdminPage={() => navigateTo('admin', () => {
                           setShowMyPage(false);
                           setShowAdminPage(true);
                       })}
                       onOpenEvent={(event) => navigate(`/events/${event.id}`, { state: { event } })}
        />;
    }

    if (showFavorites) {
        return <FavoritesPage user={user}
                              onLoginClick={() => navigate('/login')}
                              onLogout={handleLogout}
                              onBack={() => navigateTo('home', () => setShowFavorites(false))}
                              onOpenEvent={(event) => navigate(`/events/${event.id}`, { state: { event } })}
                              onOpenMyPage={() => openMyPage('profile')}
                              onOpenFavorites={openFavorites}
                              onOpenAdminPage={() => navigateTo('admin', () => {
                                  setShowFavorites(false);
                                  setShowAdminPage(true);
                              })} />;
    }

    if (showAdminPage) {
        return <AdminPage user={user}
                          onLoginClick={() => navigate('/login')}
                          onLogout={handleLogout}
                          onBack={() => navigateTo('home', () => setShowAdminPage(false))}
                          onOpenMyPage={() => openMyPage('profile')}
                          onOpenFavorites={openFavorites} />;
    }

    return (
        <MainPage
            user={user}
            onLoginClick={() => navigate('/login')}
            onLogout={handleLogout}
            onOpenMyPage={() => openMyPage('profile')}
            onOpenFavorites={openFavorites}
            onOpenAdminPage={() => navigateTo('admin', () => setShowAdminPage(true))}
            onSelectEvent={(event) => navigate(`/events/${event.id}`, { state: { event } })}
            onOpenTerms={() => openLegalPage('terms')}
            onOpenPrivacy={() => openLegalPage('privacy')}
        />
    );
}

export default LegacyApp;
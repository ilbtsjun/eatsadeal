import { useEffect, useState } from 'react';
import { useAuth } from './auth/AuthContext.jsx';
import MainPage from './pages/MainPage.jsx';
import EventDetailPage from './pages/EventDetailPage.jsx';
import AdminPage from './pages/AdminPage.jsx';
import MyPage from './pages/MyPage.jsx';
import FavoritesPage from './pages/FavoritesPage.jsx';
import TermsPage from './pages/TermsPage.jsx';
import PrivacyPage from './pages/PrivacyPage.jsx';
import { useNavigate } from 'react-router'

function LegacyApp(){
    const navigate = useNavigate();
    const { user, updateUser, logout, } = useAuth();
    const [selectedEvent, setSelectedEvent] = useState(null);
    const [showAdminPage, setShowAdminPage] = useState(false);
    const [showMyPage, setShowMyPage] = useState(false);
    const [showFavorites, setShowFavorites] = useState(false);
    const [myPageTarget, setMyPageTarget] = useState('profile');
    const [searchKeyword, setSearchKeyword] = useState('');
    const [legalPage, setLegalPage] = useState(null);

    const applyView = (view, event = null, nextSearchKeyword = '') => {
        setShowMyPage(view === 'mypage');
        setShowFavorites(view === 'favorites');
        setShowAdminPage(view === 'admin');
        if (view === 'home') setSearchKeyword(nextSearchKeyword || '');
        if (view === 'event' && event) setSelectedEvent(event);
        if (view !== 'event') setSelectedEvent(null);
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
        window.history.replaceState({ ...window.history.state, view: 'home', searchKeyword: '' }, '', window.location.href);
        const handlePopState = (event) => {
            const view = event.state?.view || 'home';
            if (view === 'terms' || view === 'privacy') setLegalPage(view);
            else applyView(view, event.state?.event || null, event.state?.searchKeyword || '');
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
        } else if (selectedEvent) {
            pageTitle = `${selectedEvent.title || '상세 정보'} - Eats a Deal`; // 이벤트 제목이 있다면 활용 가능
        }
        document.title = pageTitle;
    }, [legalPage, showMyPage, showFavorites, showAdminPage, selectedEvent]);

    const openMyPage = (target = 'profile') => navigateTo('mypage', () => {
        setShowMyPage(true);
        setMyPageTarget(target);
    });
    const openFavorites = () => navigateTo('favorites', () => {
        setShowMyPage(false);
        setShowAdminPage(false);
        setSelectedEvent(null);
        setShowFavorites(true);
    });

    const handleSearch = (keyword) => {
        const nextKeyword = keyword.trim();
        if (nextKeyword === searchKeyword) return;
        window.history.pushState({ view: 'home', searchKeyword: nextKeyword }, '', window.location.href);
        setSearchKeyword(nextKeyword);
    };

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
                       onOpenEvent={(event) => {
                           window.history.pushState({ view: 'event', event }, '', window.location.href);
                           setShowMyPage(false);
                           setSelectedEvent(event);
                       }}
        />;
    }

    if (showFavorites) {
        return <FavoritesPage user={user}
                              onLoginClick={() => navigate('/login')}
                              onLogout={handleLogout}
                              onBack={() => navigateTo('home', () => setShowFavorites(false))}
                              onOpenEvent={(event) => {
                                  window.history.pushState({ view: 'event', event }, '', window.location.href);
                                  setShowFavorites(false); setSelectedEvent(event); }}
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

    if (selectedEvent) {
        return (
            <EventDetailPage
                event={selectedEvent}
                user={user}
                onLoginClick={() => navigate('/login')}
                onLogout={handleLogout}
                onBack={() => navigateTo('home', () => setSelectedEvent(null))}
                onOpenMyPage={() => openMyPage('profile')}
                onOpenFavorites={openFavorites}
                onOpenAdminPage={() => navigateTo('admin', () => { setSelectedEvent(null); setShowAdminPage(true); })}
            />
        );
    }
    return (
        <MainPage
            user={user}
            searchKeyword={searchKeyword}
            onSearch={handleSearch}
            onLoginClick={() => navigate('/login')}
            onLogout={handleLogout}
            onOpenMyPage={() => openMyPage('profile')}
            onOpenFavorites={openFavorites}
            onOpenAdminPage={() => navigateTo('admin', () => setShowAdminPage(true))}

            onSelectEvent={(event) => {
                window.history.pushState({ view: 'event', event }, '', window.location.href);
                setSelectedEvent(event);
            }}
            onOpenTerms={() => openLegalPage('terms')}
            onOpenPrivacy={() => openLegalPage('privacy')}
        />
    );
}

export default LegacyApp;
import { useEffect, useState } from 'react';
import { apiRequest } from './api/client';
import MainPage from './pages/MainPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import EventDetailPage from './pages/EventDetailPage.jsx';
import SignupPage from './pages/SignupPage.jsx';
import AdminPage from './pages/AdminPage.jsx';
import MyPage from './pages/MyPage.jsx';
import FavoritesPage from './pages/FavoritesPage.jsx';
import TermsPage from './pages/TermsPage.jsx';
import PrivacyPage from './pages/PrivacyPage.jsx';

const SAVED_USER_KEY = 'eats-a-deal-user';
const TOKEN_KEY = 'eats-a-deal-token';

function getSavedUser() {
  try { return JSON.parse(localStorage.getItem(SAVED_USER_KEY)) || null; } catch { return null; }
}

function App() {
  const [user, setUser] = useState(getSavedUser);
  const [showLogin, setShowLogin] = useState(false);
  const [showSignup, setShowSignup] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState(null);
  const [showAdminPage, setShowAdminPage] = useState(false);
  const [showMyPage, setShowMyPage] = useState(false);
  const [showFavorites, setShowFavorites] = useState(false);
  const [myPageTarget, setMyPageTarget] = useState('profile');
  const [searchKeyword, setSearchKeyword] = useState('');
  const [legalPage, setLegalPage] = useState(null);

  const applyView = (view, event = null, nextSearchKeyword = '') => {
    setShowLogin(view === 'login');
    setShowSignup(view === 'signup');
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
    window.history.replaceState({ view: 'home', searchKeyword: '' }, '', window.location.href);
    const handlePopState = (event) => {
      const view = event.state?.view || 'home';
      if (view === 'terms' || view === 'privacy') setLegalPage(view);
      else applyView(view, event.state?.event || null, event.state?.searchKeyword || '');
    };
    window.addEventListener('popstate', handlePopState);
    return () => window.removeEventListener('popstate', handlePopState);
  }, []);
  const openMyPage = (target = 'profile') => navigateTo('mypage', () => {
    setShowMyPage(true);
    setMyPageTarget(target);
  });
  const openFavorites = () => navigateTo('favorites', () => {
    setShowLogin(false);
    setShowSignup(false);
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

  const handleLogin = (loggedInUser) => {
    localStorage.setItem(SAVED_USER_KEY, JSON.stringify(loggedInUser));
    localStorage.setItem(TOKEN_KEY, loggedInUser.token);
    setUser(loggedInUser);
    navigateTo('home', () => setShowLogin(false));
  };
  const handleLogout = () => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) apiRequest('/api/auth/logout', { method: 'POST' }).catch(() => {});
    localStorage.removeItem(SAVED_USER_KEY);
    localStorage.removeItem(TOKEN_KEY);
    setUser(null);
  };
  if (legalPage === 'terms') return <TermsPage onBack={closeLegalPage} onPrivacy={() => openLegalPage('privacy')} />;
  if (legalPage === 'privacy') return <PrivacyPage onBack={closeLegalPage} onTerms={() => openLegalPage('terms')} />;
  if (showLogin) return <LoginPage onLogin={handleLogin} onBack={() => navigateTo('home', () => setShowLogin(false))} onSignupClick={() => navigateTo('signup', () => { setShowLogin(false); setShowSignup(true); })} />;

  if (showSignup) {
    return (
        <SignupPage
            onBack={() => navigateTo('home', () => setShowSignup(false))}
            onLoginClick={() => navigateTo('login', () => { setShowSignup(false); setShowLogin(true); })}
            onSignupSuccess={() => { setShowSignup(false); setShowLogin(true); }}
        />
    );
  }

  if (showMyPage) {
    return <MyPage user={user} initialSection={myPageTarget} onLoginClick={() => navigateTo('login', () => setShowLogin(true))} onLogout={handleLogout} onBack={() => navigateTo('home', () => setShowMyPage(false))} onOpenMyPage={() => openMyPage('profile')} onOpenFavorites={openFavorites} onOpenAdminPage={() => navigateTo('admin', () => { setShowMyPage(false); setShowAdminPage(true); })} onOpenEvent={(event) => { window.history.pushState({ view: 'event', event }, '', window.location.href); setShowMyPage(false); setSelectedEvent(event); }} />;
  }

  if (showFavorites) {
    return <FavoritesPage user={user} onLoginClick={() => navigateTo('login', () => setShowLogin(true))} onLogout={handleLogout} onBack={() => navigateTo('home', () => setShowFavorites(false))} onOpenEvent={(event) => { window.history.pushState({ view: 'event', event }, '', window.location.href); setShowFavorites(false); setSelectedEvent(event); }} onOpenMyPage={() => openMyPage('profile')} onOpenFavorites={openFavorites} onOpenAdminPage={() => navigateTo('admin', () => { setShowFavorites(false); setShowAdminPage(true); })} />;
  }

  if (showAdminPage) {
    return <AdminPage user={user} onLoginClick={() => navigateTo('login', () => setShowLogin(true))} onLogout={handleLogout} onBack={() => navigateTo('home', () => setShowAdminPage(false))} onOpenMyPage={() => openMyPage('profile')} onOpenFavorites={openFavorites} />;
  }

  if (selectedEvent) {
    return (
        <EventDetailPage
            event={selectedEvent}
            user={user}
            onLoginClick={() => navigateTo('login', () => setShowLogin(true))}
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
          onLoginClick={() => navigateTo('login', () => setShowLogin(true))}
          onLogout={handleLogout}
          onSelectEvent={(event) => { window.history.pushState({ view: 'event', event }, '', window.location.href); setSelectedEvent(event); }}
          onOpenAdminPage={() => navigateTo('admin', () => setShowAdminPage(true))}
          onOpenMyPage={() => openMyPage('profile')}
          searchKeyword={searchKeyword}
          onSearch={handleSearch}
          onOpenFavorites={openFavorites}
          onOpenTerms={() => openLegalPage('terms')}
          onOpenPrivacy={() => openLegalPage('privacy')}
      />
  );
}
export default App;

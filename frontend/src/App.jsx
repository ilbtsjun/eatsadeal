import { useState } from 'react';
import MainPage from './pages/MainPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import EventDetailPage from './pages/EventDetailPage.jsx';
import SignupPage from './pages/SignupPage.jsx';
import AdminPage from './pages/AdminPage.jsx';
import MyPage from './pages/MyPage.jsx';

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
  const handleLogin = (loggedInUser) => {
    localStorage.setItem(SAVED_USER_KEY, JSON.stringify(loggedInUser));
    localStorage.setItem(TOKEN_KEY, loggedInUser.token);
    setUser(loggedInUser);
    setShowLogin(false);
  };
  const handleLogout = () => {
    const token = localStorage.getItem(TOKEN_KEY);
    if (token) fetch('/user/auth/logout', { method: 'POST', headers: { token } }).catch(() => {});
    localStorage.removeItem(SAVED_USER_KEY);
    localStorage.removeItem(TOKEN_KEY);
    setUser(null);
  };
  if (showLogin) return <LoginPage onLogin={handleLogin} onBack={() => setShowLogin(false)} onSignupClick={() => { setShowLogin(false); setShowSignup(true); }} />;

  if (showSignup) {
    return (
      <SignupPage
        onBack={() => setShowSignup(false)}
        onLoginClick={() => { setShowSignup(false); setShowLogin(true); }}
        onSignupSuccess={() => { setShowSignup(false); setShowLogin(true); }}
      />
    );
  }

  if (showMyPage) {
    return <MyPage user={user} onLoginClick={() => setShowLogin(true)} onLogout={handleLogout} onBack={() => setShowMyPage(false)} />;
  }

  if (showAdminPage) {
    return <AdminPage user={user} onLoginClick={() => setShowLogin(true)} onLogout={handleLogout} onBack={() => setShowAdminPage(false)} onOpenMyPage={() => { setShowAdminPage(false); setShowMyPage(true); }} />;
  }

  if (selectedEvent) {
    return (
      <EventDetailPage
        event={selectedEvent}
        user={user}
        onLoginClick={() => setShowLogin(true)}
        onLogout={handleLogout}
        onBack={() => setSelectedEvent(null)}
      />
    );
  }

  return (
    <MainPage
      user={user}
      onLoginClick={() => setShowLogin(true)}
      onLogout={handleLogout}
      onSelectEvent={setSelectedEvent}
      onOpenAdminPage={() => setShowAdminPage(true)}
      onOpenMyPage={() => setShowMyPage(true)}
    />
  );
}
export default App;

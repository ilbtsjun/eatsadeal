import { useState } from 'react';
import MainPage from './pages/MainPage.jsx';
import LoginPage from './pages/LoginPage.jsx';
import EventDetailPage from './pages/EventDetailPage.jsx';

const SAVED_USER_KEY = 'eats-a-deal-user';

function getSavedUser() {
  try { return JSON.parse(localStorage.getItem(SAVED_USER_KEY)) || null; } catch { return null; }
}

function App() {
  const [user, setUser] = useState(getSavedUser);
  const [showLogin, setShowLogin] = useState(false);
  const [selectedEvent, setSelectedEvent] = useState(null);
  const handleLogin = (loggedInUser) => { localStorage.setItem(SAVED_USER_KEY, JSON.stringify(loggedInUser)); setUser(loggedInUser); setShowLogin(false); };
  const handleLogout = () => { localStorage.removeItem(SAVED_USER_KEY); setUser(null); };
  if (showLogin) return <LoginPage onLogin={handleLogin} onBack={() => setShowLogin(false)} />;

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
    />
  );
}
export default App;

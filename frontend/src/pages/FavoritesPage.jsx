import { useEffect, useState } from 'react';
import Header from '../components/Header';
import { apiRequest } from '../api/client';
import './FavoritesPage.css';

export default function FavoritesPage({ user, onLoginClick, onLogout, onBack, onOpenEvent, onOpenMyPage, onOpenFavorites, onOpenAdminPage }) {
  const [favorites, setFavorites] = useState([]);
  const [loading, setLoading] = useState(true);
  const [error, setError] = useState('');

  useEffect(() => {
    let cancelled = false;
    setLoading(true);
    apiRequest('/api/user/me/favorites')
      .then(async (data) => {
        const rawFavorites = Array.isArray(data) ? data : data?.favorites || data?.content || [];
        const ids = rawFavorites
          .map((item) => (typeof item === 'object' ? item.id || item.eventId : item))
          .filter(Boolean);
        const details = await Promise.allSettled(ids.map((id) => apiRequest(`/api/events/${id}`)));
        if (!cancelled) setFavorites(details.filter((result) => result.status === 'fulfilled').map((result) => result.value));
      })
      .catch((requestError) => { if (!cancelled) setError(requestError.message); })
      .finally(() => { if (!cancelled) setLoading(false); });
    return () => { cancelled = true; };
  }, [user]);

  return (
    <div className="favorites-page">
      <Header
        user={user}
        onLoginClick={onLoginClick}
        onLogout={onLogout}
        onOpenMyPage={onOpenMyPage}
        onOpenFavorites={onOpenFavorites}
        onOpenAdminPage={onOpenAdminPage}
      />
      <main className="favorites-container">
        <button type="button" className="favorites-back" onClick={onBack}>← 메인으로</button>
        <h1>찜한 목록</h1>
        <p className="favorites-intro">찜한 이벤트를 모아볼 수 있습니다.</p>
        {loading && <p className="favorites-message">찜한 목록을 불러오는 중입니다...</p>}
        {!loading && error && <p className="favorites-error">오류가 발생했습니다: {error}</p>}
        {!loading && !error && (favorites.length ? (
          <div className="favorites-list">
            {favorites.map((event) => (
              <button type="button" className="favorite-event-item" key={event.id} onClick={() => onOpenEvent?.(event)}>
                <strong>{event.title || `이벤트 ${event.id}`}</strong>
                <span>{event.brandName || event.brand || '브랜드 정보 없음'}</span>
              </button>
            ))}
          </div>
        ) : <p className="favorites-empty">찜한 이벤트가 없습니다.</p>)}
      </main>
    </div>
  );
}

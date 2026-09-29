import { useEffect, useState } from 'react';
import Header from '../components/Header';
import { apiRequest } from '../api/client';
import './FavoritesPage.css';

export default function FavoritesPage({ user, onLoginClick, onLogout, onBack, onOpenEvent, onOpenMyPage, onOpenFavorites, onOpenAdminPage }) {
    const [favorites, setFavorites] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [toggleError, setToggleError] = useState('');
    const [pendingId, setPendingId] = useState(null); // 찜 토글 요청 중인 이벤트 id (중복 클릭 방지)

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
                if (!cancelled) {
                    setFavorites(details
                        .filter((result) => result.status === 'fulfilled')
                        .map((result) => ({ ...result.value, isFavorite: true })));
                }
            })
            .catch((requestError) => { if (!cancelled) setError(requestError.message); })
            .finally(() => { if (!cancelled) setLoading(false); });
        return () => { cancelled = true; };
    }, [user]);

    // 찜 해제해도 목록에서 바로 사라지지 않고 하트만 비워둠 → 실수로 눌렀을 때 다시 찜할 수 있음.
    // (페이지를 다시 열면 해제된 이벤트는 목록에서 빠집니다)
    const toggleFavorite = async (event) => {
        if (pendingId === event.id) return;
        const nextFavorite = !event.isFavorite;
        setPendingId(event.id);
        setToggleError('');
        try {
            await apiRequest(`/api/events/${event.id}/favorite`, {
                method: nextFavorite ? 'POST' : 'DELETE',
            });
            setFavorites((current) => current.map((item) => (
                item.id === event.id ? { ...item, isFavorite: nextFavorite } : item
            )));
        } catch (requestError) {
            setToggleError(requestError.message);
        } finally {
            setPendingId(null);
        }
    };

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
                {toggleError && <p className="favorites-error" role="alert">{toggleError}</p>}
                {loading && <p className="favorites-message">찜한 목록을 불러오는 중입니다...</p>}
                {!loading && error && <p className="favorites-error">오류가 발생했습니다: {error}</p>}
                {!loading && !error && (favorites.length ? (
                    <div className="favorites-list">
                        {favorites.map((event) => (
                            // 버튼 안에 버튼을 넣을 수 없으므로, 카드 버튼과 찜 버튼을 형제로 두고 겹쳐서 배치
                            <div className="favorite-event-row" key={event.id}>
                                <button
                                    type="button"
                                    className="favorite-event-item"
                                    onClick={() => onOpenEvent?.({ ...event, brand: event.brandName || event.brand })}
                                >
                                    <strong>{event.title || `이벤트 ${event.id}`}</strong>
                                    <span>{event.brandName || event.brand || '브랜드 정보 없음'}</span>
                                </button>
                                <button
                                    type="button"
                                    className={`favorite-toggle ${event.isFavorite ? 'is-favorite' : ''}`}
                                    aria-label={event.isFavorite ? '찜 취소' : '찜하기'}
                                    aria-pressed={event.isFavorite}
                                    disabled={pendingId === event.id}
                                    onClick={() => toggleFavorite(event)}
                                >
                                    <svg className="favorite-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M20.8 8.8c0 5.1-8.8 10.1-8.8 10.1S3.2 13.9 3.2 8.8A4.8 4.8 0 0 1 12 6.1a4.8 4.8 0 0 1 8.8 2.7Z" /></svg>
                                </button>
                            </div>
                        ))}
                    </div>
                ) : <p className="favorites-empty">찜한 이벤트가 없습니다.</p>)}
            </main>
        </div>
    );
}
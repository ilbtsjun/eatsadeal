import Header from '@/common/components/Header';
import './FavoritesPage.css';
import { useFavoriteList } from '@/features/favorites/hooks/useFavoriteList.js';

export default function FavoritesPage({ user, onBack, onOpenEvent }) {
    const {
        favorites,
        loading,
        error,
        toggleError,
        pendingId,
        toggleFavorite,
    } = useFavoriteList(user);

    return (
        <div className="favorites-page">
            <Header />
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
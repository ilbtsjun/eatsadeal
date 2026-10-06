import { useEffect, useState } from 'react';
import { apiRequest, getToken } from '../api/client';
import './EventCardList.css';

const BRAND_CATEGORY_MAP = {
    // 치킨 브랜드
    'BHC': 'chicken', 'BBQ': 'chicken', 'Kyochon': 'chicken', 'Pelicana': 'chicken', 'Goobne': 'chicken',
    // 피자 브랜드
    'Dominos': 'pizza', 'Papajohns': 'pizza', 'Pizzamaru': 'pizza', 'Pizzaettang': 'pizza', 'Pizzaschool': 'pizza',
    // 햄버거 브랜드
    'Burgerking': 'hamburger', 'Frankburger': 'hamburger', 'KFC': 'hamburger', 'Lottelia': 'hamburger', 'Momstouch': 'hamburger',
};

const getFallbackImage = (brandName) => {
    const category = BRAND_CATEGORY_MAP[brandName];
    if (category === 'chicken') return '/images/default-chicken.webp';
    if (category === 'pizza') return '/images/default-pizza.webp';
    if (category === 'hamburger') return '/images/default-hamburger.webp';
};

function calculateDDay(endDate) {
    if (!endDate) return '';
    const today = new Date();
    const end = new Date(endDate);
    today.setHours(0, 0, 0, 0);
    end.setHours(0, 0, 0, 0);
    const diffDays = Math.ceil((end.getTime() - today.getTime()) / (1000 * 60 * 60 * 24));
    if (diffDays < 0) return '종료';
    if (diffDays === 0) return 'D-Day';
    return `D-${diffDays}`;
}

function getEventCodeName(eventCodes) {
    if (!eventCodes || eventCodes.length === 0) return '이벤트';
    const codeName = {
        DISCOUNT_PRICE: '정액 할인', DISCOUNT_RATE: '정률 할인', BUY_ONE_GET_ONE: '1+1',
        BUY_N_GET_N: 'N+1', TAKE_OUT: '포장 할인', DELIVERY_FREE: '배달비 무료',
        GIFT_PROMO: '사은품', PAYMENT_PROMO: '결제 할인', MEMBERSHIP: '멤버십', TIME_SALE: '타임세일',
    };
    return codeName[eventCodes[0]] || '이벤트';
}

export default function EventCardList({
                                          activeCategory = 'all',
                                          activeSort = 'latest',
                                          searchKeyword = '',
                                          onSelectEvent,
                                          user,
                                      }) {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [page, setPage] = useState(0);
    const [totalPages, setTotalPages] = useState(0);
    const [totalElements, setTotalElements] = useState(0);
    const PAGE_SIZE = 20;
    const PAGE_GROUP_SIZE = 5;

    useEffect(() => {
        let cancelled = false;
        const params = new URLSearchParams({
            page: String(page),
            size: String(PAGE_SIZE),

            sort: activeSort === 'deadline' ? 'endingSoon' : activeSort === 'popular' ? 'popular' : 'latest',
        });
        if (activeCategory !== 'all' && /^\d+$/.test(String(activeCategory))) params.set('categoryId', activeCategory);
        if (searchKeyword.trim()) params.set('keyword', searchKeyword.trim());

        setLoading(true);
        setError('');
        const favoriteRequest = user && getToken()
            ? apiRequest('/api/user/me/favorites')
            : Promise.resolve([]);

        Promise.all([apiRequest(`/api/events?${params.toString()}`), favoriteRequest])
            .then(([data, favoriteIds]) => {
                if (cancelled) return;
                const favoriteSet = new Set(Array.isArray(favoriteIds) ? favoriteIds.map(Number) : []);
                setTotalPages(Number(data.totalPages || 0));
                setTotalElements(Number(data.totalElements || data.content?.length || 0));
                setEvents((data.content || []).map((event) => ({
                    ...event,
                    brand: event.brandName,
                    isFavorite: favoriteSet.has(Number(event.id)),
                })));
            })
            .catch((requestError) => { if (!cancelled) setError(requestError.message); })
            .finally(() => { if (!cancelled) setLoading(false); });

        return () => { cancelled = true; };
    }, [user, activeCategory, activeSort, searchKeyword, page]);

    useEffect(() => {
        setPage(0);
    }, [activeCategory, activeSort, searchKeyword, user]);

    const toggleFavorite = async (event, clickEvent) => {
        clickEvent.stopPropagation();
        if (!user) {
            window.alert('찜하기 기능을 사용하려면 로그인해주세요.');
            return;
        }

        const nextFavorite = !event.isFavorite;
        try {
            await apiRequest(`/api/events/${event.id}/favorite`, {
                method: nextFavorite ? 'POST' : 'DELETE',
            });
            setEvents((current) => current.map((item) => item.id === event.id
                ? { ...item, isFavorite: nextFavorite }
                : item));
        } catch (requestError) {
            setError(requestError.message);
        }
    };

    const visibleEvents = events;

    const groupStart = Math.floor(page / PAGE_GROUP_SIZE) * PAGE_GROUP_SIZE;
    const groupEnd = Math.min(groupStart + PAGE_GROUP_SIZE, totalPages);
    const pageNumbers = Array.from({ length: groupEnd - groupStart }, (_, i) => groupStart + i);

    if (loading) return <p>이벤트 정보를 불러오는 중입니다...</p>;
    if (error) return <p>오류가 발생했습니다: {error}</p>;

    return (
        <div className="card-list-container">
            <div className="event-list-heading">
                <p className="event-result-count">총 <strong>{totalElements}</strong>개의 할인정보</p>

            </div>
            {visibleEvents.length === 0 ? (
                <p className="empty-events">선택한 음식 테마의 할인 정보가 없습니다.</p>
            ) : (
                <div className="card-grid">
                    {visibleEvents.map((event) => (
                        <div key={event.id} className="event-card-wrapper">
                            <button type="button" className="event-card" onClick={() => onSelectEvent?.(event)}>
                                <div className="card-image-box">
                                    {event.img ? (
                                        <img src={event.img}
                                             alt={event.title}
                                             className="card-image"
                                             onLoad={(e) => {
                                                 if (e.currentTarget.naturalWidth <= 1) e.currentTarget.src = getFallbackImage(event.brand);
                                             }}
                                             onError={(e) => {
                                                 e.currentTarget.onerror = null; // 무한 루프 방지
                                                 e.currentTarget.src = getFallbackImage(event.brand);
                                             }}/>
                                    ) : <span className="card-emoji">🍗</span>}
                                    <span className="card-dday">{calculateDDay(event.endDate)}</span>
                                </div>
                                <div className="card-info">
                                    <span className="card-brand">{event.brand || '이츠어딜'}</span>
                                    <h3 className="card-title">{event.title}</h3>
                                    <div className="card-footer">
                                        <span className="card-discount">{getEventCodeName(event.eventCodes)}</span>
                                        <span className="card-views" aria-label="조회수">조회수 {Number(event.viewCount || 0).toLocaleString()}</span>
                                    </div>
                                </div>
                            </button>
                            <button
                                type="button"
                                className={`card-favorite ${event.isFavorite ? 'is-favorite' : ''}`}
                                aria-label={event.isFavorite ? '찜 취소' : '찜하기'}
                                aria-pressed={Boolean(event.isFavorite)}
                                onClick={(clickEvent) => toggleFavorite(event, clickEvent)}
                            >
                                <svg className="favorite-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M20.8 8.8c0 5.1-8.8 10.1-8.8 10.1S3.2 13.9 3.2 8.8A4.8 4.8 0 0 1 12 6.1a4.8 4.8 0 0 1 8.8 2.7Z" /></svg>
                            </button>
                        </div>
                    ))}
                </div>
            )}
            {totalPages > 1 && (
                <nav className="pagination" aria-label="이벤트 페이지 이동">
                    <button
                        type="button"
                        className="page-btn"
                        onClick={() => setPage(0)}
                        disabled={page === 0}
                        aria-label="첫 페이지"
                    >
                        &laquo;
                    </button>
                    <button
                        type="button"
                        className="page-btn"
                        onClick={() => setPage((p) => Math.max(p - 1, 0))}
                        disabled={page === 0}
                        aria-label="이전 페이지"
                    >
                        &lsaquo;
                    </button>

                    {pageNumbers.map((pageIndex) => (
                        <button
                            key={pageIndex}
                            type="button"
                            className={`page-btn ${pageIndex === page ? 'is-active' : ''}`}
                            onClick={() => setPage(pageIndex)}
                            aria-current={pageIndex === page ? 'page' : undefined}
                        >
                            {pageIndex + 1}
                        </button>
                    ))}

                    <button
                        type="button"
                        className="page-btn"
                        onClick={() => setPage((p) => Math.min(p + 1, totalPages - 1))}
                        disabled={page >= totalPages - 1}
                        aria-label="다음 페이지"
                    >
                        &rsaquo;
                    </button>
                    <button
                        type="button"
                        className="page-btn"
                        onClick={() => setPage(totalPages - 1)}
                        disabled={page >= totalPages - 1}
                        aria-label="마지막 페이지"
                    >
                        &raquo;
                    </button>
                </nav>
            )}
        </div>
    );
}
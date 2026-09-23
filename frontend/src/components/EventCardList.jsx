import { useEffect, useState } from 'react';
import { apiRequest, getToken } from '../api/client';
import './EventCardList.css';

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

// 백엔드에 category 필드가 추가되기 전에도 기존 데이터로 필터가 동작하도록 만든 임시 분류 함수입니다.
function getEventCategory(event) {
    if (event.category) return String(event.category).toLowerCase();
    if (event.categoryId) return String(event.categoryId).toLowerCase();

    const text = `${event.title || ''} ${event.description || ''} ${event.brand || ''}`.toLowerCase();
    const categoryKeywords = {
        chicken: ['치킨', 'bhc', '교촌', '굽네', '네네', '푸라닭', '후라이드'],
        pizza: ['피자', '도미노', '피자헛', '미스터피자'],
        bunsik: ['분식', '떡볶이', '김밥', '순대', '튀김'],
        western: ['양식', '파스타', '스테이크', '햄버거', '버거'],
        chinese: ['중식', '짜장', '짬뽕', '탕수육', '마라'],
        korean: ['한식', '비빔밥', '불고기', '국밥', '찌개'],
        japanese: ['일식', '초밥', '스시', '돈카츠', '우동', '라멘'],
        fastfood: ['패스트푸드', '맥도날드', '버거킹', '롯데리아', 'kfc'],
        cafe: ['카페', '디저트', '커피', '베이커리', '케이크', '빵'],
    };
    return Object.entries(categoryKeywords).find(([, keywords]) =>
        keywords.some((keyword) => text.includes(keyword.toLowerCase())),
    )?.[0] || 'etc';
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
    const [pageInput, setPageInput] = useState('1');
    const PAGE_SIZE = 20; // 데스크톱 기준 4열 x 5줄

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
        setPageInput('1');
    }, [activeCategory, activeSort, searchKeyword, user]);

    useEffect(() => {
        setPageInput(String(page + 1));
    }, [page]);

    const moveToInputPage = (event) => {
        event.preventDefault();
        const requestedPage = Number.parseInt(pageInput, 10);
        if (!Number.isInteger(requestedPage) || totalPages < 1) {
            setPageInput(String(page + 1));
            return;
        }
        const targetPage = Math.min(Math.max(requestedPage, 1), totalPages) - 1;
        setPage(targetPage);
        setPageInput(String(targetPage + 1));
    };

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
                                        <img src={event.img} alt={event.title} className="card-image" />
                                    ) : <span className="card-emoji">🍗</span>}
                                    <span className="card-dday">{calculateDDay(event.endDate)}</span>
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
                                <div className="card-info">
                                    <span className="card-brand">{event.brand || '이츠어딜'}</span>
                                    <h3 className="card-title">{event.title}</h3>
                                    <div className="card-footer">
                                        <span className="card-discount">{getEventCodeName(event.eventCodes)}</span>
                                        <span className="card-views" aria-label="조회수">조회수 {Number(event.viewCount || 0).toLocaleString()}</span>
                                    </div>
                                </div>
                            </button>

                        </div>
                    ))}
                </div>
            )}
            {totalPages > 1 && (
                <nav className="pagination" aria-label="이벤트 페이지 이동">
                    <form className="page-jump-form" onSubmit={moveToInputPage}>
                        <label htmlFor="event-page-input">페이지</label>
                        <input
                            id="event-page-input"
                            type="number"
                            min="1"
                            max={totalPages}
                            value={pageInput}
                            onChange={(event) => setPageInput(event.target.value)}
                            disabled={loading}
                            aria-label={`페이지 번호 입력, 1에서 ${totalPages}까지`}
                        />
                        <span>/ {totalPages}</span>
                    </form>
                </nav>
            )}
        </div>
    );
}

import { useEffect, useMemo, useState } from 'react';
import './EventCardList.css';
import AdminTools from './AdminTools';

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

function toTime(value, fallback) {
    const time = value ? new Date(value).getTime() : NaN;
    return Number.isNaN(time) ? fallback : time;
}

function sortEvents(events, activeSort) {
    return [...events].sort((a, b) => {
        if (activeSort === 'deadline') {
            // 종료일이 빠른 이벤트가 먼저 오도록 정렬합니다.
            return toTime(a.endDate, Number.MAX_SAFE_INTEGER) - toTime(b.endDate, Number.MAX_SAFE_INTEGER);
        }
        if (activeSort === 'discount') {
            // 현재 Event에는 할인율 필드가 없으므로 정률 할인 이벤트를 우선합니다.
            const aRate = a.eventCodes?.includes('DISCOUNT_RATE') ? 1 : 0;
            const bRate = b.eventCodes?.includes('DISCOUNT_RATE') ? 1 : 0;
            return bRate - aRate;
        }
        return toTime(b.startDate, 0) - toTime(a.startDate, 0);
    });
}

export default function EventCardList({
    activeCategory = 'all',
    activeSort = 'latest',
    isAdmin = false,
    isManaging = false,
    onToggleManage,
    onSelectEvent,
}) {
    const [events, setEvents] = useState([]);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');

    useEffect(() => {
        fetch('http://localhost:8080/api/events')
            .then((response) => {
                if (!response.ok) throw new Error('이벤트 정보를 불러오지 못했습니다.');
                return response.json();
            })
            .then((data) => setEvents(data))
            .catch((requestError) => setError(requestError.message))
            .finally(() => setLoading(false));
    }, []);

    const visibleEvents = useMemo(() => {
        const filteredEvents = activeCategory === 'all'
            ? events
            : events.filter((event) => getEventCategory(event) === activeCategory);
        return sortEvents(filteredEvents, activeSort);
    }, [events, activeCategory, activeSort]);

    if (loading) return <p>이벤트 정보를 불러오는 중입니다...</p>;
    if (error) return <p>오류가 발생했습니다: {error}</p>;

    return (
        <div className="card-list-container">
            <div className="event-list-heading">
                <p className="event-result-count">총 <strong>{visibleEvents.length}</strong>개의 할인정보</p>
                {isAdmin && (
                    <AdminTools
                        isManaging={isManaging}
                        onToggleManage={onToggleManage}
                    />
                )}
            </div>
            {visibleEvents.length === 0 ? (
                <p className="empty-events">선택한 음식 테마의 할인 정보가 없습니다.</p>
            ) : (
                <div className="card-grid">
                    {visibleEvents.map((event) => (
                        <div key={event.id} className={`event-card-wrapper ${isAdmin && isManaging ? 'is-managing' : ''}`}>
                            <button type="button" className="event-card" onClick={() => onSelectEvent?.(event)}>
                                <div className="card-image-box">
                                    {event.img ? (
                                        <img src={event.img} alt={event.title} className="card-image" />
                                    ) : <span className="card-emoji">🍗</span>}
                                    <span className="card-dday">{calculateDDay(event.endDate)}</span>
                                </div>
                                <div className="card-info">
                                    <span className="card-brand">{event.brand || '이츠어딜'}</span>
                                    <h3 className="card-title">{event.title}</h3>
                                    <div className="card-footer">
                                        <span className="card-discount">{getEventCodeName(event.eventCodes)}</span>
                                    </div>
                                </div>
                            </button>
                            {isAdmin && isManaging && (
                                <div className="event-admin-actions">
                                    <button type="button" onClick={() => {}}>수정</button>
                                    <button type="button" onClick={() => {}}>삭제</button>
                                </div>
                            )}
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}

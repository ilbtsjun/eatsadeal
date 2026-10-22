import { useEventList } from '../hooks/useEventList.js';
import EventCard from './EventCard.jsx';
import Pagination from './Pagination.jsx';
import './EventCardList.css';

export default function EventCardList({
                                          activeCategory = 'all',
                                          activeEventCode = 'all',
                                          activeSort = 'latest',
                                          searchKeyword = '',
                                          page = 0,
                                          onPageChange,
                                          onSelectEvent,
                                          user,
                                      }) {
    const { events,
        loading,
        error,
        totalPages,
        totalElements,
        toggleFavorite } = useEventList({
        activeCategory,
        activeEventCode,
        activeSort,
        searchKeyword,
        page,
        user,
        onPageChange,
    });

    if (loading) return <p>이벤트 정보를 불러오는 중입니다...</p>;
    if (error) return <p>오류가 발생했습니다: {error}</p>;

    return (
        <div className="card-list-container">
            <div className="event-list-heading">
                <p className="event-result-count">총 <strong>{totalElements}</strong>개의 할인정보</p>
            </div>
            {events.length === 0 ? (
                <p className="empty-events">선택한 조건의 할인 정보가 없습니다.</p>
            ) : (
                <div className="card-grid">
                    {events.map((event) => (
                        <EventCard
                            key={event.id}
                            event={event}
                            toggleFavorite={toggleFavorite}
                            onSelectEvent={onSelectEvent}
                        />
                    ))}
                </div>
            )}
            <Pagination page={page} totalPages={totalPages} onPageChange={onPageChange} />
        </div>
    );
}
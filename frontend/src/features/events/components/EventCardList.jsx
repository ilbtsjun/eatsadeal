import StatusMessage from '@/common/components/StatusMessage.jsx';
import { useEventList } from '../hooks/useEventList.js';
import EventCard from './EventCard.jsx';
import Pagination from './Pagination.jsx';
import './EventCardList.css';

export default function EventCardList({
                                          activeCategory = 'all',
                                          activeBrand = 'all',
                                          activeEventCode = 'all',
                                          activeSort = 'latest',
                                          searchKeyword = '',
                                          page = 0,
                                          onPageChange,
                                          onSelectEvent,
                                          onLoginClick,
                                          user,
                                      }) {
    const { events,
        loading,
        loadError,
        actionError,
        pendingId,
        totalPages,
        totalElements,
        toggleFavorite,
        reload } = useEventList({
        activeCategory,
        activeBrand,
        activeEventCode,
        activeSort,
        searchKeyword,
        page,
        user,
        onPageChange,
        onLoginClick,
    });

    if (loading) return <StatusMessage type="loading" size="inline" message="이벤트 정보를 불러오는 중입니다..." />;
    if (loadError) return <StatusMessage type="error" size="inline" title="이벤트를 불러오지 못했어요" message={loadError} onRetry={reload} />;

    return (
        <div className="card-list-container">
            <div className="event-list-heading">
                <p className="event-result-count">총 <strong>{totalElements}</strong>개의 할인정보</p>
            </div>
            {actionError && <p className="event-action-error" role="alert">{actionError}</p>}
            {events.length === 0 ? (
                <p className="empty-events">선택한 조건의 할인 정보가 없습니다.</p>
            ) : (
                <div className="card-grid">
                    {events.map((event) => (
                        <EventCard
                            key={event.id}
                            event={event}
                            toggleFavorite={toggleFavorite}
                            favoritePending={pendingId === event.id}
                            onSelectEvent={onSelectEvent}
                        />
                    ))}
                </div>
            )}
            <Pagination page={page} totalPages={totalPages} onPageChange={onPageChange} />
        </div>
    );
}
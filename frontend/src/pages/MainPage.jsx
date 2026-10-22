import Header from '@/common/components/Header';
import CategoryFilter from '@/features/categories/components/CategoryFilter';
import HighlightBanner from '@/features/events/components/HighlightBanner';
import EventCardList from '@/features/events/components/EventCardList';
import FloatingButton from '@/features/reports/components/FloatingButton';
import SiteFooter from '@/common/components/SiteFooter';
import { useEventSearchParams } from '@/features/events/hooks/useEventSearchParams.js';

export default function MainPage({ user, onSelectEvent, onOpenTerms, onOpenPrivacy}) {
    const {
        category,
        eventCode,
        sort,
        keyword,
        page,
        updateSearchParams,
    } = useEventSearchParams();

    return (
        <div className="main-page">
            <Header
                showSearch
                searchKeyword={keyword}
                onSearch={(k) => updateSearchParams({ q: k.trim() })}
            />

            <CategoryFilter
                activeCategory={category}
                onCategoryChange={(c) => updateSearchParams({ category: c })}
                activeEventCode={eventCode}
                onEventCodeChange={(code) => updateSearchParams({ eventCode: code })}
                activeSort={sort}
                onSortChange={(s) => updateSearchParams({ sort: s })}
            />

            <HighlightBanner onSelectEvent={onSelectEvent}/>

            <EventCardList
                activeCategory={category}
                activeEventCode={eventCode}
                activeSort={sort}
                searchKeyword={keyword}
                page={page}
                onPageChange={(p, replace = false) => updateSearchParams({ page: p + 1 }, { keepPage: true, replace })}
                onSelectEvent={onSelectEvent}
                user={user}
            />

            <FloatingButton />
            <SiteFooter onOpenTerms={onOpenTerms} onOpenPrivacy={onOpenPrivacy} />
        </div>
    );
}
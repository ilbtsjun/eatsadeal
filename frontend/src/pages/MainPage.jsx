import { useSearchParams } from 'react-router';
import Header from '@/common/components/Header';
import CategoryFilter from '@/features/categories/components/CategoryFilter';
import HighlightBanner from '@/features/events/components/HighlightBanner';
import EventCardList from '@/features/events/components/EventCardList';
import FloatingButton from '@/features/reports/components/FloatingButton';
import SiteFooter from '@/common/components/SiteFooter';

const DEFAULTS = { category: 'all', sort: 'latest', q: '', page: 1 };

export default function MainPage({ user, onSelectEvent, onOpenTerms, onOpenPrivacy}) {
    const [params, setParams] = useSearchParams();
    const category = params.get('category') ?? DEFAULTS.category;
    const sort = params.get('sort') ?? DEFAULTS.sort;
    const keyword = params.get('q') ?? DEFAULTS.q;
    const page = Math.max(1, Number(params.get('page')) || 1) - 1;

    const update = (patch, { keepPage = false, replace = false } = {}) => {
        const next = new URLSearchParams(params);
        Object.entries(patch).forEach(([key, value]) => {
            if (value == null || value === '' || value === DEFAULTS[key])
                next.delete(key);
            else
                next.set(key, String(value));
        });
        if (!keepPage)
            next.delete('page');
        if (next.toString() === params.toString())
            return;
        setParams(next, { replace });
    };

    return (
        <div className="main-page">
            <Header
                showSearch
                searchKeyword={keyword}
                onSearch={(k) => update({ q: k.trim() })}
            />

            <CategoryFilter
                activeCategory={category}
                onCategoryChange={(c) => update({ category: c })}
                activeSort={sort}
                onSortChange={(s) => update({ sort: s })}
            />

            <HighlightBanner onSelectEvent={onSelectEvent}/>

            <EventCardList
                activeCategory={category}
                activeSort={sort}
                searchKeyword={keyword}
                page={page}
                onPageChange={(p, replace = false) => update({ page: p + 1 }, { keepPage: true, replace })}
                onSelectEvent={onSelectEvent}
                user={user}
            />

            <FloatingButton />
            <SiteFooter onOpenTerms={onOpenTerms} onOpenPrivacy={onOpenPrivacy} />
        </div>
    );
}
import { useSearchParams } from 'react-router';
import Header from '../components/Header';
import CategoryFilter from '../components/CategoryFilter';
import HighlightBanner from '../components/HighlightBanner';
import EventCardList from '../components/EventCardList';
import FloatingButton from '../components/FloatingButton';
import SiteFooter from '../components/SiteFooter';

const DEFAULTS = { category: 'all', sort: 'latest', q: '', page: 1 };

export default function MainPage({ user, onLoginClick, onLogout, onSelectEvent, onOpenAdminPage,
                                     onOpenMyPage, onOpenFavorites, onOpenTerms, onOpenPrivacy,}) {
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
                user={user}
                onLoginClick={onLoginClick}
                onLogout={onLogout}
                onOpenMyPage={onOpenMyPage}
                onOpenFavorites={onOpenFavorites}
                onOpenAdminPage={onOpenAdminPage}
                showSearch
                searchKeyword={keyword}
                onSearch={(k) => update({ q:k.trim() })}
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
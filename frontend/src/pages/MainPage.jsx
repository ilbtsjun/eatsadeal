import { useState } from 'react';
import Header from '../components/Header';
import CategoryFilter from '../components/CategoryFilter';
import HighlightBanner from '../components/HighlightBanner';
import EventCardList from '../components/EventCardList';
import FloatingButton from '../components/FloatingButton';

export default function MainPage({ user, onLoginClick, onLogout, onSelectEvent, onOpenAdminPage, onOpenMyPage }) {
  const [activeCategory, setActiveCategory] = useState('all');
  const [activeSort, setActiveSort] = useState('latest');
  const [isManaging, setIsManaging] = useState(false);

  return (
    <div className="main-page">
      <Header
        user={user}
        onLoginClick={onLoginClick}
        onLogout={onLogout}
        onOpenMyPage={onOpenMyPage}
        onOpenAdminPage={onOpenAdminPage}
      />

      <CategoryFilter
        activeCategory={activeCategory}
        onCategoryChange={setActiveCategory}
        activeSort={activeSort}
        onSortChange={setActiveSort}
      />

      <HighlightBanner />

      <EventCardList
        activeCategory={activeCategory}
        activeSort={activeSort}
        isAdmin={user?.role === 'ADMIN'}
        isManaging={isManaging}
        onToggleManage={() => setIsManaging((current) => !current)}
        onSelectEvent={onSelectEvent}
        onOpenAdminPage={onOpenAdminPage}
        onOpenMyPage={onOpenMyPage}
      />

      <FloatingButton />
    </div>
  );
}

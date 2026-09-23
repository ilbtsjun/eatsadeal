import { useState } from 'react';
import Header from '../components/Header';
import CategoryFilter from '../components/CategoryFilter';
import HighlightBanner from '../components/HighlightBanner';
import EventCardList from '../components/EventCardList';
import FloatingButton from '../components/FloatingButton';

export default function MainPage({ user, onLoginClick, onLogout, onSelectEvent, onOpenAdminPage, onOpenMyPage, onOpenFavorites, searchKeyword = '', onSearch }) {
  const [activeCategory, setActiveCategory] = useState('all');
  const [activeSort, setActiveSort] = useState('latest');

  return (
    <div className="main-page">
      <Header
        user={user}
        onLoginClick={onLoginClick}
        onLogout={onLogout}
        onOpenMyPage={onOpenMyPage}
        onOpenFavorites={onOpenFavorites}
        onOpenAdminPage={onOpenAdminPage}
        searchKeyword={searchKeyword}
        onSearch={onSearch}
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
        searchKeyword={searchKeyword}
        onSelectEvent={onSelectEvent}
        user={user}
      />

      <FloatingButton />
    </div>
  );
}

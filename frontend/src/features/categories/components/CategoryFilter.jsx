import { useCategories } from '../hooks/useCategories.js';
import { EVENT_CODE_NAMES } from '@/features/events/utils/eventDisplay.js';
import './CategoryFilter.css';

export default function CategoryFilter({ activeCategory, onCategoryChange, activeEventCode = 'all', onEventCodeChange, activeSort, onSortChange }) {
    const { categories, categoryBrands, brandPanelOpen, selectCategory } =
        useCategories({ activeCategory, onCategoryChange });
    return (
    <div className="category-filter-container">
      <div className="category-list" aria-label="음식 카테고리">
        {categories.map((category) =>
            <button key={category.id}
                    type="button"
                    className={`category-btn ${activeCategory === category.id ? 'active' : ''}`}
                    aria-expanded={category.id === 'all' ? undefined : activeCategory === category.id && brandPanelOpen}
                    onClick={() => selectCategory(category.id)}>
                {category.name}
            </button>)
        }
      </div>
      <div className={`brand-panel ${brandPanelOpen ? 'open' : ''}`} aria-hidden={!brandPanelOpen}>
        <div className="brand-panel-inner">
          <div className="brand-list" aria-label="브랜드">
            {[{ id: 'all', name: '전체' }, ...categoryBrands].map((brand) =>
                <button key={brand.id}
                        type="button"
                        tabIndex={brandPanelOpen ? 0 : -1}
                        className={`brand-btn ${brand.id === 'all' ? 'active' : ''}`}>
                    {brand.name}
                </button>)
            }
          </div>
        </div>
      </div>
      <div className="event-code-list" aria-label="할인 종류">
        {[['all', '전체'], ...Object.entries(EVENT_CODE_NAMES)].map(([code, name]) =>
            <button key={code}
                    type="button"
                    className={`event-code-btn ${activeEventCode === code ? 'active' : ''}`}
                    onClick={() => onEventCodeChange?.(code)}>
                {name}
            </button>)
        }
      </div>
      <div className="filter-bar">
        <span className="result-count">음식 테마를 선택해 할인 정보를 확인해보세요</span>
        <div className="sort-buttons" aria-label="정렬 기준">
          {[['latest', '최신순'], ['deadline', '마감임박순'], ['popular', '인기순']]
              .map(([sort, label], index) =>
                  <span key={sort} className="sort-item">
                      {index > 0 && <span className="divider">|</span>}
                      <button type="button" className={activeSort === sort ? 'active-sort' : ''}
                              onClick={() => onSortChange(sort)}>{label}</button>
                  </span>)
          }
        </div>
      </div>
    </div>
  );
}

import { useCategories } from '../hooks/useCategories.js';
import { EVENT_CODE_NAMES } from '@/features/events/utils/eventDisplay.js';
import './CategoryFilter.css';

export default function CategoryFilter({ activeCategory, onCategoryChange, activeBrand = 'all', onBrandChange, activeEventCode = 'all', onEventCodeChange, activeSort, onSortChange }) {
    const {
        categories,
        categoryBrands,
        brandPanelOpen,
        selectCategory,
        selectBrand,
        brandListRef,
        brandScroll,
        scrollBrands,
    } = useCategories({ activeCategory, onCategoryChange, activeBrand, onBrandChange });
    return (
    <div className="category-filter-container">
      <div className="category-list" aria-label="음식 카테고리">
        {categories.map((category) =>
            <button key={category.id}
                    type="button"
                    className={`category-btn ${activeCategory === category.id ? 'active' : ''}`}
                    aria-expanded={activeCategory === category.id && brandPanelOpen}
                    onClick={() => selectCategory(category.id)}>
                {category.name}
            </button>)
        }
      </div>
      <div className={`brand-panel ${brandPanelOpen ? 'open' : ''}`} aria-hidden={!brandPanelOpen}>
        <div className="brand-panel-inner">
          <div className={`brand-scroller ${brandScroll.prev || brandScroll.next ? '' : 'no-overflow'}`}>
            <button type="button"
                    className="brand-arrow"
                    aria-label="이전 브랜드"
                    tabIndex={brandPanelOpen ? 0 : -1}
                    disabled={!brandScroll.prev}
                    onClick={() => scrollBrands(-1)}>
                &lsaquo;
            </button>
            <div className="brand-list" aria-label="브랜드" ref={brandListRef}>
              {[{ id: 'all', name: '전체' }, ...categoryBrands].map((brand) =>
                  <button key={brand.id}
                          type="button"
                          tabIndex={brandPanelOpen ? 0 : -1}
                          className={`brand-btn ${activeBrand === brand.id ? 'active' : ''}`}
                          onClick={() => selectBrand(brand.id)}>
                      {brand.name}
                  </button>)
              }
            </div>
            <button type="button"
                    className="brand-arrow"
                    aria-label="다음 브랜드"
                    tabIndex={brandPanelOpen ? 0 : -1}
                    disabled={!brandScroll.next}
                    onClick={() => scrollBrands(1)}>
                &rsaquo;
            </button>
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

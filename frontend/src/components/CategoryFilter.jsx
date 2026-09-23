import { useEffect, useState } from 'react';
import { apiRequest } from '../api/client';
import './CategoryFilter.css';



export default function CategoryFilter({ activeCategory, onCategoryChange, activeSort, onSortChange, onCategoriesChange }) {
  const [categories, setCategories] = useState([{ id: 'all', name: '전체' }]);
  useEffect(() => {
    let cancelled = false;

    apiRequest('/api/categories')
      .then((data) => {
        if (cancelled) return;
        const list = Array.isArray(data)
          ? data
          : data?.data || data?.content || data?.categories || data?.categoryList || [];
        const nextCategories = list
          .map((item) => ({
            id: item.id ?? item.categoryId,
            name: item.name ?? item.categoryName,
          }))
          .filter((item) => item.id !== null && item.id !== undefined && item.name);
        const next = [{ id: 'all', name: '전체' }, ...nextCategories.map((item) => ({
          id: String(item.id),
          name: item.name,
        }))];
        setCategories(next);
        onCategoriesChange?.(next);
      })
      .catch(() => {
        if (!cancelled) {
          // API 실패 시 숫자가 아닌 가짜 ID로 이벤트 검색을 요청하지 않습니다.
          const onlyAll = [{ id: 'all', name: '전체' }];
          setCategories(onlyAll);
          onCategoriesChange?.(onlyAll);
        }
      });

    return () => { cancelled = true; };
  }, [onCategoriesChange]);

  return (
    <div className="category-filter-container">
      <div className="category-list" aria-label="음식 카테고리">
        {categories.map((category) => <button key={category.id} type="button" className={`category-btn ${activeCategory === category.id ? 'active' : ''}`} onClick={() => onCategoryChange(category.id)}>{category.name}</button>)}
      </div>
      <div className="filter-bar">
        <span className="result-count">음식 테마를 선택해 할인 정보를 확인해보세요</span>
        <div className="sort-buttons" aria-label="정렬 기준">
          {[['latest', '최신순'], ['deadline', '마감임박순'], ['popular', '인기순']].map(([sort, label], index) => <span key={sort} className="sort-item">{index > 0 && <span className="divider">|</span>}<button type="button" className={activeSort === sort ? 'active-sort' : ''} onClick={() => onSortChange(sort)}>{label}</button></span>)}
        </div>
      </div>
    </div>
  );
}

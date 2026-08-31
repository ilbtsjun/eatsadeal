import './CategoryFilter.css';

const categories = [
    { id: 'all', name: '전체' },
    { id: 'chicken', name: '치킨' },
    { id: 'pizza', name: '피자' },
    { id: 'bunsik', name: '분식' },
    { id: 'western', name: '양식' },
    { id: 'chinese', name: '중식' },
    { id: 'korean', name: '한식' },
    { id: 'japanese', name: '일식' },
    { id: 'fastfood', name: '패스트푸드' },
    { id: 'cafe', name: '카페/디저트' },
];

export default function CategoryFilter({
    activeCategory,
    onCategoryChange,
    activeSort,
    onSortChange,
}) {
    return (
        <div className="category-filter-container">
            <div className="category-list" aria-label="음식 카테고리">
                {categories.map((category) => (
                    <button
                        key={category.id}
                        type="button"
                        className={`category-btn ${activeCategory === category.id ? 'active' : ''}`}
                        onClick={() => onCategoryChange(category.id)}
                    >
                        {category.name}
                    </button>
                ))}
            </div>

            <div className="filter-bar">
                <span className="result-count">음식 테마를 선택해 할인 정보를 확인해보세요</span>
                <div className="sort-buttons" aria-label="정렬 기준">
                    {[
                        ['latest', '최신순'],
                        ['deadline', '마감임박순'],
                        ['discount', '할인율높은순'],
                    ].map(([sort, label], index) => (
                        <span key={sort} className="sort-item">
                            {index > 0 && <span className="divider">|</span>}
                            <button
                                type="button"
                                className={activeSort === sort ? 'active-sort' : ''}
                                onClick={() => onSortChange(sort)}
                            >
                                {label}
                            </button>
                        </span>
                    ))}
                </div>
            </div>
        </div>
    );
}

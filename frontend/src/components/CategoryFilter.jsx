import React, { useState } from 'react';
import './CategoryFilter.css';

export default function CategoryFilter() {
    // 선택된 카테고리 상태 관리 (기본값: 'all')
    const [activeCategory, setActiveCategory] = useState('all');
    // 선택된 정렬 기준 상태 관리 (기본값: 'latest')
    const [activeSort, setActiveSort] = useState('latest');

    const categories = [
        { id: 'all', name: '전체' },
        { id: 'chicken', name: '치킨' },
        { id: 'fastfood', name: '패스트푸드' },
        { id: 'pizza', name: '피자/양식' },
        { id: 'cafe', name: '카페/디저트' },
    ];

    return (
        <div className="category-filter-container">
            {/* 1. 카테고리 버튼 목록 */}
            <div className="category-list">
                {categories.map((cat) => (
                    <button
                        key={cat.id}
                        className={`category-btn ${activeCategory === cat.id ? 'active' : ''}`}
                        onClick={() => setActiveCategory(cat.id)}
                    >
                        {cat.name}
                    </button>
                ))}
            </div>

            {/* 2. 정렬 필터바 */}
            <div className="filter-bar">
                <span className="result-count">총 <b>12</b>개의 할인 정보</span>
                <div className="sort-buttons">
                    <button
                        className={activeSort === 'latest' ? 'active-sort' : ''}
                        onClick={() => setActiveSort('latest')}
                    >
                        최신순
                    </button>
                    <span className="divider">|</span>
                    <button
                        className={activeSort === 'deadline' ? 'active-sort' : ''}
                        onClick={() => setActiveSort('deadline')}
                    >
                        마감임박순
                    </button>
                    <span className="divider">|</span>
                    <button
                        className={activeSort === 'discount' ? 'active-sort' : ''}
                        onClick={() => setActiveSort('discount')}
                    >
                        할인율높은순
                    </button>
                </div>
            </div>
        </div>
    );
}
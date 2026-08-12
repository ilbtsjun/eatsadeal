import React from 'react';
import './Header.css';

export default function Header() {
    return (
        <header className="site-header">
            {/* 1. 로고 영역 */}
            <div className="header-logo">
                <a href="/">이츠어딜!</a>
            </div>

            {/* 2. 검색창 영역 */}
            <div className="header-search">
                <input
                    type="text"
                    placeholder="브랜드나 메뉴명을 검색해보세요 (예: 버거킹, 치킨)"
                />
                <button type="button">검색</button>
            </div>

            {/* 3. 우측 메뉴 영역 */}
            <div className="header-menu">
                <button className="menu-btn">즐겨찾기</button>
                <button className="menu-btn login-btn">로그인</button>
            </div>
        </header>
    );
}
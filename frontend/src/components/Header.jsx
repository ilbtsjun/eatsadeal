import { useEffect, useRef, useState } from 'react';
import './Header.css';

export default function Header({ user, onLoginClick, onLogout, onOpenMyPage, onOpenAdminPage }) {
  const [isUserMenuOpen, setIsUserMenuOpen] = useState(false);
  const userMenuRef = useRef(null);

  useEffect(() => {
    const handleOutsideClick = (event) => {
      if (userMenuRef.current && !userMenuRef.current.contains(event.target)) setIsUserMenuOpen(false);
    };
    document.addEventListener('mousedown', handleOutsideClick);
    return () => document.removeEventListener('mousedown', handleOutsideClick);
  }, []);

  const handleLogout = () => {
    setIsUserMenuOpen(false);
    onLogout();
  };

  return (
    <header className="site-header">
      <div className="header-logo">
        <a href="/">이츠어딜!</a>
      </div>

      <form
        className="header-search"
        onSubmit={(event) => event.preventDefault()}
      >
        <input
          type="search"
          placeholder="브랜드나 메뉴명을 검색해보세요 (예: 버거킹, 치킨)"
        />
        <button type="submit">검색</button>
      </form>

      <div className="header-menu">
        {user ? (
          <div className="user-menu" ref={userMenuRef}>
            <button
              type="button"
              className={`person-button ${isUserMenuOpen ? 'active' : ''}`}
              aria-label="사용자 메뉴 열기"
              aria-expanded={isUserMenuOpen}
              onClick={() => setIsUserMenuOpen((current) => !current)}
            >
              <svg viewBox="0 0 24 24" aria-hidden="true">
                <circle cx="12" cy="8" r="3.5" />
                <path d="M4.5 20c.7-3.2 3.3-5 7.5-5s6.8 1.8 7.5 5" />
              </svg>
            </button>

            {isUserMenuOpen && (
              <div className="user-dropdown">
                <div className="user-dropdown-name">
                  {user.role === 'ADMIN' ? '관리자' : user.nickname}
                </div>
                <button type="button" onClick={() => {}}>
                  즐겨찾기
                </button>
                <button type="button" onClick={() => onOpenMyPage?.()}>
                  마이페이지
                </button>
                {user.role === 'ADMIN' && <button type="button" onClick={() => onOpenAdminPage?.()}>관리자 페이지</button>}
                <button type="button" onClick={handleLogout}>
                  로그아웃
                </button>
              </div>
            )}
          </div>
        ) : (
          <button
            className="menu-btn login-btn"
            type="button"
            onClick={onLoginClick}
          >
            로그인
          </button>
        )}
      </div>
    </header>
  );
}

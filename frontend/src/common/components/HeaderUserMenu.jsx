import { useDropdown } from '@/common/hooks/useDropdown.js';

export default function HeaderUserMenu({ user, logout, goLogin, onNavigate }) {
    const { isOpen, setIsOpen, ref } = useDropdown();

    const handleMenuClick = (path) => {
        setIsOpen(false);
        onNavigate(path);
    };

    const handleLogoutClick = () => {
        setIsOpen(false);
        logout();
    };

    if (!user) {
        return (
            <button className="menu-btn login-btn" type="button" onClick={goLogin}>
                로그인
            </button>
        );
    }

    return (
        <div className="user-menu" ref={ref}>
            <button
                type="button"
                className={`person-button ${isOpen ? 'active' : ''}`}
                aria-label="사용자 메뉴 열기"
                aria-expanded={isOpen}
                onClick={() => setIsOpen((current) => !current)}
            >
                <svg viewBox="0 0 24 24" aria-hidden="true">
                    <circle cx="12" cy="8" r="3.5" />
                    <path d="M4.5 20c.7-3.2 3.3-5 7.5-5s6.8 1.8 7.5 5" />
                </svg>
            </button>

            {isOpen && (
                <div className="user-dropdown">
                    <div className="user-dropdown-name">
                        {user.role === 'ADMIN' ? '관리자' : user.nickname}
                    </div>
                    <button type="button" onClick={() => handleMenuClick('/favorites')}>찜한 목록</button>
                    <button type="button" onClick={() => handleMenuClick('/mypage')}>마이페이지</button>
                    {user.role === 'ADMIN' && (
                        <button type="button" onClick={() => handleMenuClick('/admin')}>관리자 페이지</button>
                    )}
                    <button type="button" onClick={handleLogoutClick}>로그아웃</button>
                </div>
            )}
        </div>
    );
}
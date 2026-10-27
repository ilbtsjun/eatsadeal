import { Link, useNavigate } from 'react-router';
import { useAuth } from '@/auth/useAuth.js';
import { useGoLogin } from '@/common/hooks/useGoLogin.js';
import HeaderSearch from './HeaderSearch.jsx';
import HeaderUserMenu from './HeaderUserMenu.jsx';
import './Header.css';

export default function Header({ searchKeyword = '', onSearch, showSearch = false }) {
    const { user, logout } = useAuth();
    const navigate = useNavigate();
    const goLogin = useGoLogin();

    return (
        <header className="site-header">
            <div className="header-logo">
                <Link to="/">이츠어딜!</Link>
            </div>
            {showSearch && (
                <HeaderSearch searchKeyword={searchKeyword} onSearch={onSearch} />
            )}

            <div className="header-menu">
                <HeaderUserMenu
                    user={user}
                    logout={logout}
                    goLogin={goLogin}
                    onNavigate={(path) => navigate(path)}
                />
            </div>
        </header>
    );
}
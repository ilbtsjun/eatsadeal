const TABS = [
    { key: 'brand', label: '브랜드 관리' },
    { key: 'category', label: '카테고리 관리' },
    { key: 'user', label: '회원 관리' },
    { key: 'crawler', label: '크롤링 관리' },
];

export default function AdminTabs({ tab, onChange }) {
    return (
        <nav className="admin-tabs" aria-label="관리 항목">
            {TABS.map(({ key, label }) => (
                <button
                    key={key}
                    className={tab === key ? 'active' : ''}
                    type="button"
                    aria-current={tab === key ? 'page' : undefined}
                    onClick={() => onChange(key)}
                >
                    {label}
                </button>
            ))}
        </nav>
    );
}
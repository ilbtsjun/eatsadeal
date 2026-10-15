const CRAWL_ALL = { path: '/api/crawl', label: '전체' };
const CRAWL_GROUPS = [
    {
        key: 'chicken',
        label: '치킨',
        path: '/api/crawl/chicken',
        brands: [
            { path: '/api/crawl/bhc', label: 'BHC' },
            { path: '/api/crawl/bbq', label: 'BBQ' },
            { path: '/api/crawl/kyochon', label: '교촌' },
            { path: '/api/crawl/pelicana', label: '페리카나' },
            { path: '/api/crawl/goobne', label: '굽네' },
        ],
    },
    {
        key: 'pizza',
        label: '피자',
        path: '/api/crawl/pizza',
        brands: [
            { path: '/api/crawl/dominos', label: '도미노' },
            { path: '/api/crawl/papajohns', label: '파파존스' },
            { path: '/api/crawl/pizzamaru', label: '피자마루' },
            { path: '/api/crawl/pizzaetang', label: '피자에땅' },
            { path: '/api/crawl/pizzaschool', label: '피자스쿨' },
        ],
    },
    {
        key: 'hamburger',
        label: '햄버거',
        path: '/api/crawl/hamburger',
        brands: [
            { path: '/api/crawl/burgerking', label: '버거킹' },
            { path: '/api/crawl/frankburger', label: '프랭크버거' },
            { path: '/api/crawl/kfc', label: 'KFC' },
            { path: '/api/crawl/lotteria', label: '롯데리아' },
            { path: '/api/crawl/momstouch', label: '맘스터치' },
        ],
    },
];

export default function CrwalerManagement({crawlerLoading, onRunCrawler,}){
    return(
        <section className="admin-panel">
            <div className="panel-heading">
                <div>
                    <h2>크롤링 관리</h2>
                    <p className="panel-description">외부 브랜드의 이벤트를 수집해 이벤트 목록에 반영합니다.</p>
                </div>
            </div>

            <div className="crawler-actions">
                <button type="button"
                        disabled={crawlerLoading}
                        onClick={() => onRunCrawler(CRAWL_ALL.path, CRAWL_ALL.label)}>
                    {CRAWL_ALL.label} 크롤링
                </button>

                {CRAWL_GROUPS.map((group) => (
                    <button type="button"
                            key={group.key}
                            disabled={crawlerLoading}
                            onClick={() => onRunCrawler(group.path, group.label)}>
                        {group.label}
                        크롤링
                    </button>
                ))}

                {CRAWL_GROUPS.map((group) => (
                    <div className="crawler-brand-actions" key={group.key}>
                        <strong>{group.label} 브랜드별</strong>
                        {group.brands.map((brand) => (
                            <button type="button"
                                    key={brand.path}
                                    disabled={crawlerLoading}
                                    onClick={() => onRunCrawler(brand.path, brand.label)}
                            >
                                {brand.label}
                            </button>
                        ))}
                    </div>
                ))}
            </div>

            {crawlerLoading && (
                <p className="admin-empty">
                    크롤링 중입니다. 완료될 때까지 잠시 기다려주세요.
                </p>
            )}
        </section>
    )
}
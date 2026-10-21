import { useEffect } from 'react';

const STATUS_LABEL = {
    SUCCESS: '성공',
    PARTIAL: '일부 실패',
    EMPTY: '수집 0건',
    FAILED: '실패',
    SKIPPED_INACTIVE: '건너뜀(비활성)',
};

const formatTime = (value) => (value ? new Date(value).toLocaleString('ko-KR') : '-');

function StatusBadge({ status }) {
    return (
        <span className={`crawl-status crawl-status-${String(status).toLowerCase()}`}>
            {STATUS_LABEL[status] ?? status}
        </span>
    );
}

function CrawlRunTable({ runs, showTime }) {
    return (
        <div className="crawl-run-table-wrap">
            <table className="crawl-run-table">
                <thead>
                    <tr>
                        {showTime && <th>시각</th>}
                        <th>브랜드</th>
                        <th>상태</th>
                        <th>수집</th>
                        <th>신규</th>
                        <th>변경</th>
                        <th>동일</th>
                        <th>오류</th>
                        <th>소요(초)</th>
                        <th>메시지</th>
                    </tr>
                </thead>
                <tbody>
                    {runs.map((run) => (
                        <tr key={run.id}>
                            {showTime && <td>{formatTime(run.startedAt)}</td>}
                            <td>{run.brandKey}</td>
                            <td><StatusBadge status={run.status} /></td>
                            <td>{run.collectedCount}</td>
                            <td>{run.createdCount ?? '-'}</td>
                            <td>{run.updatedCount ?? '-'}</td>
                            <td>{run.unchangedCount ?? '-'}</td>
                            <td>{run.errorCount}</td>
                            <td>{(run.elapsedMs / 1000).toFixed(1)}</td>
                            <td className="crawl-run-message">{run.message ?? ''}</td>
                        </tr>
                    ))}
                </tbody>
            </table>
        </div>
    );
}

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

export default function CrwalerManagement({
    crawlerLoading,
    onRunCrawler,
    latestRuns = [],
    recentRuns = [],
    runsLoading = false,
    onLoadRuns,
}){
    useEffect(() => {
        onLoadRuns?.();
    }, [onLoadRuns]);

    const counts = latestRuns.reduce((acc, run) => {
        acc[run.status] = (acc[run.status] ?? 0) + 1;
        return acc;
    }, {});
    const firstRun = latestRuns[0];
    const lastRun = latestRuns[latestRuns.length - 1];

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

            <div className="crawl-run-section">
                <div className="crawl-run-heading">
                    <h3>최근 실행 결과</h3>
                    <button type="button" disabled={runsLoading} onClick={onLoadRuns}>
                        새로고침
                    </button>
                </div>

                {latestRuns.length === 0 ? (
                    <p className="admin-empty">
                        {runsLoading ? '불러오는 중입니다...' : '아직 기록된 크롤링이 없습니다.'}
                    </p>
                ) : (
                    <>
                        <p className="panel-description">
                            {formatTime(firstRun.startedAt)} ~ {formatTime(lastRun.finishedAt)}
                        </p>
                        <div className="crawl-run-summary">
                            {Object.keys(STATUS_LABEL).map((status) => (
                                counts[status] ? (
                                    <span key={status} className={`crawl-status crawl-status-${status.toLowerCase()}`}>
                                        {STATUS_LABEL[status]} {counts[status]}
                                    </span>
                                ) : null
                            ))}
                        </div>
                        <CrawlRunTable runs={latestRuns} />
                    </>
                )}
            </div>

            {recentRuns.length > 0 && (
                <details className="crawl-run-section">
                    <summary>실행 이력 (최근 {recentRuns.length}건)</summary>
                    <CrawlRunTable runs={recentRuns} showTime />
                </details>
            )}
        </section>
    )
}
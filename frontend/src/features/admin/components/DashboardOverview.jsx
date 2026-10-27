import { Link } from 'react-router';
import { formatDateTime } from '@/common/utils/DateUtil.js';
import { useAdminDashboard } from '@/features/admin/hooks/useAdminDashboard.js';

function StatCard({ label, value, tone }) {
    return (
        <div className={`stat-card${tone ? ` ${tone}` : ''}`}>
            <span>{label}</span>
            <strong>{value.toLocaleString()}</strong>
        </div>
    );
}

export default function DashboardOverview() {
    const { dashboard, loading, error, loadDashboard } = useAdminDashboard();

    if (loading) {
        return <p className="admin-empty">대시보드를 불러오는 중입니다...</p>;
    }

    if (error || !dashboard) {
        return (
            <section className="admin-panel">
                <p className="admin-empty">대시보드를 불러오지 못했습니다. {error}</p>
                <div className="dashboard-retry">
                    <button type="button" className="create-placeholder" onClick={loadDashboard}>다시 시도</button>
                </div>
            </section>
        );
    }

    const { events, crawl, members, topEvents, brandEventCounts } = dashboard;
    const aiPercent = crawl.aiDailyLimit > 0
        ? Math.min(100, Math.round((crawl.aiCallsToday / crawl.aiDailyLimit) * 100))
        : 0;
    const maxBrandCount = Math.max(1, ...brandEventCounts.map((brand) => brand.eventCount));

    return (
        <>
            <section className="admin-panel">
                <div className="panel-heading">
                    <div>
                        <h2>이벤트</h2>
                        <p className="panel-description">지금 노출 중인 이벤트와 손볼 이벤트 수입니다.</p>
                    </div>
                    <button type="button" className="create-placeholder" onClick={loadDashboard}>새로고침</button>
                </div>
                <div className="stat-grid">
                    <StatCard label="노출 중" value={events.visible} />
                    <StatCard label="이번 주 시작" value={events.startedThisWeek} />
                    <StatCard label="3일 안에 마감" value={events.endingSoon} />
                    <StatCard label="숨김" value={events.hidden} />
                    <StatCard label="잠금" value={events.locked} />
                    <StatCard label="코드 없음" value={events.withoutEventCode} tone={events.withoutEventCode ? 'warn' : ''} />
                    <StatCard label="설명 없음" value={events.withoutDescription} tone={events.withoutDescription ? 'warn' : ''} />
                </div>
            </section>

            <section className="admin-panel">
                <h2>크롤링</h2>
                <p className="panel-description">
                    마지막 실행: {crawl.lastRunAt ? formatDateTime(crawl.lastRunAt) : '기록 없음'}
                </p>
                <div className="stat-grid">
                    <StatCard label="성공 브랜드" value={crawl.successCount} />
                    <StatCard label="실패·0건 브랜드" value={crawl.failedCount} tone={crawl.failedCount ? 'danger' : ''} />
                </div>
                {crawl.failedBrands.length > 0 && (
                    <div className="dashboard-tags">
                        {crawl.failedBrands.map((brand) => (
                            <span key={brand}>{brand}</span>
                        ))}
                    </div>
                )}
                <div className="dashboard-usage">
                    <div>
                        <span>오늘 AI 사용량</span>
                        <strong>{crawl.aiCallsToday} / {crawl.aiDailyLimit}회</strong>
                    </div>
                    <div className="bar-track">
                        <div className={`bar-fill${aiPercent >= 90 ? ' danger' : ''}`} style={{ width: `${aiPercent}%` }} />
                    </div>
                </div>
            </section>

            <section className="admin-panel">
                <h2>회원·댓글</h2>
                <div className="stat-grid">
                    <StatCard label="전체 회원" value={members.totalUsers} />
                    <StatCard label="이번 주 가입" value={members.newUsersThisWeek} />
                    <StatCard label="정지 회원" value={members.suspendedUsers} />
                    <StatCard label="전체 댓글" value={members.totalComments} />
                    <StatCard label="이번 주 댓글" value={members.newCommentsThisWeek} />
                </div>
            </section>

            <div className="dashboard-columns">
                <section className="admin-panel">
                    <h2>조회수 상위 5개</h2>
                    {topEvents.length === 0 ? (
                        <p className="admin-empty">노출 중인 이벤트가 없습니다.</p>
                    ) : (
                        <ol className="dashboard-ranking">
                            {topEvents.map((event, index) => (
                                <li key={event.id}>
                                    <em>{index + 1}</em>
                                    <div>
                                        <Link to={`/events/${event.id}`}>{event.title}</Link>
                                        <span>{event.brandName}</span>
                                    </div>
                                    <strong>{event.viewCount.toLocaleString()}</strong>
                                </li>
                            ))}
                        </ol>
                    )}
                </section>

                <section className="admin-panel">
                    <h2>브랜드별 이벤트 수</h2>
                    {brandEventCounts.length === 0 ? (
                        <p className="admin-empty">노출 중인 이벤트가 없습니다.</p>
                    ) : (
                        <div className="dashboard-bars">
                            {brandEventCounts.map((brand) => (
                                <div key={brand.brandName} className="bar-row">
                                    <span>{brand.brandName}</span>
                                    <div className="bar-track">
                                        <div className="bar-fill" style={{ width: `${(brand.eventCount / maxBrandCount) * 100}%` }} />
                                    </div>
                                    <strong>{brand.eventCount}</strong>
                                </div>
                            ))}
                        </div>
                    )}
                </section>
            </div>
        </>
    );
}

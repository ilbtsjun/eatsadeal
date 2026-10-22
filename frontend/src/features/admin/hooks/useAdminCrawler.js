import { useCallback, useState } from 'react';
import { runCrawler as run, getLatestCrawlRuns, getCrawlRuns } from '@/api/index.js'

export function useAdminCrawler(setMessage) {
    const [crawlerLoading, setCrawlerLoading] = useState(false);
    const [latestRuns, setLatestRuns] = useState([]);
    const [recentRuns, setRecentRuns] = useState([]);
    const [runsLoading, setRunsLoading] = useState(false);

    const loadRuns = useCallback(async () => {
        setRunsLoading(true);
        try {
            const [latest, recent] = await Promise.all([getLatestCrawlRuns(), getCrawlRuns(50)]);
            setLatestRuns(latest ?? []);
            setRecentRuns(recent ?? []);
        } catch (error) {
            setMessage(`크롤링 이력을 불러오지 못했습니다: ${error.message}`);
        } finally {
            setRunsLoading(false);
        }
    }, [setMessage]);

    const runCrawler = async (path, label) => {
        setCrawlerLoading(true);
        setMessage(`${label} 크롤링을 실행하는 중입니다...`);

        try {
            const result = await run(path);
            const { saved = 0, skipped = [], failed = [], partial = [], empty = [] } = result ?? {};

            const parts = [`${saved}건 수집`];
            if (skipped.length) parts.push(`건너뜀(비활성 브랜드): ${skipped.join(', ')}`);
            if (failed.length) parts.push(`실패: ${failed.join(', ')}`);
            if (partial.length) parts.push(`일부 실패: ${partial.join(', ')}`);
            if (empty.length) parts.push(`수집 0건: ${empty.join(', ')}`);

            window.dispatchEvent(new CustomEvent('eatsadeal:events-updated'));
            setMessage(`${label} 크롤링 완료 - ${parts.join(' / ')}`);
            loadRuns();
        } catch (error) {
            setMessage(`${label} 크롤링에 실패했습니다: ${error.message}`);
        } finally {
            setCrawlerLoading(false);
        }
    };

    return { crawlerLoading, runCrawler, latestRuns, recentRuns, runsLoading, loadRuns };
}
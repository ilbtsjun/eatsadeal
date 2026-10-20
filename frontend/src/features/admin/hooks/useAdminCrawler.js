import { useState } from 'react';
import { runCrawler as run } from '@/api/index.js'

export function useAdminCrawler(setMessage) {
    const [crawlerLoading, setCrawlerLoading] = useState(false);

    const runCrawler = async (path, label) => {
        setCrawlerLoading(true);
        setMessage(`${label} 크롤링을 실행하는 중입니다...`);

        try {
            const result = await run(path);
            const { saved = 0, skipped = [], failed = [] } = result ?? {};

            const parts = [`${saved}건 수집`];
            if (skipped.length) parts.push(`건너뜀(비활성 브랜드): ${skipped.join(', ')}`);
            if (failed.length) parts.push(`실패: ${failed.join(', ')}`);

            window.dispatchEvent(new CustomEvent('eatsadeal:events-updated'));
            setMessage(`${label} 크롤링 완료 - ${parts.join(' / ')}`);
        } catch (error) {
            setMessage(`${label} 크롤링에 실패했습니다: ${error.message}`);
        } finally {
            setCrawlerLoading(false);
        }
    };

    return { crawlerLoading, runCrawler };
}
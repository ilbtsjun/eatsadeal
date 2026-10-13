import { useState } from 'react';
import { apiRequest } from '@/api/client';
import { runCrawler as run } from '@/api/index.js'

const request = apiRequest;

export function useAdminCrawler(setMessage) {
    const [crawlerLoading, setCrawlerLoading] = useState(false);

    const runCrawler = async (path, label) => {
        setCrawlerLoading(true);
        setMessage(`${label} 크롤링을 실행하는 중입니다...`);

        try {
            const result = await run(path);
            const count = Array.isArray(result)
                ? ` ${result.length}건을 수집했습니다.`
                : ' 완료되었습니다.';

            window.dispatchEvent(new CustomEvent('eatsadeal:events-updated'));
            setMessage(`${label} 크롤링${count} 이벤트 목록에서 확인할 수 있습니다.`);
        } catch (error) {
            setMessage(`${label} 크롤링에 실패했습니다: ${error.message}`);
        } finally {
            setCrawlerLoading(false);
        }
    };

    return { crawlerLoading, runCrawler };
}
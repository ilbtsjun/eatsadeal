import { useEffect } from 'react';

const SITE_NAME = '이츠어딜!';
const SITE_URL = 'https://www.eatsadeal.kr';
const DEFAULT_TITLE = `${SITE_NAME} - 프랜차이즈 할인 이벤트 모음`;
const DEFAULT_DESCRIPTION = '치킨, 피자, 햄버거 프랜차이즈의 할인·1+1·쿠폰 이벤트를 한곳에서 확인하세요.';

function setMeta(selector, value) {
    const element = document.head.querySelector(selector);
    if (element) {
        element.setAttribute(selector.startsWith('link') ? 'href' : 'content', value);
    }
}

export function usePageTitle(title, { description, path } = {}) {
    useEffect(() => {
        const fullTitle = title ? `${title} | ${SITE_NAME}` : DEFAULT_TITLE;
        const fullDescription = description?.trim() || DEFAULT_DESCRIPTION;
        const url = `${SITE_URL}${path ?? window.location.pathname}`;

        document.title = fullTitle;
        setMeta('meta[name="description"]', fullDescription);
        setMeta('meta[property="og:title"]', fullTitle);
        setMeta('meta[property="og:description"]', fullDescription);
        setMeta('meta[property="og:url"]', url);
        setMeta('link[rel="canonical"]', url);
    }, [title, description, path]);
}

export const CRAWL_ALL = { path: '/api/crawl', label: '전체' };
export const CRAWL_GROUPS = [
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

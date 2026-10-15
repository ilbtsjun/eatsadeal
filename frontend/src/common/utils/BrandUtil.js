const BRAND_CATEGORY_MAP = {
    // 치킨 브랜드
    'BHC': 'chicken', 'BBQ': 'chicken', 'Kyochon': 'chicken', 'Pelicana': 'chicken', 'Goobne': 'chicken',
    // 피자 브랜드
    'Dominos': 'pizza', 'Papajohns': 'pizza', 'Pizzamaru': 'pizza', 'Pizzaettang': 'pizza', 'Pizzaschool': 'pizza',
    // 햄버거 브랜드
    'Burgerking': 'hamburger', 'Frankburger': 'hamburger', 'KFC': 'hamburger', 'Lotteria': 'hamburger', 'Momstouch': 'hamburger',
};

const FALLBACK_IMAGE = {
    chicken : '/images/default-chicken.webp',
    pizza : '/images/default-pizza.webp',
    hamburger : '/images/default-hamburger.webp'
}

export const getFallbackImage = (brandName) => FALLBACK_IMAGE[BRAND_CATEGORY_MAP[brandName]] ?? null;


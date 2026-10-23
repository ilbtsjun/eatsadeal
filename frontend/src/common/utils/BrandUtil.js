// 키는 소문자로 둔다. DB 브랜드명(KyoChonChicken, PizzaSchool 등)과 대소문자가 달라도 찾을 수 있게 한다.
const BRAND_CATEGORY_MAP = {
    // 치킨 브랜드
    'bhc': 'chicken', 'bbq': 'chicken', 'kyochonchicken': 'chicken', 'pelicana': 'chicken', 'goobne': 'chicken',
    // 피자 브랜드
    'dominos': 'pizza', 'papajohns': 'pizza', 'pizzamaru': 'pizza', 'pizzaetang': 'pizza', 'pizzaschool': 'pizza',
    // 햄버거 브랜드
    'burgerking': 'hamburger', 'frankburger': 'hamburger', 'kfc': 'hamburger', 'lotteria': 'hamburger', 'momstouch': 'hamburger',
};

const FALLBACK_IMAGE = {
    chicken : '/images/default-chicken.webp',
    pizza : '/images/default-pizza.webp',
    hamburger : '/images/default-hamburger.webp'
}

// 모르는 브랜드여도 깨진 이미지 대신 기본 이미지를 보여준다.
export const getFallbackImage = (brandName) =>
    FALLBACK_IMAGE[BRAND_CATEGORY_MAP[String(brandName ?? '').toLowerCase()]] ?? FALLBACK_IMAGE.chicken;

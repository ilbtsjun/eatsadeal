const EVENT_CODE_NAMES = {
    DISCOUNT_PRICE: '정액 할인',
    DISCOUNT_RATE: '정률 할인',
    BUY_ONE_GET_ONE: '1+1',
    BUY_N_GET_N: 'N+1',
    TAKE_OUT: '포장 할인',
    DELIVERY_FREE: '배달비 무료',
    GIFT_PROMO: '사은품',
    PAYMENT_PROMO: '결제 할인',
    MEMBERSHIP: '멤버십',
    TIME_SALE: '타임세일',
};

export function getEventCodeName(eventCodes, { all = false } = {}) {
    if (!eventCodes?.length) return '이벤트';

    const names = eventCodes.map(
        (code) => EVENT_CODE_NAMES[code] || '이벤트'
    );

    return all ? names.join(' · ') : names[0];
}
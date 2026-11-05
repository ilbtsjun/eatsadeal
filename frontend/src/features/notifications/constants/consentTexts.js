export const CONSENT_SOURCE = {
    SUBSCRIBE_POPUP: 'SUBSCRIBE_POPUP',
    MYPAGE: 'MYPAGE',
};

export const CONSENT_ITEMS = [
    {
        key: 'marketingAgreed',
        agreedAtKey: 'marketingAgreedAt',
        title: '마케팅 목적 개인정보 이용 동의 (선택)',
        description: '구독한 브랜드와 이용 기록을 바탕으로 맞춤 이벤트 소식을 안내하는 데 이용해요.',
    },
    {
        key: 'emailAgreed',
        agreedAtKey: 'emailAgreedAt',
        title: '광고성 정보 이메일 수신 동의 (선택)',
        description: '구독한 브랜드의 새 이벤트와 마감 임박 소식을 이메일로 받아요.',
    },
];

export const CONSENT_NOTICE = '두 항목 모두 동의해야 알림 메일을 받을 수 있어요. 동의는 마이페이지에서 언제든 바꿀 수 있어요.';

export const CONSENT_PROMPT_DISMISS_KEY = 'consentPromptDismissed';

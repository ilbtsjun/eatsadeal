import { CONSENT_PROMPT_DISMISS_KEY } from '../constants/consentTexts.js';

const keyOf = (userId) => `${CONSENT_PROMPT_DISMISS_KEY}:${userId ?? ''}`;

export const isConsentPromptDismissed = (userId) => {
    try {
        return sessionStorage.getItem(keyOf(userId)) === '1';
    } catch {
        return false;
    }
};

export const markConsentPromptDismissed = (userId) => {
    try {
        sessionStorage.setItem(keyOf(userId), '1');
    } catch {
        // sessionStorage를 쓸 수 없으면 이번만 닫는다
    }
};

export const clearConsentPromptDismissed = () => {
    try {
        Object.keys(sessionStorage)
            .filter((key) => key.startsWith(`${CONSENT_PROMPT_DISMISS_KEY}:`))
            .forEach((key) => sessionStorage.removeItem(key));
    } catch {
        // sessionStorage를 쓸 수 없으면 지울 기록도 없다
    }
};

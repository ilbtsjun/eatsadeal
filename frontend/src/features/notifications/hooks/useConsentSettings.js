import { useState } from 'react';
import { getMyConsents, updateMyConsents } from '@/api/index.js';
import { useFetch } from '@/common/hooks/useFetch.js';
import { CONSENT_SOURCE } from '../constants/consentTexts.js';

export function useConsentSettings() {
    const {
        data: consents,
        setData: setConsents,
        loading,
        loadError,
        reload,
    } = useFetch(getMyConsents, { fallbackMessage: '동의 정보를 불러오지 못했습니다.' });
    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState('');

    const toggleConsent = async (key, checked) => {
        if (saving || !consents) return;

        setSaving(true);
        setSaveError('');

        try {
            const updated = await updateMyConsents({
                marketingAgreed: consents.marketingAgreed,
                emailAgreed: consents.emailAgreed,
                [key]: checked,
                source: CONSENT_SOURCE.MYPAGE,
            });
            setConsents(updated);
        } catch (requestError) {
            setSaveError(requestError.message || '동의 설정을 저장하지 못했습니다.');
        } finally {
            setSaving(false);
        }
    };

    return { consents, loading, loadError, reload, saving, saveError, toggleConsent };
}

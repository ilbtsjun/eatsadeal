import { useEffect, useState } from 'react';
import { getMyConsents, updateMyConsents } from '@/api/index.js';
import { CONSENT_SOURCE } from '../constants/consentTexts.js';

export function useConsentSettings() {
    const [consents, setConsents] = useState(null);
    const [loading, setLoading] = useState(true);
    const [error, setError] = useState('');
    const [reloadKey, setReloadKey] = useState(0);
    const [saving, setSaving] = useState(false);
    const [saveError, setSaveError] = useState('');

    useEffect(() => {
        let cancelled = false;

        getMyConsents()
            .then((data) => {
                if (cancelled) return;
                setConsents(data);
                setError('');
            })
            .catch((requestError) => {
                if (!cancelled) setError(requestError.message);
            })
            .finally(() => {
                if (!cancelled) setLoading(false);
            });

        return () => { cancelled = true; };
    }, [reloadKey]);

    const reload = () => {
        setLoading(true);
        setError('');
        setReloadKey((key) => key + 1);
    };

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

    return { consents, loading, error, reload, saving, saveError, toggleConsent };
}

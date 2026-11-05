import { useState } from 'react';
import { getMyConsents, updateMyConsents } from '@/api/index.js';
import { CONSENT_SOURCE } from '../constants/consentTexts.js';
import { isConsentPromptDismissed, markConsentPromptDismissed } from '../utils/consentPromptStorage.js';

const EMPTY_DRAFT = { marketingAgreed: false, emailAgreed: false };

export function useConsentPrompt(user) {
    const [isOpen, setIsOpen] = useState(false);
    const [draft, setDraft] = useState(EMPTY_DRAFT);
    const [saving, setSaving] = useState(false);
    const [error, setError] = useState('');

    const canSave = draft.marketingAgreed || draft.emailAgreed;

    const promptAfterSubscribe = () => {
        if (!user || isOpen || isConsentPromptDismissed(user.id)) return;

        getMyConsents()
            .then((current) => {
                if (current.marketingAgreed && current.emailAgreed) return;
                setDraft({
                    marketingAgreed: Boolean(current.marketingAgreed),
                    emailAgreed: Boolean(current.emailAgreed),
                });
                setError('');
                setIsOpen(true);
            })
            .catch(() => {});
    };

    const toggleDraft = (key, checked) => {
        setDraft((current) => ({ ...current, [key]: checked }));
        setError('');
    };

    const save = async () => {
        if (saving || !canSave) return;

        setSaving(true);
        setError('');

        try {
            await updateMyConsents({ ...draft, source: CONSENT_SOURCE.SUBSCRIBE_POPUP });
            setIsOpen(false);
        } catch (requestError) {
            setError(requestError.message || '동의 정보를 저장하지 못했습니다.');
        } finally {
            setSaving(false);
        }
    };

    const dismiss = () => {
        if (saving) return;
        markConsentPromptDismissed(user?.id);
        setIsOpen(false);
    };

    return { isOpen, draft, saving, error, canSave, promptAfterSubscribe, toggleDraft, save, dismiss };
}

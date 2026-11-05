import StatusMessage from '@/common/components/StatusMessage.jsx';
import { formatDateTime } from '@/common/utils/DateUtil.js';
import { CONSENT_ITEMS, CONSENT_NOTICE } from '../constants/consentTexts.js';
import { useConsentSettings } from '../hooks/useConsentSettings.js';
import './ConsentSettings.css';

export default function ConsentSettings() {
    const { consents, loading, loadError, reload, saving, saveError, toggleConsent } = useConsentSettings();

    return (
        <section className="mypage-panel consent-settings">
            <h2>알림 수신 동의</h2>
            <p className="consent-settings-notice">{CONSENT_NOTICE}</p>

            {loading && <StatusMessage type="loading" size="inline" message="동의 정보를 불러오는 중입니다." />}
            {!loading && loadError && (
                <StatusMessage type="error" size="inline" message={loadError} onRetry={reload} />
            )}
            {!loading && !loadError && consents && (
                <ul className="consent-list">
                    {CONSENT_ITEMS.map((item) => {
                        const agreed = consents[item.key];
                        return (
                            <li className="consent-item" key={item.key}>
                                <div className="consent-item-text">
                                    <strong>{item.title}</strong>
                                    <p>{item.description}</p>
                                    <span className={`consent-item-status ${agreed ? 'is-agreed' : ''}`}>
                                        {agreed ? `${formatDateTime(consents[item.agreedAtKey])} 동의` : '동의하지 않음'}
                                    </span>
                                </div>
                                <label className="consent-switch">
                                    <input
                                        type="checkbox"
                                        role="switch"
                                        checked={agreed}
                                        disabled={saving}
                                        aria-label={item.title}
                                        onChange={(event) => toggleConsent(item.key, event.target.checked)}
                                    />
                                    <span className="consent-switch-track" aria-hidden="true" />
                                </label>
                            </li>
                        );
                    })}
                </ul>
            )}
            {saveError && <p className="consent-error" role="alert">{saveError}</p>}
        </section>
    );
}

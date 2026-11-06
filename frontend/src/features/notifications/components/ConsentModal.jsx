import Modal from '@/common/components/Modal.jsx';
import { CONSENT_ITEMS, CONSENT_NOTICE } from '../constants/consentTexts.js';
import './ConsentModal.css';

export default function ConsentModal({ draft, saving, error, canSave, onToggle, onSave, onDismiss }) {
    return (
        <Modal backdropClassName="consent-backdrop" onClose={onDismiss} locked={saving}>
            <div className="consent-dialog" role="dialog" aria-modal="true" aria-labelledby="consent-title">
                <h2 id="consent-title">구독한 브랜드 소식을 받아볼까요?</h2>
                <p className="consent-dialog-intro">
                    새 이벤트와 마감 임박 소식을 이메일로 알려드려요.
                    <br />{CONSENT_NOTICE}
                </p>

                <ul className="consent-check-list">
                    {CONSENT_ITEMS.map((item) => (
                        <li key={item.key}>
                            <label className="consent-check">
                                <input
                                    type="checkbox"
                                    checked={draft[item.key]}
                                    disabled={saving}
                                    onChange={(event) => onToggle(item.key, event.target.checked)}
                                />
                                <span className="consent-check-text">
                                    <strong>{item.title}</strong>
                                    <small>{item.description}</small>
                                </span>
                            </label>
                        </li>
                    ))}
                </ul>

                <a className="consent-policy-link" href="/privacy" target="_blank" rel="noreferrer">
                    개인정보처리방침 보기
                </a>

                {error && <p className="consent-error" role="alert">{error}</p>}

                <div className="consent-dialog-actions">
                    <button type="button" className="consent-later" onClick={onDismiss} disabled={saving}>
                        다음에
                    </button>
                    <button type="button" className="consent-save" onClick={onSave} disabled={!canSave || saving}>
                        {saving ? '저장 중...' : '저장'}
                    </button>
                </div>
            </div>
        </Modal>
    );
}

import { useId } from 'react';
import Modal from './Modal.jsx';
import './ConfirmDialog.css';

export default function ConfirmDialog({
                                          title,
                                          message,
                                          confirmLabel = '확인',
                                          pendingLabel = '처리 중...',
                                          pending = false,
                                          error = '',
                                          onConfirm,
                                          onClose,
                                      }) {
    const titleId = useId();

    return (
        <Modal backdropClassName="confirm-backdrop" onClose={onClose} locked={pending}>
            <div className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby={titleId}>
                <h2 id={titleId}>{title}</h2>
                <p>{message}</p>
                {error && <p className="confirm-error" role="alert">{error}</p>}
                <div className="confirm-dialog-actions">
                    <button type="button" className="confirm-cancel" disabled={pending} onClick={onClose}>취소</button>
                    <button type="button" className="confirm-delete" disabled={pending} onClick={onConfirm}>
                        {pending ? pendingLabel : confirmLabel}
                    </button>
                </div>
            </div>
        </Modal>
    );
}

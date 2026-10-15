import Modal from '@/common/components/Modal.jsx';

export default function ConfirmActionModal({
                                               confirmDelete,
                                               confirmText,
                                               onClose,
                                               onConfirm,
                                           }) {
    if (!confirmDelete || !confirmText) return null;

    return (
        <Modal backdropClassName="admin-modal-backdrop" onClose={onClose}>
            <div className="admin-confirm-modal" role="dialog" aria-modal="true" aria-labelledby="delete-title">
                <div className="modal-heading">
                    <h2 id="delete-title">{confirmText.title}</h2>
                    <button type="button" onClick={onClose}>×</button>
                </div>
                <p><strong>{confirmDelete.name}</strong>{confirmText.question}</p>
                {confirmText.note && <span>{confirmText.note}</span>}
                <div className="confirm-actions">
                    <button type="button" onClick={onClose}>취소</button>
                    <button
                        type="button"
                        className={confirmDelete.kind === 'activate' ? 'primary-confirm' : 'danger-confirm'}
                        onClick={onConfirm}
                    >
                        {confirmText.button}
                    </button>
                </div>
            </div>
        </Modal>
    );
}

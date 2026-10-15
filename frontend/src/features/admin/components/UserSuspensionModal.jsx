import Modal from '@/common/components/Modal.jsx';

export default function UserSuspensionModal({
                                                suspendModal,
                                                selectedUser,
                                                suspendError,
                                                suspendSubmitting,
                                                onClose,
                                                onSubmit,
                                                onDaysChange,
                                                onReasonChange,
                                            }) {
    if (!suspendModal) return null;

    const isReleaseModal = suspendModal.mode === 'release';

    return (
        <Modal
            backdropClassName="admin-modal-backdrop"
            onClose={onClose}
            locked={suspendSubmitting}
        >
            <form
                className="admin-modal"
                role="dialog"
                aria-modal="true"
                aria-labelledby="suspend-title"
                onSubmit={onSubmit}
            >
                <div className="modal-heading">
                    <h2 id="suspend-title">{isReleaseModal ? '정지 해제' : '회원 정지'}</h2>
                    <button type="button" onClick={onClose}>×</button>
                </div>
                <p className="suspend-target">
                    대상: <strong>{selectedUser?.nickname || `회원 ${selectedUser?.id}`}</strong>
                </p>
                {!isReleaseModal && (
                    <label>
                        정지 기간 (일)
                        <input
                            type="number"
                            min="1"
                            step="1"
                            value={suspendModal.days}
                            onChange={onDaysChange}
                            disabled={suspendSubmitting}
                            required
                        />
                    </label>
                )}
                <label>
                    {isReleaseModal ? '해제 사유' : '정지 사유'}
                    <textarea
                        rows={3}
                        value={suspendModal.reason}
                        onChange={onReasonChange}
                        placeholder={isReleaseModal ? '정지를 해제하는 사유를 입력하세요' : '정지 사유를 입력하세요'}
                        disabled={suspendSubmitting}
                        required
                    />
                </label>
                {suspendError && <p className="admin-modal-error" role="alert">{suspendError}</p>}
                <button className="modal-submit" type="submit" disabled={suspendSubmitting}>
                    {suspendSubmitting ? '처리 중...' : (isReleaseModal ? '정지 해제' : '정지하기')}
                </button>
            </form>
        </Modal>
    );
}

import { useUserQuit } from '../hooks/useUserQuit.js';
import Modal from '@/common/components/Modal.jsx';
import '@/common/components/ConfirmDialog.css';

export default function UserQuit({ onLogout, onBack }){
    const{
        showQuitModal,
        quitPassword,
        setQuitPassword,
        quitError,
        setQuitError,
        quitLoading,
        quitDone,
        finishQuit,
        openQuitModal,
        closeQuitModal,
        handleQuit
    } = useUserQuit({onLogout, onBack});
    return (
        <>
            <section className="danger-zone">
                <h2>회원탈퇴</h2>
                <p>탈퇴하면 계정 정보를 다시 복구할 수 없습니다.</p>
                <button type="button" onClick={openQuitModal}>회원탈퇴</button>
            </section>
            {showQuitModal &&
                <Modal backdropClassName="confirm-backdrop" onClose={closeQuitModal} locked={quitLoading}>
                    <div className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="quit-title">
                        {quitDone ? (
                            <>
                                <h2 id="quit-title">회원탈퇴 완료</h2>
                                <p>회원탈퇴가 완료되었습니다. 그동안 이용해주셔서 감사합니다.</p>
                                <div className="confirm-dialog-actions">
                                    <button type="button" className="confirm-delete" onClick={finishQuit} autoFocus>확인</button>
                                </div>
                            </>
                        ) : (
                            <form onSubmit={handleQuit}>
                                <h2 id="quit-title">회원탈퇴</h2>
                                <p>탈퇴하면 계정 정보를 다시 복구할 수 없습니다.
                                    <br />계속하려면 비밀번호를 입력해주세요.</p>
                                <input className="confirm-input" type="password"
                                       value={quitPassword}
                                       onChange={(event) => {
                                           setQuitPassword(event.target.value);
                                           setQuitError('');
                                       }}
                                       placeholder="비밀번호"
                                       autoComplete="current-password"
                                       autoFocus disabled={quitLoading} />
                                {quitError &&
                                    <p className="confirm-error" role="alert">{quitError}</p>
                                }
                                <div className="confirm-dialog-actions">
                                    <button type="button"
                                            className="confirm-cancel"
                                            onClick={closeQuitModal}
                                            disabled={quitLoading}>취소</button>
                                    <button type="submit"
                                            className="confirm-delete"
                                            disabled={quitLoading}>
                                        {quitLoading ? '처리 중...' : '탈퇴하기'}
                                    </button>
                                </div>
                            </form>
                        )}
                    </div>
                </Modal>
            }
        </>
    )
}
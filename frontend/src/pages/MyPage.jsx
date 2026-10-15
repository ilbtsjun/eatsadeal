import Header from '@/common/components/Header';
import Modal from '@/common/components/Modal';
import { useMyPage } from '@/features/account/hooks/useMyPage.js';
import UserProfile from '@/features/account/components/UserProfile.jsx';
import UserComments from '@/features/account/components/UserComments.jsx';
import './MyPage.css';

export default function MyPage({ user, onLogout, onBack, onOpenEvent, onUserUpdate }) {
  const {
    passwordForm,
    showQuitModal,
    quitPassword,
    setQuitPassword,
    quitError,
    quitLoading,
    updatePassword,
    openQuitModal,
    closeQuitModal,
    handleQuit,
    passwordMessage,
    passwordError,
    setQuitError,
    handlePasswordChange,
  } = useMyPage({ onLogout, onBack });

  return <div className="mypage">
    <Header />
    <main className="mypage-container">
      <button type="button" className="mypage-back" onClick={onBack}>← 메인으로</button>
      <h1>마이페이지</h1>
      <p className="mypage-intro">내 정보와 활동을 관리할 수 있습니다.</p>
      <UserProfile user={user} onUserUpdate={onUserUpdate}/>
      <section className="mypage-panel">
        <h2>보안 설정</h2>
        <form className="mypage-form" onSubmit={updatePassword}>
          <label>현재 비밀번호
            <input
                name="currentPassword"
                type="password"
                value={passwordForm.currentPassword}
                onChange={handlePasswordChange}
                required
            />
          </label>
          <label>새 비밀번호
            <input
                name="updatePassword"
                type="password"
                value={passwordForm.updatePassword}
                onChange={handlePasswordChange}
                required
            />
          </label>
          <label>새 비밀번호 확인
            <input
                name="passwordConfirm"
                type="password"
                value={passwordForm.passwordConfirm}
                onChange={handlePasswordChange}
                required
            />
          </label>

          {(passwordMessage || passwordError) && (
              <p
                  className={passwordError ? 'mypage-error' : 'mypage-message'}
                  role="status"
              >
                {passwordError || passwordMessage}
              </p>
          )}
          <button type="submit">비밀번호 변경</button>
        </form>
      </section>
      <UserComments user={user} onOpenEvent={onOpenEvent}/>
    {showQuitModal &&
        <Modal backdropClassName="confirm-backdrop" onClose={closeQuitModal} locked={quitLoading}>
          <div className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="quit-title">
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
          </div>
        </Modal>}
    <section className="danger-zone">
      <h2>회원탈퇴</h2>
      <p>탈퇴하면 계정 정보를 다시 복구할 수 없습니다.</p>
      <button type="button" onClick={openQuitModal}>회원탈퇴</button>
    </section>
  </main></div>;
}
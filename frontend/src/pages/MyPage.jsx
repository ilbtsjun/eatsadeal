import Header from '@/common/components/Header';
import Modal from '@/common/components/Modal';
import { useMyPage } from '@/features/account/hooks/useMyPage.js';
import UserProfile from '@/features/account/components/UserProfile.jsx';
import './MyPage.css';

export default function MyPage({ user, onLogout, onBack, onOpenEvent, onUserUpdate }) {
  const {
    passwordForm,
    setPasswordForm,
    comments,
    commentToDelete,
    setCommentToDelete,
    eventTitles,
    setCommentPage,
    currentCommentPage,
    commentPageCount,
    showQuitModal,
    quitPassword,
    setQuitPassword,
    quitError,
    quitLoading,
    update,
    updatePassword,
    openQuitModal,
    closeQuitModal,
    handleQuit,
    deleteComment,
    getCommentId,
    COMMENTS_PER_PAGE,
    passwordMessage,
    passwordError,
    setPasswordMessage,
    setPasswordError,
    setQuitError,
  } = useMyPage({ user, onLogout, onBack });

  return <div className="mypage">
    <Header />
    <main className="mypage-container">
      <button type="button" className="mypage-back" onClick={onBack}>← 메인으로</button>
      <h1>마이페이지</h1>
      <p className="mypage-intro">내 정보와 활동을 관리할 수 있습니다.</p>
      <UserProfile
          user={user}
          onUserUpdate={onUserUpdate}
      />
      <section className="mypage-panel">
        <h2>보안 설정</h2>
        <form className="mypage-form" onSubmit={updatePassword}>
          <label>현재 비밀번호
            <input
                name="currentPassword"
                type="password"
                value={passwordForm.currentPassword}
                onChange={update(setPasswordForm, () => {
                  setPasswordMessage('');
                  setPasswordError('');
                })}
                required
            />
          </label>
          <label>새 비밀번호
            <input
                name="updatePassword"
                type="password"
                value={passwordForm.updatePassword}
                onChange={update(setPasswordForm, () => {
                  setPasswordMessage('');
                  setPasswordError('');
                })}
                required
            />
          </label>
          <label>새 비밀번호 확인
            <input
                name="passwordConfirm"
                type="password"
                value={passwordForm.passwordConfirm}
                onChange={update(setPasswordForm, () => {
                  setPasswordMessage('');
                  setPasswordError('');
                })}
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
      <section className="mypage-panel">
        <h2>내가 작성한 댓글</h2>
        {comments.length ? <>
        {comments.slice(currentCommentPage * COMMENTS_PER_PAGE,
            (currentCommentPage + 1) * COMMENTS_PER_PAGE).map((comment, index) => {
          const number = currentCommentPage * COMMENTS_PER_PAGE + index + 1;
          const eventId = comment.eventId;
          return <article className="my-comment"
                          key={getCommentId(comment) || `${eventId}-${comment.createdAt}`}>
            <div className="my-comment-meta">
              <span className="my-comment-number">{number}.</span>
              <span>{comment.createdAt || comment.createdAtAt || comment.created_at || ''}</span>
              <button type="button"
                      onClick={() => setCommentToDelete(comment)}>삭제</button>
            </div>
            <button type="button"
                    className="my-comment-event"
                    onClick={() => eventId && onOpenEvent?.({
                      id: eventId,
                      title: eventTitles[String(eventId)] || `이벤트 ${eventId}` })}>
              {eventTitles[String(eventId)] || `이벤트 ${eventId || ''}`}</button>
            <p>{comment.content || comment.comment}</p>
          </article>;
        })}
      {commentPageCount > 1
          && <div className="comment-pagination">
            <button type="button"
                    disabled={currentCommentPage === 0}
                    onClick={() => setCommentPage((page) => page - 1)}>이전</button>
            <span>{currentCommentPage + 1} / {commentPageCount}</span>
            <button type="button" disabled={currentCommentPage >= commentPageCount - 1}
                    onClick={() => setCommentPage((page) => page + 1)}>다음</button>
      </div>}
    </> : <p className="mypage-empty">작성한 댓글이 없습니다.</p>}</section>
    {commentToDelete &&
        <Modal backdropClassName="confirm-backdrop" onClose={() => setCommentToDelete(null)}>
          <div className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="my-comment-delete-title">
            <h2 id="my-comment-delete-title">댓글 삭제</h2>
            <p>이 댓글을 삭제하시겠습니까?</p>
            <div className="confirm-dialog-actions">
              <button type="button"
                      className="confirm-cancel"
                      onClick={() => setCommentToDelete(null)}>취소</button>
              <button type="button"
                      className="confirm-delete"
                      onClick={() => deleteComment(commentToDelete)}>삭제</button>
            </div>
          </div>
        </Modal>
    }
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
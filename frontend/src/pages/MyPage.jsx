import { useEffect, useState, useRef } from 'react';
import Header from '../components/Header';
import './MyPage.css';
import { apiRequest } from '../api/client';

function getCommentId(comment) {
  const id = comment.id ?? comment.commentId ?? comment.comment_id;
  return id === null || id === undefined || id === '' ? null : String(id);
}

export default function MyPage({ user, initialSection = 'profile', onLoginClick, onLogout, onBack, onOpenEvent, onOpenMyPage, onOpenFavorites, onOpenAdminPage }) {
  const [profile, setProfile] = useState(user || {});
  const [profileForm, setProfileForm] = useState({ nickname: user?.nickname || '', name: user?.name || '', phoneNumber: user?.phoneNumber || '', birth: user?.birth || '' });
  const [passwordForm, setPasswordForm] = useState({ currentPassword: '', updatePassword: '', passwordConfirm: '' });
  const [comments, setComments] = useState([]);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');
  const [editingProfile, setEditingProfile] = useState(false);
  const [commentToDelete, setCommentToDelete] = useState(null);
  const [eventTitles, setEventTitles] = useState({});
  const [commentPage, setCommentPage] = useState(0);
  const COMMENTS_PER_PAGE = 10;

  useEffect(() => {
    apiRequest('/api/user/me').then((data) => { setProfile(data); setProfileForm({ nickname: data.nickname || '', name: data.name || '', phoneNumber: data.phoneNumber || '', birth: data.birth || '' }); }).catch((e) => setError(e.message));
    apiRequest('/api/comments').then((data) => {
      const serverComments = Array.isArray(data) ? data : (data.comments || data.content || []);
      setComments(serverComments);
    }).catch((e) => {
      setComments([]);
      setError(e.message);
    });
  }, [user]);

  useEffect(() => {
    const eventIds = [...new Set(comments.map((comment) => comment.eventId).filter(Boolean))];
    if (!eventIds.length) return;

    Promise.all(eventIds.map(async (eventId) => {
      try {
        const event = await apiRequest(`/api/events/${eventId}`);
        return [String(eventId), event.title || `이벤트 ${eventId}`];
      } catch {
        return [String(eventId), `이벤트 ${eventId}`];
      }
    })).then((entries) => setEventTitles(Object.fromEntries(entries)));
  }, [comments]);

  const currentCommentPage = Math.min(commentPage, Math.max(0, Math.ceil(comments.length / COMMENTS_PER_PAGE) - 1));


  const updateProfile = async (event) => {
    event.preventDefault();
    setError('');
    try {
      await apiRequest('/api/user/me', {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          nickname: profileForm.nickname.trim(),
          name: profileForm.name.trim(),
          phoneNumber: profileForm.phoneNumber.trim(),
          birth: profileForm.birth || null,
        }),
      });
      setProfile((current) => ({ ...current, ...profileForm }));
      setEditingProfile(false);
      setMessage('회원정보가 수정되었습니다.');
    } catch (e) {
      setError(e.message);
    }
  };
  const updatePassword = async (event) => { event.preventDefault(); setError(''); if (passwordForm.updatePassword !== passwordForm.passwordConfirm) { setError('새 비밀번호가 일치하지 않습니다.'); return; } try { await apiRequest('/api/user/me/password', { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(passwordForm) }); setPasswordForm({ currentPassword: '', updatePassword: '', passwordConfirm: '' }); setMessage('비밀번호가 변경되었습니다.'); } catch (e) { setError(e.message); } };
  const quit = async () => { const password = window.prompt('회원탈퇴를 진행하려면 비밀번호를 입력해주세요.'); if (password === null) return; try { await apiRequest('/api/user/me', { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ password }) }); onLogout(); } catch (e) { setError(e.message); } };
  const deleteComment = async (comment) => {
    const commentId = getCommentId(comment);
    if (!commentId) return;
    try {
      await apiRequest(`/api/comments/${commentId}`, { method: 'DELETE' });
      setComments((current) => current.filter((item) => getCommentId(item) !== commentId));
      setMessage('댓글이 삭제되었습니다.');
    } catch (e) { setError(e.message); }
    finally { setCommentToDelete(null); }
  };
  const update = (setForm) => (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));

  return <div className="mypage"><Header user={user} onLoginClick={onLoginClick} onLogout={onLogout} onOpenMyPage={onOpenMyPage} onOpenFavorites={onOpenFavorites} onOpenAdminPage={onOpenAdminPage} /><main className="mypage-container"><button type="button" className="mypage-back" onClick={onBack}>← 메인으로</button><h1>마이페이지</h1><p className="mypage-intro">내 정보와 활동을 관리할 수 있습니다.</p>{(message || error) && <p className={error ? 'mypage-error' : 'mypage-message'}>{error || message}</p>}
    <section className="mypage-panel"><h2>회원정보</h2><form className="mypage-form" onSubmit={updateProfile}><label>이메일<input value={profile.email || ''} readOnly /></label><label>닉네임<input name="nickname" value={profileForm.nickname} onChange={update(setProfileForm)} readOnly={!editingProfile} required /></label><label>이름<input name="name" value={profileForm.name} onChange={update(setProfileForm)} readOnly={!editingProfile} /></label><label>전화번호<input name="phoneNumber" value={profileForm.phoneNumber} onChange={update(setProfileForm)} readOnly={!editingProfile} /></label><label>생년월일<input name="birth" type="date" value={profileForm.birth || ''} onChange={update(setProfileForm)} readOnly={!editingProfile} /></label>{editingProfile ? <div className="mypage-form-actions"><button type="submit">완료</button><button type="button" className="secondary" onClick={() => { setEditingProfile(false); setProfileForm({ nickname: profile.nickname || '', name: profile.name || '', phoneNumber: profile.phoneNumber || '', birth: profile.birth || '' }); }}>취소</button></div> : <button type="button" onClick={() => { setEditingProfile(true); setMessage('정보를 수정한 뒤 완료를 눌러 저장하세요.'); }}>정보 수정</button>}</form></section>
    <section className="mypage-panel"><h2>보안 설정</h2><form className="mypage-form" onSubmit={updatePassword}><label>현재 비밀번호<input name="currentPassword" type="password" value={passwordForm.currentPassword} onChange={update(setPasswordForm)} required /></label><label>새 비밀번호<input name="updatePassword" type="password" value={passwordForm.updatePassword} onChange={update(setPasswordForm)} required /></label><label>새 비밀번호 확인<input name="passwordConfirm" type="password" value={passwordForm.passwordConfirm} onChange={update(setPasswordForm)} required /></label><button type="submit">비밀번호 변경</button></form></section>
    <section className="mypage-panel"><h2>내가 작성한 댓글</h2>{comments.length ? <>
      {comments.slice(currentCommentPage * COMMENTS_PER_PAGE, (currentCommentPage + 1) * COMMENTS_PER_PAGE).map((comment, index) => {
        const number = currentCommentPage * COMMENTS_PER_PAGE + index + 1;
        const eventId = comment.eventId;
        return <article className="my-comment" key={getCommentId(comment) || `${eventId}-${comment.createdAt}`}><div className="my-comment-meta"><span className="my-comment-number">{number}.</span><span>{comment.createdAt || comment.createdAtAt || comment.created_at || ''}</span><button type="button" onClick={() => setCommentToDelete(comment)}>삭제</button></div><button type="button" className="my-comment-event" onClick={() => eventId && onOpenEvent?.({ id: eventId, title: eventTitles[String(eventId)] || `이벤트 ${eventId}` })}>{eventTitles[String(eventId)] || `이벤트 ${eventId || ''}`}</button><p>{comment.content || comment.comment}</p></article>;
      })}
      {Math.ceil(comments.length / COMMENTS_PER_PAGE) > 1 && <div className="comment-pagination"><button type="button" disabled={currentCommentPage === 0} onClick={() => setCommentPage((page) => page - 1)}>이전</button><span>{currentCommentPage + 1} / {Math.ceil(comments.length / COMMENTS_PER_PAGE)}</span><button type="button" disabled={currentCommentPage >= Math.ceil(comments.length / COMMENTS_PER_PAGE) - 1} onClick={() => setCommentPage((page) => page + 1)}>다음</button></div>}
    </> : <p className="mypage-empty">작성한 댓글이 없습니다.</p>}</section>
    {commentToDelete && <div className="confirm-backdrop" role="presentation"><div className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="my-comment-delete-title"><h2 id="my-comment-delete-title">댓글 삭제</h2><p>이 댓글을 삭제하시겠습니까?</p><div className="confirm-dialog-actions"><button type="button" className="confirm-cancel" onClick={() => setCommentToDelete(null)}>취소</button><button type="button" className="confirm-delete" onClick={() => deleteComment(commentToDelete)}>삭제</button></div></div></div>}
    <section className="danger-zone"><h2>회원탈퇴</h2><p>탈퇴하면 계정 정보를 다시 복구할 수 없습니다.</p><button type="button" onClick={quit}>회원탈퇴</button></section>
  </main></div>;
}

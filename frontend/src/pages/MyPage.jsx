import { useEffect, useState } from 'react';
import Header from '../components/Header';
import './MyPage.css';

const token = () => localStorage.getItem('eats-a-deal-token');

function getCommentId(comment) {
  return comment.id ?? comment.commentId;
}
async function request(url, options = {}) {
  const response = await fetch(url, { ...options, headers: { token: token() || '', ...(options.headers || {}) } });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.msg || data.message || '요청에 실패했습니다.');
  return data;
}

export default function MyPage({ user, onLoginClick, onLogout, onBack }) {
  const [profile, setProfile] = useState(user || {});
  const [profileForm, setProfileForm] = useState({ nickname: user?.nickname || '', name: user?.name || '', phoneNumber: user?.phoneNumber || '', birth: user?.birth || '' });
  const [passwordForm, setPasswordForm] = useState({ currentPassword: '', updatePassword: '', passwordConfirm: '' });
  const [comments, setComments] = useState([]);
  const [message, setMessage] = useState('');
  const [error, setError] = useState('');

  useEffect(() => {
    request('/user/mypage').then((data) => { setProfile(data); setProfileForm({ nickname: data.nickname || '', name: data.name || '', phoneNumber: data.phoneNumber || '', birth: data.birth || '' }); }).catch((e) => setError(e.message));
    request('/comment/my').then((data) => {
      const serverComments = Array.isArray(data) ? data : (data.comments || data.content || []);
      setComments(serverComments);
    }).catch(() => {
      const found = [];
      Object.keys(localStorage).filter((key) => key.startsWith('eats-a-deal-comments-')).forEach((key) => { try { (JSON.parse(localStorage.getItem(key)) || []).forEach((comment) => found.push({ ...comment, eventId: key.replace('eats-a-deal-comments-', '') })); } catch { /* ignore malformed local data */ } });
      setComments(found.filter((comment) => comment.author === user?.nickname || comment.author === user?.id));
    });
  }, [user]);

  const updateProfile = async (event) => { event.preventDefault(); setError(''); try { await request('/user/mypage/update', { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ ...profileForm, birth: profileForm.birth || null }) }); setProfile((current) => ({ ...current, ...profileForm })); setMessage('회원정보가 수정되었습니다.'); } catch (e) { setError(e.message); } };
  const updatePassword = async (event) => { event.preventDefault(); setError(''); if (passwordForm.updatePassword !== passwordForm.passwordConfirm) { setError('새 비밀번호가 일치하지 않습니다.'); return; } try { await request('/user/mypage/updatePassword', { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(passwordForm) }); setPasswordForm({ currentPassword: '', updatePassword: '', passwordConfirm: '' }); setMessage('비밀번호가 변경되었습니다.'); } catch (e) { setError(e.message); } };
  const quit = async () => { const password = window.prompt('회원탈퇴를 진행하려면 비밀번호를 입력해주세요.'); if (password === null) return; try { await request('/user/quit', { method: 'DELETE', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ password }) }); onLogout(); } catch (e) { setError(e.message); } };
  const deleteComment = async (comment) => {
    const commentId = getCommentId(comment);
    if (!commentId || !window.confirm('이 댓글을 삭제하시겠습니까?')) return;
    try {
      await request(`/comment/${commentId}`, { method: 'DELETE' });
      setComments((current) => current.filter((item) => getCommentId(item) !== commentId));
      setMessage('댓글이 삭제되었습니다.');
    } catch (e) { setError(e.message); }
  };
  const update = (setForm) => (event) => setForm((current) => ({ ...current, [event.target.name]: event.target.value }));

  return <div className="mypage"><Header user={user} onLoginClick={onLoginClick} onLogout={onLogout} /><main className="mypage-container"><button type="button" className="mypage-back" onClick={onBack}>← 메인으로</button><h1>마이페이지</h1><p className="mypage-intro">내 정보와 활동을 관리할 수 있습니다.</p>{(message || error) && <p className={error ? 'mypage-error' : 'mypage-message'}>{error || message}</p>}
    <section className="mypage-panel"><h2>회원정보</h2><form className="mypage-form" onSubmit={updateProfile}><label>이메일<input value={profile.email || ''} readOnly /></label><label>닉네임<input name="nickname" value={profileForm.nickname} onChange={update(setProfileForm)} required /></label><label>이름<input name="name" value={profileForm.name} onChange={update(setProfileForm)} /></label><label>전화번호<input name="phoneNumber" value={profileForm.phoneNumber} onChange={update(setProfileForm)} /></label><label>생년월일<input name="birth" type="date" value={profileForm.birth || ''} onChange={update(setProfileForm)} /></label><button type="submit">정보 수정</button></form></section>
    <section className="mypage-panel"><h2>보안 설정</h2><form className="mypage-form" onSubmit={updatePassword}><label>현재 비밀번호<input name="currentPassword" type="password" value={passwordForm.currentPassword} onChange={update(setPasswordForm)} required /></label><label>새 비밀번호<input name="updatePassword" type="password" value={passwordForm.updatePassword} onChange={update(setPasswordForm)} required /></label><label>새 비밀번호 확인<input name="passwordConfirm" type="password" value={passwordForm.passwordConfirm} onChange={update(setPasswordForm)} required /></label><button type="submit">비밀번호 변경</button></form></section>
    <section className="mypage-panel"><h2>내가 작성한 댓글</h2>{comments.length ? comments.map((comment) => <article className="my-comment" key={getCommentId(comment) || `${comment.eventId}-${comment.createdAt}`}><div className="my-comment-meta"><span>{comment.createdAt || comment.createdAtAt || comment.created_at || ''}</span><button type="button" onClick={() => deleteComment(comment)}>삭제</button></div><p>{comment.content || comment.comment}</p></article>) : <p className="mypage-empty">작성한 댓글이 없습니다.</p>}</section>
    <section className="danger-zone"><h2>회원탈퇴</h2><p>탈퇴하면 계정 정보를 다시 복구할 수 없습니다.</p><button type="button" onClick={quit}>회원탈퇴</button></section>
  </main></div>;
}

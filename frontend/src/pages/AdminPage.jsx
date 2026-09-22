import { useEffect, useState } from 'react';
import Header from '../components/Header';
import './AdminPage.css';
import { apiRequest } from '../api/client';

const request = apiRequest;

export default function AdminPage({ user, onLoginClick, onLogout, onBack, onOpenMyPage, onOpenFavorites }) {
  const [tab, setTab] = useState('brand');
  const [brands, setBrands] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedUser, setSelectedUser] = useState(null);
  const [userNickname, setUserNickname] = useState('');
  const [confirmDelete, setConfirmDelete] = useState(null);
  const [userMessage, setUserMessage] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState(null);
  const [comments, setComments] = useState([]);
  const [commentsLoading, setCommentsLoading] = useState(false);
  const [crawlerLoading, setCrawlerLoading] = useState(false);

  const loadLists = async () => {
    setLoading(true);
    setMessage('');
    try {
      const [brandData, categoryData] = await Promise.all([
        request('/api/brands'),
        request('/api/categories'),
      ]);
      setBrands(Array.isArray(brandData) ? brandData : []);
      setCategories(Array.isArray(categoryData) ? categoryData : []);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => {
    // 목록 초기 조회는 외부 API와 동기화하기 위한 초기 효과입니다.
    // eslint-disable-next-line react-hooks/set-state-in-effect
    loadLists().catch(() => {});
  }, []);

  const loadComments = async () => {
    setCommentsLoading(true);
    setMessage('');
    try {
      const eventData = await request('/api/events?page=0&size=100');
      const events = eventData.content || [];
      const results = await Promise.allSettled(events.map((event) => request(`/api/events/${event.id}/comments`)));
      const merged = [];
      results.forEach((result, index) => {
        if (result.status !== 'fulfilled') return;
        const event = events[index];
        const list = Array.isArray(result.value) ? result.value : (result.value.comments || result.value.content || []);
        list.forEach((comment) => merged.push({ ...comment, eventTitle: event.title, eventId: comment.eventId || event.id }));
      });
      setComments(merged.sort((a, b) => new Date(b.createdAt || 0) - new Date(a.createdAt || 0)));
    } catch (error) {
      setMessage(`댓글 목록을 불러오지 못했습니다: ${error.message}`);
    } finally {
      setCommentsLoading(false);
    }
  };

  const updateCommentVisibility = async (comment) => {
    const commentId = comment.commentId || comment.id;
    if (!commentId) return;
    try {
      const isHidden = String(comment.status || '').toUpperCase() === 'HIDDEN';
      await request(`/api/comments/${commentId}/${isHidden ? 'unhide' : 'hide'}`, { method: 'PATCH' });
      setComments((current) => current.map((item) => item.commentId === comment.commentId ? { ...item, status: isHidden ? 'ACTIVE' : 'HIDDEN', content: isHidden ? item.originalContent || item.content : '숨김 처리된 댓글입니다.' } : item));
      setMessage(isHidden ? '댓글 숨김을 해제했습니다.' : '댓글을 숨김 처리했습니다.');
      await loadComments();
    } catch (error) { setMessage(error.message); }
  };

  const runCrawler = async (path, label) => {
    setCrawlerLoading(true);
    setMessage(`${label} 크롤링을 실행하는 중입니다...`);
    try {
      const result = await request(path);
      const count = Array.isArray(result) ? ` ${result.length}건을 수집했습니다.` : ' 완료되었습니다.';
      window.dispatchEvent(new CustomEvent('eatsadeal:events-updated'));
      setMessage(`${label} 크롤링${count} 이벤트 목록에서 확인할 수 있습니다.`);
    } catch (error) {
      setMessage(`${label} 크롤링에 실패했습니다: ${error.message}`);
    } finally { setCrawlerLoading(false); }
  };

  const deleteItem = async (type, id, name) => {
    setConfirmDelete({ type, id, name });
  };

  const confirmDeleteItem = async () => {
    if (!confirmDelete) return;
    const { type, id } = confirmDelete;
    setConfirmDelete(null);
    try {
      await request(`/api/${type === 'brand' ? 'brands' : 'categories'}/${id}`, { method: 'DELETE' });
      setMessage(`${type === 'brand' ? '브랜드' : '카테고리'}가 삭제되었습니다.`);
      await loadLists();
    } catch (error) { setMessage(error.message); }
  };

  const findUser = async (event) => {
    event.preventDefault();
    setUserMessage('');
    const userId = Number(userNickname.trim());
    if (!Number.isInteger(userId) || userId <= 0) {
      setSelectedUser(null);
      setUserMessage('현재 백엔드에는 닉네임으로 회원을 조회하는 API가 없어 회원 ID(숫자)로 검색해야 합니다.');
      return;
    }
    try {
      setSelectedUser(await request(`/api/user/admin/${userId}`));
    } catch (error) {
      setSelectedUser(null);
      setUserMessage(error.message);
    }
  };

  const changeUserStatus = async (active) => {
    try {
      await request(`/api/user/admin/${selectedUser.id}/suspension`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ suspendTime: 30, suspendReason: active ? '관리자 해제' : '관리자 처리', status: !active }),
      });
      setUserMessage(active ? '회원 정지가 해제되었습니다.' : '회원이 정지되었습니다.');
      setSelectedUser((current) => ({ ...current, status: active ? 'ACTIVE' : 'SUSPENDED' }));
    } catch (error) { setUserMessage(error.message); }
  };

  const openCreate = (type) => setForm(type === 'brand' ? { type, name: '', url: '', img: '', categoryIds: '' } : { type, name: '', img: '' });
  const openEdit = async (type, item) => {
    if (type !== 'brand') {
      setForm({ type, id: item.id, name: item.name || '', img: item.img || '' });
      return;
    }
    try {
      const detail = await request(`/api/brands/${item.id}`);
      setForm({ type, id: item.id, name: detail.name || '', url: detail.url || '', img: detail.img || '', categoryIds: (detail.categoryIds || []).join(', ') });
    } catch (error) {
      setMessage(`브랜드 상세 정보를 불러오지 못했습니다: ${error.message}`);
    }
  };
  const submitForm = async (event) => {
    event.preventDefault();
    const name = form.name.trim();
    const img = form.img.trim();

    if (!name) {
      setMessage('이름을 입력해주세요.');
      return;
    }
    if (!img) {
      setMessage(form.type === 'category' ? '카테고리 이미지 URL을 입력해주세요.' : '브랜드 이미지 URL을 입력해주세요.');
      return;
    }

    try {
      if (form.type === 'brand') {
        const categoryIds = form.categoryIds
          .split(',')
          .map((value) => Number(value.trim()))
          .filter((value) => Number.isInteger(value) && value > 0);
        if (!categoryIds.length) {
          setMessage('카테고리 ID를 하나 이상 입력해주세요. 예: 1, 2');
          return;
        }
        const body = { name, url: form.url.trim(), img, categoryIds };
        if (form.id) await request(`/api/brands/${form.id}`, { method: 'PATCH', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
        else await request('/api/brands', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
      } else {
        const body = form.id ? { img } : { name, img };
        await request(form.id ? `/api/categories/${form.id}` : '/api/categories', { method: form.id ? 'PATCH' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
      }
      setForm(null); setMessage('저장되었습니다.'); await loadLists();
    } catch (error) { setMessage(error.message); }
  };

  if (user?.role !== 'ADMIN') return <div className="admin-denied"><h1>접근 권한이 없습니다.</h1><button type="button" onClick={onBack}>메인으로</button></div>;

  return (
    <div className="admin-page">
      <Header user={user} onLoginClick={onLoginClick} onLogout={onLogout} onOpenMyPage={onOpenMyPage} onOpenFavorites={onOpenFavorites} onOpenAdminPage={onBack} />
      <main className="admin-container">
        <button type="button" className="admin-back" onClick={onBack}>← 메인으로</button>
        <div className="admin-page-title"><div><span>ADMINISTRATION</span><h1>관리자 페이지</h1><p>브랜드, 카테고리, 회원 정보를 관리합니다.</p></div><button type="button" onClick={loadLists}>새로고침</button></div>
        <nav className="admin-tabs" aria-label="관리 항목">
          <button className={tab === 'brand' ? 'active' : ''} type="button" onClick={() => setTab('brand')}>브랜드 관리</button>
          <button className={tab === 'category' ? 'active' : ''} type="button" onClick={() => setTab('category')}>카테고리 관리</button>
          <button className={tab === 'user' ? 'active' : ''} type="button" onClick={() => setTab('user')}>회원 관리</button>
          <button className={tab === 'comment' ? 'active' : ''} type="button" onClick={() => { setTab('comment'); loadComments(); }}>댓글 관리</button>
          <button className={tab === 'crawler' ? 'active' : ''} type="button" onClick={() => setTab('crawler')}>크롤링 관리</button>
        </nav>
        {message && <p className="admin-message" role="status">{message}</p>}
        {loading ? <p className="admin-empty">목록을 불러오는 중입니다...</p> : tab === 'user' ? (
          <section className="admin-panel"><h2>회원 정보 조회</h2><p className="panel-description">회원 ID(숫자)를 입력하면 회원 정보를 확인하고 정지 상태를 관리할 수 있습니다. 닉네임 검색은 백엔드 API 추가가 필요합니다.</p><form className="user-search-form" onSubmit={findUser}><input value={userNickname} onChange={(event) => setUserNickname(event.target.value)} placeholder="회원 ID(숫자) 입력" required /><button type="submit">조회</button></form>{userMessage && <p className="admin-message">{userMessage}</p>}{selectedUser && <div className="user-info-card"><div><span>회원 ID</span><strong>{selectedUser.id}</strong></div><div><span>닉네임</span><strong>{selectedUser.nickname || '-'}</strong></div><div><span>이메일</span><strong>{selectedUser.email || '-'}</strong></div><div><span>권한</span><strong>{String(selectedUser.role || 'USER')}</strong></div><div className="user-actions"><button type="button" onClick={() => changeUserStatus(false)}>회원 정지</button><button type="button" className="secondary" onClick={() => changeUserStatus(true)}>정지 해제</button></div></div>}</section>
        ) : tab === 'comment' ? (
          <section className="admin-panel"><div className="panel-heading"><div><h2>댓글 관리</h2><p className="panel-description">이벤트별 댓글을 조회하고 숨김 또는 숨김 해제를 처리합니다.</p></div><button type="button" className="create-placeholder" onClick={loadComments}>새로고침</button></div>{commentsLoading ? <p className="admin-empty">댓글을 불러오는 중입니다...</p> : comments.length === 0 ? <p className="admin-empty">등록된 댓글이 없습니다.</p> : <div className="admin-comment-list">{comments.map((comment) => { const commentId = comment.commentId || comment.id; const hidden = String(comment.status || '').toUpperCase() === 'HIDDEN'; return <div className="admin-comment-row" key={commentId}><div className="admin-comment-content"><strong>{comment.nickname || '알 수 없는 사용자'}</strong><span>{comment.eventTitle || `이벤트 ${comment.eventId}`}</span><p className={hidden ? 'hidden-comment' : ''}>{comment.content || (hidden ? '숨김 처리된 댓글입니다.' : '')}</p><small>{comment.createdAt ? new Date(comment.createdAt).toLocaleString('ko-KR') : ''} · 상태: {comment.status || 'ACTIVE'}</small></div><button type="button" className={hidden ? 'secondary' : 'delete'} onClick={() => updateCommentVisibility(comment)}>{hidden ? '숨김 해제' : '숨김'}</button></div>; })}</div>}</section>
        ) : tab === 'crawler' ? (
          <section className="admin-panel"><div className="panel-heading"><div><h2>크롤링 관리</h2><p className="panel-description">외부 브랜드의 이벤트를 수집해 이벤트 목록에 반영합니다.</p></div></div><div className="crawler-actions"><button type="button" disabled={crawlerLoading} onClick={() => runCrawler('/api/crawl', '전체')}>전체 크롤링</button><button type="button" disabled={crawlerLoading} onClick={() => runCrawler('/api/crawl/chicken', '치킨')}>치킨 크롤링</button><button type="button" disabled={crawlerLoading} onClick={() => runCrawler('/api/crawl/pizza', '피자')}>피자 크롤링</button><div className="crawler-brand-actions"><strong>브랜드별</strong><button type="button" disabled={crawlerLoading} onClick={() => runCrawler('/api/crawl/bhc', 'BHC')}>BHC</button><button type="button" disabled={crawlerLoading} onClick={() => runCrawler('/api/crawl/bbq', 'BBQ')}>BBQ</button><button type="button" disabled={crawlerLoading} onClick={() => runCrawler('/api/crawl/kyochon', '교촌')}>교촌</button><button type="button" disabled={crawlerLoading} onClick={() => runCrawler('/api/crawl/pelicana', '페리카나')}>페리카나</button><button type="button" disabled={crawlerLoading} onClick={() => runCrawler('/api/crawl/goobne', '굽네')}>굽네</button><button type="button" disabled={crawlerLoading} onClick={() => runCrawler('/api/crawl/dominos', '도미노')}>도미노</button></div></div>{crawlerLoading && <p className="admin-empty">크롤링 중입니다. 완료될 때까지 잠시 기다려주세요.</p>}</section>
        ) : (
          <section className="admin-panel"><div className="panel-heading"><div><h2>{tab === 'brand' ? '브랜드 목록' : '카테고리 목록'}</h2><p className="panel-description">현재 등록된 {tab === 'brand' ? '브랜드' : '카테고리'} 정보입니다.</p></div><button type="button" className="create-placeholder" onClick={() => openCreate(tab)}>생성</button></div><div className="management-list">{(tab === 'brand' ? brands : categories).length === 0 ? <p className="admin-empty">등록된 정보가 없습니다.</p> : (tab === 'brand' ? brands : categories).map((item) => <div className="management-row" key={item.id}><div className="item-main">{item.img && <img src={item.img} alt="" onError={(event) => { event.currentTarget.style.display = 'none'; }} />}<div><strong>{item.name}</strong><span>ID: {item.id}</span></div></div><div className="row-actions"><button type="button" onClick={() => openEdit(tab, item)}>수정</button><button type="button" className="delete" onClick={() => deleteItem(tab, item.id, item.name)}>삭제</button></div></div>)}</div></section>
        )}
        {form && <div className="admin-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setForm(null); }}><form className="admin-modal" onSubmit={submitForm}><div className="modal-heading"><h2>{form.id ? '정보 수정' : '정보 생성'}</h2><button type="button" onClick={() => setForm(null)}>×</button></div><label>이름<input value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} required /></label>{form.type === 'brand' && <><label>브랜드 URL<input value={form.url} onChange={(event) => setForm((current) => ({ ...current, url: event.target.value }))} required={!form.id} /></label><label>카테고리 ID<input value={form.categoryIds} onChange={(event) => setForm((current) => ({ ...current, categoryIds: event.target.value }))} placeholder="예: 1, 2 (생성 시 필수)" required={!form.id} /></label></>}<label>{form.type === 'category' ? '카테고리 링크(URL)' : '이미지 URL'}<input value={form.img} onChange={(event) => setForm((current) => ({ ...current, img: event.target.value }))} required /></label><button className="modal-submit" type="submit">저장</button></form></div>}
        {confirmDelete && <div className="admin-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setConfirmDelete(null); }}><div className="admin-confirm-modal" role="dialog" aria-modal="true" aria-labelledby="delete-title"><div className="modal-heading"><h2 id="delete-title">삭제 확인</h2><button type="button" onClick={() => setConfirmDelete(null)}>×</button></div><p><strong>{confirmDelete.name}</strong>을(를) 삭제하시겠습니까?</p><span>삭제한 정보는 복구할 수 없습니다.</span><div className="confirm-actions"><button type="button" onClick={() => setConfirmDelete(null)}>취소</button><button type="button" className="danger-confirm" onClick={confirmDeleteItem}>삭제하기</button></div></div></div>}
      </main>
    </div>
  );
}

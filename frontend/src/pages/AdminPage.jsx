import { useEffect, useState } from 'react';
import Header from '../components/Header';
import './AdminPage.css';

const token = () => localStorage.getItem('eats-a-deal-token');
const authHeaders = () => ({ token: token() || '' });

async function request(url, options = {}) {
  const response = await fetch(url, { ...options, headers: { ...authHeaders(), ...(options.headers || {}) } });
  const data = await response.json().catch(() => ({}));
  if (!response.ok) throw new Error(data.msg || data.message || '요청에 실패했습니다.');
  return data;
}

export default function AdminPage({ user, onLoginClick, onLogout, onBack, onOpenMyPage }) {
  const [tab, setTab] = useState('brand');
  const [brands, setBrands] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedUser, setSelectedUser] = useState(null);
  const [userId, setUserId] = useState('');
  const [userMessage, setUserMessage] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState(null);

  const loadLists = async () => {
    setLoading(true);
    setMessage('');
    try {
      const [brandData, categoryData] = await Promise.all([
        request('/brand/list'),
        request('/category/list'),
      ]);
      setBrands(Array.isArray(brandData) ? brandData : []);
      setCategories(Array.isArray(categoryData) ? categoryData : []);
    } catch (error) {
      setMessage(error.message);
    } finally {
      setLoading(false);
    }
  };

  useEffect(() => { loadLists(); }, []);

  const deleteItem = async (type, id, name) => {
    if (!window.confirm(`'${name}'을(를) 삭제하시겠습니까?`)) return;
    try {
      await request(`/${type}/${id}/delete`, { method: 'DELETE' });
      setMessage(`${type === 'brand' ? '브랜드' : '카테고리'}가 삭제되었습니다.`);
      await loadLists();
    } catch (error) { setMessage(error.message); }
  };

  const findUser = async (event) => {
    event.preventDefault();
    setUserMessage('');
    try {
      setSelectedUser(await request(`/user/${userId.trim()}`));
    } catch (error) {
      setSelectedUser(null);
      setUserMessage(error.message);
    }
  };

  const changeUserStatus = async (active) => {
    try {
      await request(`/user/${selectedUser.id}/${active ? 'active' : 'suspend'}`, {
        method: 'PUT',
        ...(active ? {} : { headers: { 'Content-Type': 'application/json' }, body: JSON.stringify({ suspendTime: 30, suspendReason: '관리자 처리' }) }),
      });
      setUserMessage(active ? '회원 정지가 해제되었습니다.' : '회원이 정지되었습니다.');
      setSelectedUser((current) => ({ ...current, status: active ? 'ACTIVE' : 'SUSPENDED' }));
    } catch (error) { setUserMessage(error.message); }
  };

  const openCreate = (type) => setForm(type === 'brand' ? { type, name: '', url: '', img: '', categoryIds: '' } : { type, name: '', img: '' });
  const openEdit = (type, item) => setForm(type === 'brand' ? { type, id: item.id, name: item.name || '', url: item.url || '', img: item.img || '', categoryIds: '' } : { type, id: item.id, name: item.name || '', img: item.img || '' });
  const submitForm = async (event) => {
    event.preventDefault();
    try {
      if (form.type === 'brand') {
        const body = { name: form.name, url: form.url, img: form.img, categoryIds: form.categoryIds.split(',').map((value) => Number(value.trim())).filter(Boolean) };
        if (form.id) await request(`/brand/${form.id}/update`, { method: 'PUT', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
        else await request('/brand/create', { method: 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
      } else {
        const body = form.id ? { img: form.img } : { name: form.name, img: form.img };
        await request(form.id ? `/category/${form.id}/update` : '/category/create', { method: form.id ? 'PUT' : 'POST', headers: { 'Content-Type': 'application/json' }, body: JSON.stringify(body) });
      }
      setForm(null); setMessage('저장되었습니다.'); await loadLists();
    } catch (error) { setMessage(error.message); }
  };

  if (user?.role !== 'ADMIN') return <div className="admin-denied"><h1>접근 권한이 없습니다.</h1><button type="button" onClick={onBack}>메인으로</button></div>;

  return (
    <div className="admin-page">
      <Header user={user} onLoginClick={onLoginClick} onLogout={onLogout} onOpenMyPage={onOpenMyPage} />
      <main className="admin-container">
        <button type="button" className="admin-back" onClick={onBack}>← 메인으로</button>
        <div className="admin-page-title"><div><span>ADMINISTRATION</span><h1>관리자 페이지</h1><p>브랜드, 카테고리, 회원 정보를 관리합니다.</p></div><button type="button" onClick={loadLists}>새로고침</button></div>
        <nav className="admin-tabs" aria-label="관리 항목">
          <button className={tab === 'brand' ? 'active' : ''} type="button" onClick={() => setTab('brand')}>브랜드 관리</button>
          <button className={tab === 'category' ? 'active' : ''} type="button" onClick={() => setTab('category')}>카테고리 관리</button>
          <button className={tab === 'user' ? 'active' : ''} type="button" onClick={() => setTab('user')}>회원 관리</button>
        </nav>
        {message && <p className="admin-message" role="status">{message}</p>}
        {loading ? <p className="admin-empty">목록을 불러오는 중입니다...</p> : tab === 'user' ? (
          <section className="admin-panel"><h2>회원 정보 조회</h2><p className="panel-description">회원 ID를 입력하면 회원 정보를 확인하고 정지 상태를 관리할 수 있습니다.</p><form className="user-search-form" onSubmit={findUser}><input value={userId} onChange={(event) => setUserId(event.target.value)} placeholder="회원 ID 입력" required /><button type="submit">조회</button></form>{userMessage && <p className="admin-message">{userMessage}</p>}{selectedUser && <div className="user-info-card"><div><span>회원 ID</span><strong>{selectedUser.id}</strong></div><div><span>닉네임</span><strong>{selectedUser.nickname || '-'}</strong></div><div><span>이메일</span><strong>{selectedUser.email || '-'}</strong></div><div><span>권한</span><strong>{String(selectedUser.role || 'USER')}</strong></div><div className="user-actions"><button type="button" onClick={() => changeUserStatus(false)}>회원 정지</button><button type="button" className="secondary" onClick={() => changeUserStatus(true)}>정지 해제</button></div></div>}</section>
        ) : (
          <section className="admin-panel"><div className="panel-heading"><div><h2>{tab === 'brand' ? '브랜드 목록' : '카테고리 목록'}</h2><p className="panel-description">현재 등록된 {tab === 'brand' ? '브랜드' : '카테고리'} 정보입니다.</p></div><button type="button" className="create-placeholder" onClick={() => openCreate(tab)}>생성</button></div><div className="management-list">{(tab === 'brand' ? brands : categories).length === 0 ? <p className="admin-empty">등록된 정보가 없습니다.</p> : (tab === 'brand' ? brands : categories).map((item) => <div className="management-row" key={item.id}><div className="item-main">{item.img && <img src={item.img} alt="" onError={(event) => { event.currentTarget.style.display = 'none'; }} />}<div><strong>{item.name}</strong><span>ID: {item.id}</span></div></div><div className="row-actions"><button type="button" onClick={() => openEdit(tab, item)}>수정</button><button type="button" className="delete" onClick={() => deleteItem(tab, item.id, item.name)}>삭제</button></div></div>)}</div></section>
        )}
        {form && <div className="admin-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setForm(null); }}><form className="admin-modal" onSubmit={submitForm}><div className="modal-heading"><h2>{form.id ? '정보 수정' : '정보 생성'}</h2><button type="button" onClick={() => setForm(null)}>×</button></div><label>이름<input value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} required /></label>{form.type === 'brand' && <><label>브랜드 URL<input value={form.url} onChange={(event) => setForm((current) => ({ ...current, url: event.target.value }))} required={!form.id} /></label><label>카테고리 ID<input value={form.categoryIds} onChange={(event) => setForm((current) => ({ ...current, categoryIds: event.target.value }))} placeholder="예: 1, 2 (생성 시 필수)" required={!form.id} /></label></>}<label>이미지 URL<input value={form.img} onChange={(event) => setForm((current) => ({ ...current, img: event.target.value }))} required /></label><button className="modal-submit" type="submit">저장</button></form></div>}
      </main>
    </div>
  );
}

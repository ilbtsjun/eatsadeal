import { useEffect, useState } from 'react';
import Header from '../components/Header';
import './AdminPage.css';
import { apiRequest } from '../api/client';

const request = apiRequest;

/**
 * 크롤러 목록 (모두 POST 요청).
 */
const CRAWL_ALL = { path: '/api/crawl', label: '전체' };
const CRAWL_GROUPS = [
  {
    key: 'chicken',
    label: '치킨',
    path: '/api/crawl/chicken',
    brands: [
      { path: '/api/crawl/bhc', label: 'BHC' },
      { path: '/api/crawl/bbq', label: 'BBQ' },
      { path: '/api/crawl/kyochon', label: '교촌' },
      { path: '/api/crawl/pelicana', label: '페리카나' },
      { path: '/api/crawl/goobne', label: '굽네' },
    ],
  },
  {
    key: 'pizza',
    label: '피자',
    path: '/api/crawl/pizza',
    brands: [
      { path: '/api/crawl/dominos', label: '도미노' },
      { path: '/api/crawl/papajohns', label: '파파존스' },
      { path: '/api/crawl/pizzamaru', label: '피자마루' },
      { path: '/api/crawl/pizzaetang', label: '피자에땅' },
      { path: '/api/crawl/pizzaschool', label: '피자스쿨' },
    ],
  },
  {
    key: 'hamburger',
    label: '햄버거',
    path: '/api/crawl/hamburger',
    brands: [
      { path: '/api/crawl/burgerking', label: '버거킹' },
      { path: '/api/crawl/frankburger', label: '프랭크버거' },
      { path: '/api/crawl/kfc', label: 'KFC' },
      { path: '/api/crawl/lotteria', label: '롯데리아' },
      { path: '/api/crawl/momstouch', label: '맘스터치' },
    ],
  },
];

// 브랜드 활성/정지: 정지는 기존 DELETE(소프트 삭제)를 사용하고, 활성화는 아래 경로로 가정했습니다.
// 백엔드의 실제 활성화 경로가 다르면 이 함수만 고치면 됩니다.
const BRAND_ACTIVATE_API = (id) => `/api/brands/${id}/active`;
const isBrandActive = (brand) => Boolean(brand?.isActive ?? brand?.active ?? true);

const CONFIRM_TEXT = {
  delete: { title: '삭제 확인', question: '을(를) 삭제하시겠습니까?', note: '삭제한 정보는 복구할 수 없습니다.', button: '삭제하기' },
  deactivate: { title: '브랜드 정지', question: ' 브랜드를 정지하시겠습니까?', note: '정지한 브랜드는 언제든 다시 활성화할 수 있습니다.', button: '정지하기' },
  activate: { title: '브랜드 활성화', question: ' 브랜드를 다시 활성화하시겠습니까?', note: '', button: '활성화하기' },
};

// 백엔드 UserStatus: ACTIVE / SUSPEND / WITHDRAWN
const USER_STATUS_LABELS = { ACTIVE: '정상', SUSPEND: '정지', WITHDRAWN: '탈퇴' };
const getUserStatus = (target) => String(target?.userStatus ?? target?.status ?? '').toUpperCase();

export default function AdminPage({ user, onLoginClick, onLogout, onBack, onOpenMyPage, onOpenFavorites }) {
  const [tab, setTab] = useState('brand');
  const [brands, setBrands] = useState([]);
  const [categories, setCategories] = useState([]);
  const [selectedUser, setSelectedUser] = useState(null);
  const [userNickname, setUserNickname] = useState('');
  const [userComments, setUserComments] = useState([]);
  const [commentsLoading, setCommentsLoading] = useState(false);
  const [confirmDelete, setConfirmDelete] = useState(null);
  const [userMessage, setUserMessage] = useState('');
  const [message, setMessage] = useState('');
  const [loading, setLoading] = useState(false);
  const [form, setForm] = useState(null);
  const [crawlerLoading, setCrawlerLoading] = useState(false);

  // 정지 / 정지 해제 모달 (null 이면 닫힘)
  const [suspendModal, setSuspendModal] = useState(null); // { mode: 'suspend' | 'release', days, reason }
  const [suspendError, setSuspendError] = useState('');
  const [suspendSubmitting, setSuspendSubmitting] = useState(false);

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

  // 크롤링은 POST 요청입니다.
  const runCrawler = async (path, label) => {
    setCrawlerLoading(true);
    setMessage(`${label} 크롤링을 실행하는 중입니다...`);
    try {
      const result = await request(path, { method: 'POST' });
      const count = Array.isArray(result) ? ` ${result.length}건을 수집했습니다.` : ' 완료되었습니다.';
      window.dispatchEvent(new CustomEvent('eatsadeal:events-updated'));
      setMessage(`${label} 크롤링${count} 이벤트 목록에서 확인할 수 있습니다.`);
    } catch (error) {
      setMessage(`${label} 크롤링에 실패했습니다: ${error.message}`);
    } finally { setCrawlerLoading(false); }
  };

  // 카테고리 삭제 확인창
  const deleteItem = (type, id, name) => {
    setConfirmDelete({ kind: 'delete', type, id, name });
  };

  // 브랜드 정지 / 활성화 확인창 (현재 상태에 따라 반대 동작)
  const toggleBrandActive = (brand) => {
    setConfirmDelete({ kind: isBrandActive(brand) ? 'deactivate' : 'activate', type: 'brand', id: brand.id, name: brand.name });
  };

  const confirmDeleteItem = async () => {
    if (!confirmDelete) return;
    const { kind, type, id } = confirmDelete;
    setConfirmDelete(null);
    try {
      if (kind === 'activate') {
        await request(BRAND_ACTIVATE_API(id), { method: 'PATCH' });
        setMessage('브랜드가 활성화되었습니다.');
      } else if (kind === 'deactivate') {
        await request(`/api/brands/${id}`, { method: 'DELETE' });
        setMessage('브랜드가 정지되었습니다.');
      } else {
        await request(`/api/${type === 'brand' ? 'brands' : 'categories'}/${id}`, { method: 'DELETE' });
        setMessage(`${type === 'brand' ? '브랜드' : '카테고리'}가 삭제되었습니다.`);
      }
      await loadLists();
    } catch (error) { setMessage(error.message); }
  };

  // ===== 회원 정지 / 해제 (모달) =====
  const openSuspendModal = (release) => {
    if (!selectedUser?.id) return;
    setSuspendError('');
    setSuspendModal({
      mode: release ? 'release' : 'suspend',
      days: '7',
      reason: release ? '관리자에 의한 정지 해제' : '',
    });
  };

  const closeSuspendModal = () => {
    if (suspendSubmitting) return;
    setSuspendModal(null);
    setSuspendError('');
  };

  const submitSuspension = async (event) => {
    event.preventDefault();
    if (!selectedUser?.id || !suspendModal) return;

    const release = suspendModal.mode === 'release';
    const reason = suspendModal.reason.trim();
    let suspendTime = 1;

    if (!release) {
      suspendTime = Number(suspendModal.days);
      if (!Number.isInteger(suspendTime) || suspendTime < 1) {
        setSuspendError('정지 기간은 1일 이상의 정수로 입력해주세요.');
        return;
      }
    }
    if (!reason) {
      setSuspendError(release ? '해제 사유를 입력해주세요.' : '정지 사유를 입력해주세요.');
      return;
    }

    setSuspendSubmitting(true);
    setSuspendError('');
    try {
      await request(`/api/user/admin/${selectedUser.id}/suspension`, {
        method: 'PATCH',
        headers: { 'Content-Type': 'application/json' },
        // status: true = 정지, false = 정지 해제 (기존 코드의 `!active` 와 같은 의미)
        body: JSON.stringify({ suspendTime, suspendReason: reason, status: !release }),
      });
      setSelectedUser(await request(`/api/user/admin/user?id=${selectedUser.id}`));
      setSuspendModal(null);
      setUserMessage(release ? '회원 정지가 해제되었습니다.' : '회원이 정지되었습니다.');
    } catch (error) {
      setSuspendError(error.message);
    } finally {
      setSuspendSubmitting(false);
    }
  };

  const openCreate = (type) => {
    if (type !== 'brand' && type !== 'category') return;
    setMessage('');
    if (type === 'brand') {
      setForm({ type: 'brand', name: '', url: '', img: '', categoryIds: [] });
    } else {
      setForm({ type: 'category', name: '', img: '', categoryIds: [] });
    }
  };

  // 브랜드 목록 응답에는 url / categoryIds 가 없을 수 있으므로, 수정 시 상세 API 로 다시 조회
  const openEdit = async (type, item) => {
    if (!item) return;
    if (type !== 'brand' && type !== 'category') return;
    setMessage('');

    if (type === 'category') {
      setForm({ type: 'category', id: item.id, name: item.name ?? '', img: item.img ?? '', categoryIds: [] });
      return;
    }

    try {
      const detail = await request(`/api/brands/${item.id}`);
      const ids = detail.categoryIds ?? detail.categories?.map((category) => category.id) ?? [];
      setForm({
        type: 'brand',
        id: item.id,
        name: detail.name ?? item.name ?? '',
        url: detail.url ?? '',
        img: detail.img ?? item.img ?? '',
        categoryIds: ids.map(Number).filter(Number.isFinite),
      });
    } catch (error) {
      setMessage(`브랜드 상세 정보를 불러오지 못했습니다: ${error.message}`);
    }
  };

  const findUser = async (event) => {
    event.preventDefault();
    setUserMessage('');
    setSelectedUser(null);
    setUserComments([]);

    const keyword = userNickname.trim();
    if (!keyword) {
      setUserMessage('검색할 회원 ID 또는 닉네임을 입력해주세요.');
      return;
    }

    // 숫자만 입력하면 회원 ID로 먼저 찾고, 없으면 닉네임으로 다시 찾습니다. (숫자로만 된 닉네임 대응)
    const queries = /^\d+$/.test(keyword)
        ? [`id=${keyword}`, `nickname=${encodeURIComponent(keyword)}`]
        : [`nickname=${encodeURIComponent(keyword)}`];

    let result = null;
    let lastError = null;
    for (const query of queries) {
      try {
        const data = await request(`/api/user/admin/user?${query}`);
        result = Array.isArray(data) ? data[0] : data;
        if (result) break;
      } catch (error) {
        lastError = error;
      }
    }

    if (!result) {
      setUserMessage(lastError?.message || '일치하는 회원이 없습니다.');
      return;
    }

    setSelectedUser(result);
    setCommentsLoading(true);
    try {
      const comments = await request(`/api/comments/user/${result.id}`);
      setUserComments(Array.isArray(comments) ? comments : []);
    } catch (error) {
      setUserMessage(`회원 정보는 조회했지만 댓글을 불러오지 못했습니다: ${error.message}`);
    } finally {
      setCommentsLoading(false);
    }
  };

  // 브랜드 폼에서 카테고리 선택/해제
  const toggleCategory = (categoryId) => {
    const id = Number(categoryId);
    setForm((current) => ({
      ...current,
      categoryIds: current.categoryIds.includes(id)
          ? current.categoryIds.filter((value) => value !== id)
          : [...current.categoryIds, id],
    }));
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
        const categoryIds = form.categoryIds;
        if (!categoryIds.length) {
          setMessage('카테고리를 하나 이상 선택해주세요.');
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

  const selectedUserStatus = getUserStatus(selectedUser);
  const selectedUserWithdrawn = selectedUserStatus === 'WITHDRAWN';
  const selectedUserSuspended = selectedUserStatus === 'SUSPEND' || selectedUserStatus === 'SUSPENDED';
  const isReleaseModal = suspendModal?.mode === 'release';
  const confirmText = confirmDelete ? CONFIRM_TEXT[confirmDelete.kind ?? 'delete'] : null;

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
            <button className={tab === 'crawler' ? 'active' : ''} type="button" onClick={() => setTab('crawler')}>크롤링 관리</button>
          </nav>
          {message && <p className="admin-message" role="status">{message}</p>}
          {loading ? <p className="admin-empty">목록을 불러오는 중입니다...</p> : tab === 'user' ? (
              <section className="admin-panel">
                <h2>회원 정보 조회</h2>
                <p className="panel-description">회원 ID 또는 닉네임으로 검색합니다.</p>
                <form className="user-search-form" onSubmit={findUser}>
                  <input value={userNickname} onChange={(event) => setUserNickname(event.target.value)} placeholder="회원 ID 또는 닉네임 입력" required />
                  <button type="submit">조회</button>
                </form>
                {userMessage && <p className="admin-message">{userMessage}</p>}
                {selectedUser && (
                    <div className="user-info-card">
                      <div><span>회원 ID</span><strong>{selectedUser.id}</strong></div>
                      <div><span>닉네임</span><strong>{selectedUser.nickname || '-'}</strong></div>
                      <div><span>이메일</span><strong>{selectedUser.email || '-'}</strong></div>
                      <div><span>권한</span><strong>{String(selectedUser.role || 'USER')}</strong></div>
                      <div><span>상태</span><strong>{USER_STATUS_LABELS[selectedUserStatus] || selectedUserStatus || '-'}</strong></div>
                      <div className="user-actions">
                        {selectedUserWithdrawn && <p className="user-withdrawn-notice">이미 탈퇴한 회원은 정지하거나 해제할 수 없습니다.</p>}
                        <button type="button" disabled={selectedUserWithdrawn || selectedUserSuspended} onClick={() => openSuspendModal(false)}>회원 정지</button>
                        <button type="button" className="secondary" disabled={selectedUserWithdrawn || selectedUserStatus === 'ACTIVE'} onClick={() => openSuspendModal(true)}>정지 해제</button>
                      </div>
                      <div className="user-comments">
                        <div className="user-comments-header">
                          <h3>작성한 댓글</h3>
                          <span>{userComments.length}개</span>
                        </div>
                        {commentsLoading ? (<p className="admin-empty">댓글을 불러오는 중입니다...</p>
                        ) : userComments.length === 0 ? (<p className="admin-empty">작성한 댓글이 없습니다.</p>
                        ) : (<div className="user-comment-list">
                              {userComments.map((comment, index) => (
                                  <div className="user-comment-item" key={`comment-${comment.commentId ?? comment.id ?? index}`}>
                                    <div className="user-comment-content">{comment.content}</div>
                                    <div className="user-comment-meta"><span>{comment.status ?? comment.commentStatus}</span></div>
                                  </div>
                              ))}
                            </div>
                        )}
                      </div>
                    </div>
                )}
              </section>
          ) : tab === 'crawler' ? (
              <section className="admin-panel">
                <div className="panel-heading"><div><h2>크롤링 관리</h2><p className="panel-description">외부 브랜드의 이벤트를 수집해 이벤트 목록에 반영합니다.</p></div></div>
                <div className="crawler-actions">
                  <button type="button" disabled={crawlerLoading} onClick={() => runCrawler(CRAWL_ALL.path, CRAWL_ALL.label)}>{CRAWL_ALL.label} 크롤링</button>
                  {CRAWL_GROUPS.map((group) => (
                      <button type="button" key={group.key} disabled={crawlerLoading} onClick={() => runCrawler(group.path, group.label)}>{group.label} 크롤링</button>
                  ))}
                  {CRAWL_GROUPS.map((group) => (
                      <div className="crawler-brand-actions" key={group.key}>
                        <strong>{group.label} 브랜드별</strong>
                        {group.brands.map((brand) => (
                            <button type="button" key={brand.path} disabled={crawlerLoading} onClick={() => runCrawler(brand.path, brand.label)}>{brand.label}</button>
                        ))}
                      </div>
                  ))}
                </div>
                {crawlerLoading && <p className="admin-empty">크롤링 중입니다. 완료될 때까지 잠시 기다려주세요.</p>}
              </section>
          ) : (
              <section className="admin-panel">
                <div className="panel-heading">
                  <div><h2>{tab === 'brand' ? '브랜드 목록' : '카테고리 목록'}</h2><p className="panel-description">현재 등록된 {tab === 'brand' ? '브랜드' : '카테고리'} 정보입니다.</p></div>
                  <button type="button" className="create-placeholder" onClick={() => openCreate(tab)}>생성</button>
                </div>
                <div className="management-list">
                  {(tab === 'brand' ? brands : categories).length === 0 ? <p className="admin-empty">등록된 정보가 없습니다.</p> : (tab === 'brand' ? brands : categories).map((item) => {
                    const isBrand = tab === 'brand';
                    const active = !isBrand || isBrandActive(item);
                    return (
                        <div className={`management-row ${active ? '' : 'is-inactive'}`} key={item.id}>
                          <div className="item-main">
                            {item.img && <img src={item.img} alt="" onError={(event) => { event.currentTarget.style.display = 'none'; }} />}
                            <div className="item-title">
                              <strong>{item.name}</strong>
                              {isBrand && <span className={`brand-status ${active ? 'active' : 'inactive'}`}>{active ? '활성' : '정지'}</span>}
                            </div>
                          </div>
                          <div className="row-actions">
                            <button type="button" onClick={() => openEdit(tab, item)}>수정</button>
                            {isBrand ? (
                                <button type="button" className={active ? 'delete' : 'activate'} onClick={() => toggleBrandActive(item)}>{active ? '정지' : '활성화'}</button>
                            ) : (
                                <button type="button" className="delete" onClick={() => deleteItem(tab, item.id, item.name)}>삭제</button>
                            )}
                          </div>
                        </div>
                    );
                  })}
                </div>
              </section>
          )}
          {form && (
              <div className="admin-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setForm(null); }}>
                <form className="admin-modal" onSubmit={submitForm}>
                  <div className="modal-heading"><h2>{form.id ? '정보 수정' : '정보 생성'}</h2><button type="button" onClick={() => setForm(null)}>×</button></div>
                  <label>이름<input value={form.name} onChange={(event) => setForm((current) => ({ ...current, name: event.target.value }))} required /></label>
                  {form.type === 'brand' && (
                      <>
                        <label>브랜드 URL<input value={form.url} onChange={(event) => setForm((current) => ({ ...current, url: event.target.value }))} required={!form.id} /></label>
                        <div className="category-picker">
                          <span className="category-picker-label">카테고리<em>{form.categoryIds.length}개 선택</em></span>
                          {categories.length === 0 ? (
                              <p className="category-picker-empty">등록된 카테고리가 없습니다. 카테고리를 먼저 생성해주세요.</p>
                          ) : (
                              <div className="category-chip-list">
                                {categories.map((category) => {
                                  const selected = form.categoryIds.includes(Number(category.id));
                                  return (
                                      <button
                                          type="button"
                                          key={category.id}
                                          className={`category-chip ${selected ? 'selected' : ''}`}
                                          aria-pressed={selected}
                                          onClick={() => toggleCategory(category.id)}
                                      >
                                        {category.name}
                                      </button>
                                  );
                                })}
                              </div>
                          )}
                        </div>
                      </>
                  )}
                  <label>{form.type === 'category' ? '카테고리 링크(URL)' : '이미지 URL'}<input value={form.img} onChange={(event) => setForm((current) => ({ ...current, img: event.target.value }))} required /></label>
                  <button className="modal-submit" type="submit">저장</button>
                </form>
              </div>
          )}
          {suspendModal && (
              <div className="admin-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) closeSuspendModal(); }}>
                <form className="admin-modal" role="dialog" aria-modal="true" aria-labelledby="suspend-title" onSubmit={submitSuspension}>
                  <div className="modal-heading"><h2 id="suspend-title">{isReleaseModal ? '정지 해제' : '회원 정지'}</h2><button type="button" onClick={closeSuspendModal}>×</button></div>
                  <p className="suspend-target">대상: <strong>{selectedUser?.nickname || `회원 ${selectedUser?.id}`}</strong></p>
                  {!isReleaseModal && (
                      <label>정지 기간 (일)
                        <input
                            type="number"
                            min="1"
                            step="1"
                            value={suspendModal.days}
                            onChange={(event) => { setSuspendModal((current) => ({ ...current, days: event.target.value })); setSuspendError(''); }}
                            disabled={suspendSubmitting}
                            required
                        />
                      </label>
                  )}
                  <label>{isReleaseModal ? '해제 사유' : '정지 사유'}
                    <textarea
                        rows={3}
                        value={suspendModal.reason}
                        onChange={(event) => { setSuspendModal((current) => ({ ...current, reason: event.target.value })); setSuspendError(''); }}
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
              </div>
          )}
          {confirmDelete && confirmText && (
              <div className="admin-modal-backdrop" role="presentation" onMouseDown={(event) => { if (event.target === event.currentTarget) setConfirmDelete(null); }}>
                <div className="admin-confirm-modal" role="dialog" aria-modal="true" aria-labelledby="delete-title">
                  <div className="modal-heading"><h2 id="delete-title">{confirmText.title}</h2><button type="button" onClick={() => setConfirmDelete(null)}>×</button></div>
                  <p><strong>{confirmDelete.name}</strong>{confirmText.question}</p>
                  {confirmText.note && <span>{confirmText.note}</span>}
                  <div className="confirm-actions">
                    <button type="button" onClick={() => setConfirmDelete(null)}>취소</button>
                    <button type="button" className={confirmDelete.kind === 'activate' ? 'primary-confirm' : 'danger-confirm'} onClick={confirmDeleteItem}>{confirmText.button}</button>
                  </div>
                </div>
              </div>
          )}
        </main>
      </div>
  );
}
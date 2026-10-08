import { useCallback, useEffect, useState } from 'react';
import { apiRequest } from '../api/client';
import Header from '../components/Header';
import './EventDetailPage.css';
import { usePageTitle } from '../hooks/usePageTitle.jsx';

const commentApi = {
  list: (eventId) => `/api/events/${eventId}/comments`,
  create: (eventId) => `/api/events/${eventId}/comments`,
  update: (commentId) => `/api/comments/${commentId}`,
  remove: (commentId) => `/api/comments/${commentId}`,
  hide: (commentId) => `/api/comments/${commentId}/hide`,
  unhide: (commentId) => `/api/comments/${commentId}/unhide`,
};

const SUPPORTS_REPLIES = false;

const COMMENT_MAX = 300;

function calculateDDay(endDate) {
  if (!endDate) return '기간 정보 없음';
  const today = new Date();
  const end = new Date(endDate);
  today.setHours(0, 0, 0, 0);
  end.setHours(0, 0, 0, 0);
  const diffDays = Math.ceil((end - today) / (1000 * 60 * 60 * 24));
  if (diffDays < 0) return '종료된 이벤트';
  if (diffDays === 0) return '오늘 종료';
  return `D-${diffDays}`;
}

function formatDate(value) {
  if (!value) return '정보 없음';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleDateString('ko-KR');
}

function formatDateTime(value) {
  if (!value) return '';
  const date = new Date(value);
  if (Number.isNaN(date.getTime())) return value;
  return date.toLocaleString('ko-KR', { dateStyle: 'medium', timeStyle: 'short' });
}

function getEventCodeName(eventCodes) {
  if (!eventCodes?.length) return '이벤트';
  const names = {
    DISCOUNT_PRICE: '정액 할인', DISCOUNT_RATE: '정률 할인',
    BUY_ONE_GET_ONE: '1+1', BUY_N_GET_N: 'N+1', TAKE_OUT: '포장 할인',
    DELIVERY_FREE: '배달비 무료', GIFT_PROMO: '사은품', PAYMENT_PROMO: '결제 할인',
    MEMBERSHIP: '멤버십', TIME_SALE: '타임세일',
  };
  return eventCodes.map((code) => names[code] || '이벤트').join(' · ');
}

async function commentRequest(url, options = {}) { return apiRequest(url, options); }

const jsonBody = (payload) => ({
  headers: { 'Content-Type': 'application/json' },
  body: JSON.stringify(payload),
});

function getCommentId(comment) { return comment.commentId ?? comment.id; }
function getParentId(comment) { return comment.parentId ?? comment.parentCommentId ?? null; }
function getInlineReplies(comment) { return comment.replies ?? comment.children ?? []; }
// 백엔드 CommentStatus: ACTIVE / HIDDEN / DELETED (숨김·삭제 댓글의 content 는 서버가 안내 문구로 바꿔서 내려줌)
function isHiddenComment(comment) { return comment.status === 'HIDDEN'; }
function isDeletedComment(comment) { return comment.status === 'DELETED'; }

function buildCommentTree(list) {
  const repliesByParent = new Map();
  list.forEach((comment) => {
    const parentId = getParentId(comment);
    if (parentId != null) {
      repliesByParent.set(parentId, [...(repliesByParent.get(parentId) || []), comment]);
    }
  });

  return list
      .filter((comment) => getParentId(comment) == null)
      .map((comment) => ({
        comment,
        replies: [...getInlineReplies(comment), ...(repliesByParent.get(getCommentId(comment)) || [])],
      }));
}

export default function EventDetailPage({ event, user, onLoginClick, onLogout, onBack, onOpenMyPage, onOpenFavorites, onOpenAdminPage }) {
  const [detailEvent, setDetailEvent] = useState(event);
  const [loaded, setLoaded] = useState(Boolean(event.title));
  const [loadError, setLoadError] = useState(null);
  usePageTitle(detailEvent.title || '이벤트');
  const [comments, setComments] = useState([]);
  const [commentLoading, setCommentLoading] = useState(true);
  const [commentText, setCommentText] = useState('');
  const [commentError, setCommentError] = useState('');
  const [submitting, setSubmitting] = useState(false);
  const [isFavorite, setIsFavorite] = useState(Boolean(event.isFavorite));
  const [favoriteError, setFavoriteError] = useState('');

  const [editingId, setEditingId] = useState(null);
  const [editText, setEditText] = useState('');
  const [replyTargetId, setReplyTargetId] = useState(null);
  const [replyText, setReplyText] = useState('');

  const isAdmin = user?.role === 'ADMIN';

  const isMine = (comment) => Boolean(user) && (
      comment.isMine === true
      || (comment.userId != null && comment.userId === user.id)
      || (comment.nickname != null && comment.nickname === user.nickname)
  );

  useEffect(() => {
    apiRequest(`/api/events/${event.id}`)
        .then((data) => {
          setDetailEvent({ ...data, brand: data.brandName });
          setIsFavorite(Boolean(data.isFavorite));
          setLoaded(true);
        })
        .catch((error) => {
          setFavoriteError(error.message);
          setLoadError(error);
          setLoaded(true);
        });
  }, [event.id]);

  const toggleFavorite = async () => {
    if (!user) {
      onLoginClick?.();
      return;
    }
    const nextFavorite = !isFavorite;
    try {
      await apiRequest(`/api/events/${event.id}/favorite`, {
        method: nextFavorite ? 'POST' : 'DELETE',
      });
      setIsFavorite(nextFavorite);
      setFavoriteError('');
    } catch (error) {
      setFavoriteError(error.message);
    }
  };

  const loadComments = useCallback(async () => {
    try {
      const data = await commentRequest(commentApi.list(event.id));
      setComments(Array.isArray(data) ? data : []);
    } catch (error) {
      setCommentError(error.message);
    } finally {
      setCommentLoading(false);
    }
  }, [event.id]);

  useEffect(() => {
    setCommentLoading(true);
    loadComments();
  }, [loadComments]);

  const createComment = async (content, parentId = null) => {
    setSubmitting(true);
    try {
      await commentRequest(commentApi.create(event.id), {
        method: 'POST',
        ...jsonBody(parentId != null ? { content, parentId } : { content }),
      });
      setCommentError('');
      await loadComments();
      return true;
    } catch (error) {
      setCommentError(error.message);
      return false;
    } finally {
      setSubmitting(false);
    }
  };

  const handleCommentSubmit = async (submitEvent) => {
    submitEvent.preventDefault();
    const text = commentText.trim();
    if (!user || !text || submitting) return;
    if (await createComment(text)) setCommentText('');
  };

  const openReply = (comment) => {
    if (!user) {
      onLoginClick?.();
      return;
    }
    setEditingId(null);
    setReplyTargetId(getCommentId(comment));
    setReplyText('');
  };

  const handleReplySubmit = async (submitEvent, parentComment) => {
    submitEvent.preventDefault();
    const text = replyText.trim();
    if (!user || !text || submitting) return;
    if (await createComment(text, getCommentId(parentComment))) {
      setReplyTargetId(null);
      setReplyText('');
    }
  };

  const openEdit = (comment) => {
    setReplyTargetId(null);
    setEditingId(getCommentId(comment));
    setEditText(comment.content || comment.comment || '');
  };

  const handleEditSubmit = async (submitEvent, comment) => {
    submitEvent.preventDefault();
    const text = editText.trim();
    if (!text || submitting) return;
    setSubmitting(true);
    try {
      await commentRequest(commentApi.update(getCommentId(comment)), {
        method: 'PATCH',
        ...jsonBody({ content: text }),
      });
      setEditingId(null);
      setCommentError('');
      await loadComments();
    } catch (error) {
      setCommentError(error.message);
    } finally {
      setSubmitting(false);
    }
  };

  const deleteComment = async (comment) => {
    const commentId = getCommentId(comment);
    if (!commentId || !window.confirm('이 댓글을 삭제하시겠습니까?')) return;
    try {
      await commentRequest(commentApi.remove(commentId), { method: 'DELETE' });
      setCommentError('');
      await loadComments();
    } catch (error) {
      setCommentError(error.message);
    }
  };

  const hideComment = async (comment) => {
    const commentId = getCommentId(comment);
    if (!commentId || !window.confirm('이 댓글을 숨김 처리하시겠습니까?')) return;
    try {
      await commentRequest(commentApi.hide(commentId), { method: 'PATCH' });
      setCommentError('');
      await loadComments();
    } catch (error) {
      setCommentError(error.message);
    }
  };

  const unhideComment = async (comment) => {
    const commentId = getCommentId(comment);
    if (!commentId || !window.confirm('이 댓글의 숨김을 해제하시겠습니까?')) return;
    try {
      await commentRequest(commentApi.unhide(commentId), { method: 'PATCH' });
      setCommentError('');
      await loadComments();
    } catch (error) {
      setCommentError(error.message);
    }
  };

  const renderComment = (comment, isReply = false) => {
    const commentId = getCommentId(comment);
    const hidden = isHiddenComment(comment);
    const deleted = isDeletedComment(comment);
    const active = !hidden && !deleted;
    const mine = isMine(comment);
    const editing = editingId === commentId;
    const replying = replyTargetId === commentId;

    const classNames = ['comment-item'];
    if (isReply) classNames.push('is-reply');
    if (hidden) classNames.push('is-hidden');
    if (deleted) classNames.push('is-deleted');

    return (
        <div className={classNames.join(' ')} key={commentId}>
          <div className="comment-meta">
            <strong>{comment.nickname || comment.author || comment.userNickname || '익명'}</strong>
            <span>{formatDateTime(comment.createdAt || comment.created_at)}</span>
            {hidden && isAdmin && <span className="comment-hidden-badge">숨김 처리됨</span>}
          </div>

          {editing ? (
              <form className="comment-edit-form" onSubmit={(submitEvent) => handleEditSubmit(submitEvent, comment)}>
            <textarea
                value={editText}
                onChange={(changeEvent) => setEditText(changeEvent.target.value)}
                maxLength={COMMENT_MAX}
                required
                autoFocus
            />
                <div className="comment-form-footer">
                  <span>{editText.length}/{COMMENT_MAX}</span>
                  <div className="comment-inline-buttons">
                    <button type="button" className="comment-action-button" onClick={() => setEditingId(null)}>취소</button>
                    <button type="submit" disabled={submitting}>저장</button>
                  </div>
                </div>
              </form>
          ) : (
              // 숨김/삭제 댓글의 content 는 서버가 안내 문구("숨김 처리된 댓글입니다." 등)로 바꿔서 내려줌
              <p className={active ? undefined : 'comment-hidden-text'}>{comment.content}</p>
          )}

          {!editing && (
              <div className="comment-actions">
                {SUPPORTS_REPLIES && !isReply && active && (
                    <button type="button" className="comment-action-button" onClick={() => openReply(comment)}>답글</button>
                )}
                {mine && active && (
                    <button type="button" className="comment-action-button" onClick={() => openEdit(comment)}>수정</button>
                )}
                {mine && !deleted && (
                    <button type="button" className="comment-action-button comment-delete-button" onClick={() => deleteComment(comment)}>삭제</button>
                )}
                {isAdmin && active && (
                    <button type="button" className="comment-action-button comment-hide-button" onClick={() => hideComment(comment)}>숨김</button>
                )}
                {isAdmin && hidden && (
                    <button type="button" className="comment-action-button comment-unhide-button" onClick={() => unhideComment(comment)}>숨김 해제</button>
                )}
              </div>
          )}

          {replying && (
              <form className="comment-reply-form" onSubmit={(submitEvent) => handleReplySubmit(submitEvent, comment)}>
            <textarea
                value={replyText}
                onChange={(changeEvent) => setReplyText(changeEvent.target.value)}
                placeholder={`${comment.nickname || '익명'}님에게 답글을 남겨주세요.`}
                maxLength={COMMENT_MAX}
                required
                autoFocus
            />
                <div className="comment-form-footer">
                  <span>{replyText.length}/{COMMENT_MAX}</span>
                  <div className="comment-inline-buttons">
                    <button type="button" className="comment-action-button" onClick={() => setReplyTargetId(null)}>취소</button>
                    <button type="submit" disabled={submitting}>답글 작성</button>
                  </div>
                </div>
              </form>
          )}
        </div>
    );
  };

  const commentTree = buildCommentTree(comments);
  const totalComments = commentTree.reduce((count, node) => count + 1 + node.replies.length, 0);

  const renderStatus = (content) => (
      <div className="event-detail-page">
        <Header user={user} onLoginClick={onLoginClick} onLogout={onLogout} onOpenMyPage={onOpenMyPage} onOpenFavorites={onOpenFavorites} onOpenAdminPage={onOpenAdminPage} />
        <main className="event-detail-container">
          <button type="button" className="detail-back-button" onClick={onBack}>← 할인정보 목록으로</button>
          <section className="detail-status" role="status">{content}</section>
        </main>
      </div>
  );

  if (!loaded) {
    return renderStatus(
        <>
          <span className="detail-spinner" aria-hidden="true" />
          <p>이벤트를 불러오는 중입니다…</p>
        </>
    );
  }
  if (loadError?.status === 404) {
    return renderStatus(
        <>
          <span className="detail-status-emoji" aria-hidden="true">🍗</span>
          <h1>이벤트를 찾을 수 없어요</h1>
          <p>종료되었거나 삭제된 이벤트일 수 있습니다.</p>
          <button type="button" className="detail-status-button" onClick={onBack}>이벤트 목록 보기</button>
        </>
    );
  }
  if (loadError && !detailEvent.title) {   // 404가 아닌 오류(서버 오류, 네트워크 등)
    return renderStatus(
        <>
          <span className="detail-status-emoji" aria-hidden="true">😵</span>
          <h1>이벤트를 불러오지 못했어요</h1>
          <p>{loadError.message}</p>
          <button type="button" className="detail-status-button" onClick={() => window.location.reload()}>다시 시도</button>
        </>
    );
  }

  return (
      <div className="event-detail-page">
        <Header user={user} onLoginClick={onLoginClick} onLogout={onLogout} onOpenMyPage={onOpenMyPage} onOpenFavorites={onOpenFavorites} onOpenAdminPage={onOpenAdminPage} />

        <main className="event-detail-container">
          <button type="button" className="detail-back-button" onClick={onBack}>
            ← 할인정보 목록으로
          </button>

          <article className="event-detail-card">
            <div className="detail-image-box">
              {detailEvent.img ? (
                  <img src={detailEvent.img} alt={detailEvent.title} className="detail-image" />
              ) : <span className="detail-emoji">🍗</span>}
              <span className="detail-dday">{calculateDDay(detailEvent.endDate)}</span>
              <button
                  type="button"
                  className={`detail-favorite ${isFavorite ? 'is-favorite' : ''}`}
                  aria-label={isFavorite ? '찜 취소' : '찜하기'}
                  aria-pressed={isFavorite}
                  onClick={toggleFavorite}
              >
                <svg className="favorite-icon" viewBox="0 0 24 24" aria-hidden="true"><path d="M20.8 8.8c0 5.1-8.8 10.1-8.8 10.1S3.2 13.9 3.2 8.8A4.8 4.8 0 0 1 12 6.1a4.8 4.8 0 0 1 8.8 2.7Z" /></svg> 찜하기
              </button>
            </div>

            <div className="detail-content">
              <span className="detail-brand">{detailEvent.brand || '이츠어딜'}</span>
              <h1>{detailEvent.title}</h1>
              <div className="detail-tags">
                <span>{getEventCodeName(detailEvent.eventCodes)}</span>
                <span>{formatDate(detailEvent.startDate)} ~ {formatDate(detailEvent.endDate)}</span>
                <span>조회수 {Number(detailEvent.viewCount || 0).toLocaleString()}</span>
              </div>
              {favoriteError && <p className="favorite-error" role="alert">{favoriteError}</p>}
              <p className="detail-description">
                {detailEvent.description || '이벤트에 대한 자세한 할인 내용을 확인해보세요.'}
              </p>
              {detailEvent.url && (
                  <a className="original-event-link" href={detailEvent.url} target="_blank" rel="noreferrer">
                    원문 이벤트 보러가기 ↗
                  </a>
              )}
            </div>
          </article>

          <section className="comments-section" aria-labelledby="comments-title">
            <div className="comments-heading">
              <h2 id="comments-title">댓글</h2>
              <span>{totalComments}개</span>
            </div>

            {user ? (
                <form className="comment-form" onSubmit={handleCommentSubmit}>
              <textarea
                  value={commentText}
                  onChange={(submitEvent) => setCommentText(submitEvent.target.value)}
                  placeholder="이벤트에 대한 의견을 남겨주세요."
                  maxLength={COMMENT_MAX}
                  required
              />
                  <div className="comment-form-footer">
                    <span>{commentText.length}/{COMMENT_MAX}</span>
                    <button type="submit" disabled={submitting}>댓글 작성</button>
                  </div>
                </form>
            ) : (
                <div className="comment-login-guide">
                  <p>댓글을 작성하려면 로그인이 필요합니다.</p>
                  <button type="button" onClick={onLoginClick}>로그인하기</button>
                </div>
            )}

            {commentError && <p className="comment-error" role="alert">{commentError}</p>}

            <div className="comment-list">
              {commentLoading ? <p className="no-comments">댓글을 불러오는 중입니다...</p> : commentTree.length === 0 ? (
                  <p className="no-comments">첫 번째 댓글을 작성해보세요.</p>
              ) : commentTree.map(({ comment, replies }) => (
                  <div className="comment-thread" key={getCommentId(comment)}>
                    {renderComment(comment)}
                    {replies.length > 0 && (
                        <div className="comment-replies">
                          {replies.map((reply) => renderComment(reply, true))}
                        </div>
                    )}
                  </div>
              ))}
            </div>
          </section>
        </main>
      </div>
  );
}
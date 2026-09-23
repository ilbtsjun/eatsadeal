import { useEffect, useState } from 'react';
import { apiRequest } from '../api/client';
import Header from '../components/Header';
import './EventDetailPage.css';

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

function getCommentId(comment) { return comment.commentId ?? comment.id; }

export default function EventDetailPage({ event, user, onLoginClick, onLogout, onBack, onOpenMyPage, onOpenFavorites, onOpenAdminPage }) {
  const [detailEvent, setDetailEvent] = useState(event);
  const [comments, setComments] = useState([]);
  const [commentLoading, setCommentLoading] = useState(true);
  const [commentText, setCommentText] = useState('');
  const [commentError, setCommentError] = useState('');
  const [isFavorite, setIsFavorite] = useState(Boolean(event.isFavorite));
  const [favoriteError, setFavoriteError] = useState('');

  useEffect(() => {
    apiRequest(`/api/events/${event.id}`)
      .then((data) => {
        setDetailEvent({ ...data, brand: data.brandName });
        setIsFavorite(Boolean(data.isFavorite));
      })
      .catch((error) => setFavoriteError(error.message));
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

  useEffect(() => {
    setCommentLoading(true);
    commentRequest(`/api/events/${event.id}/comments`)
      .then((data) => setComments(Array.isArray(data) ? data : []))
      .catch((error) => setCommentError(error.message))
      .finally(() => setCommentLoading(false));
  }, [event.id]);

  const handleCommentSubmit = async (submitEvent) => {
    submitEvent.preventDefault();
    const text = commentText.trim();
    if (!user || !text) return;
    try {
      const created = await commentRequest(`/api/events/${event.id}/comments`, {
        method: 'POST',
        body: JSON.stringify({ content: text }),
      });
      setComments((current) => [...current, created]);
      setCommentText('');
      setCommentError('');
    } catch (error) {
      setCommentError(error.message);
    }
  };

  const deleteComment = async (comment) => {
    const commentId = getCommentId(comment);
    if (!commentId || !window.confirm('이 댓글을 삭제하시겠습니까?')) return;
    try {
      await commentRequest(`/api/comments/${commentId}`, { method: 'DELETE' });
      setComments((current) => current.filter((item) => getCommentId(item) !== commentId));
      setCommentError('');
    } catch (error) {
      setCommentError(error.message);
    }
  };

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
            <span>{comments.length}개</span>
          </div>

          {user ? (
            <form className="comment-form" onSubmit={handleCommentSubmit}>
              <textarea
                value={commentText}
                onChange={(submitEvent) => setCommentText(submitEvent.target.value)}
                placeholder="이벤트에 대한 의견을 남겨주세요."
                maxLength={300}
                required
              />
              <div className="comment-form-footer">
                <span>{commentText.length}/300</span>
                <button type="submit">댓글 작성</button>
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
            {commentLoading ? <p className="no-comments">댓글을 불러오는 중입니다...</p> : comments.length === 0 ? (
              <p className="no-comments">첫 번째 댓글을 작성해보세요.</p>
            ) : comments.map((comment) => (
              <div className="comment-item" key={getCommentId(comment)}>
                <div className="comment-meta">
                  <strong>{comment.nickname || comment.author || comment.userNickname || '익명'}</strong>
                  <span>{comment.createdAt || comment.createdAtAt || comment.created_at}</span>
                  {user && (comment.isMine || comment.userId === user.id || comment.nickname === user.nickname) && <button type="button" className="comment-delete-button" onClick={() => deleteComment(comment)}>삭제</button>}
                </div>
                <p>{comment.content || comment.comment}</p>
              </div>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}

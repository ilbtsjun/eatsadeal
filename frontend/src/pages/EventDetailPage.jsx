import { useEffect, useState } from 'react';
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

function getSavedComments(eventId) {
  try {
    return JSON.parse(localStorage.getItem(`eats-a-deal-comments-${eventId}`)) || [];
  } catch {
    return [];
  }
}

export default function EventDetailPage({ event, user, onLoginClick, onLogout, onBack }) {
  const [detailEvent, setDetailEvent] = useState(event);
  const [comments, setComments] = useState(() => getSavedComments(event.id));
  const [commentText, setCommentText] = useState('');

  useEffect(() => {
    fetch(`/event/${event.id}`)
      .then((response) => {
        if (!response.ok) throw new Error('이벤트 상세 정보를 불러오지 못했습니다.');
        return response.json();
      })
      .then((data) => setDetailEvent({ ...data, brand: data.brandName }))
      .catch(() => {
        // 목록 응답으로도 화면을 표시할 수 있으므로 상세 조회 실패 시 기존 데이터를 유지합니다.
      });
  }, [event.id]);

  useEffect(() => {
    localStorage.setItem(`eats-a-deal-comments-${event.id}`, JSON.stringify(comments));
  }, [comments, event.id]);

  const handleCommentSubmit = (submitEvent) => {
    submitEvent.preventDefault();
    const text = commentText.trim();
    if (!user || !text) return;

    setComments((current) => [
      ...current,
      {
        id: `${Date.now()}-${Math.random()}`,
        author: user.nickname || user.id,
        content: text,
        createdAt: new Date().toLocaleDateString('ko-KR'),
      },
    ]);
    setCommentText('');
  };

  return (
    <div className="event-detail-page">
      <Header user={user} onLoginClick={onLoginClick} onLogout={onLogout} />

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
          </div>

          <div className="detail-content">
            <span className="detail-brand">{detailEvent.brand || '이츠어딜'}</span>
            <h1>{detailEvent.title}</h1>
            <div className="detail-tags">
              <span>{getEventCodeName(detailEvent.eventCodes)}</span>
              <span>{formatDate(detailEvent.startDate)} ~ {formatDate(detailEvent.endDate)}</span>
            </div>
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

          <div className="comment-list">
            {comments.length === 0 ? (
              <p className="no-comments">첫 번째 댓글을 작성해보세요.</p>
            ) : comments.map((comment) => (
              <div className="comment-item" key={comment.id}>
                <div className="comment-meta">
                  <strong>{comment.author}</strong>
                  <span>{comment.createdAt}</span>
                </div>
                <p>{comment.content}</p>
              </div>
            ))}
          </div>
        </section>
      </main>
    </div>
  );
}

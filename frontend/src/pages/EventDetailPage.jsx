import { useEffect, useState } from 'react';
import Header from '@/common/components/Header';
import './EventDetailPage.css';
import { usePageTitle } from '@/common/hooks/usePageTitle.js';
import { getFallbackImage } from '../utils/BrandUtil.jsx'
import { calculateDDay, formatDate } from '../utils/DateUtil.jsx'
import CommentSection from '@/features/comments/components/CommentSection.jsx';
import { getEvent, setFavorite } from '@/api/index.js'

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

export default function EventDetailPage({ event, user, onLoginClick, onBack }) {
  const [detailEvent, setDetailEvent] = useState(event);
  const [loaded, setLoaded] = useState(Boolean(event.title));
  const [loadError, setLoadError] = useState(null);
  usePageTitle(detailEvent.title || '이벤트');
  const [isFavorite, setIsFavorite] = useState(Boolean(event.isFavorite));
  const [favoriteError, setFavoriteError] = useState('');

  useEffect(() => {
    getEvent(event.id)
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
      await setFavorite(event.id, nextFavorite);
      setIsFavorite(nextFavorite);
      setFavoriteError('');
    } catch (error) {
      setFavoriteError(error.message);
    }
  };

  const renderStatus = (content) => (
      <div className="event-detail-page">
        <Header />
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
  if (loadError && !detailEvent.title) {
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
        <Header />

        <main className="event-detail-container">
          <button type="button" className="detail-back-button" onClick={onBack}>
            ← 뒤로 가기
          </button>

          <article className="event-detail-card">
            <div className="detail-image-box">
              {detailEvent.img ? (
                  <img src={detailEvent.img} alt={detailEvent.title} className="detail-image"
                       onLoad={(e) => {
                         if (e.currentTarget.naturalWidth <= 1) e.currentTarget.src = getFallbackImage(event.brand);
                       }}
                       onError={(e) => {
                         e.currentTarget.onerror = null; // 무한 루프 방지
                         e.currentTarget.src = getFallbackImage(event.brand);
                       }}
                  />
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

          <CommentSection
              eventId={event.id}
              user={user}
              onLoginClick={onLoginClick}
          />
        </main>
      </div>
  );
}
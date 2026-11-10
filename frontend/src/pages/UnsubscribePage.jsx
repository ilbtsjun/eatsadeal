import { Link, useSearchParams } from 'react-router';
import Header from '@/common/components/Header';
import { usePageTitle } from '@/common/hooks/usePageTitle.js';
import { useUnsubscribe } from '@/features/notifications/hooks/useUnsubscribe.js';
import './UnsubscribePage.css';

export default function UnsubscribePage() {
    usePageTitle('수신거부');
    const [searchParams] = useSearchParams();
    const { status, error, submitting, unsubscribe } = useUnsubscribe(searchParams.get('token')?.trim() || '');

    return (
        <div className="unsubscribe-page">
            <Header />
            <main className="unsubscribe-card">
                {status === 'invalid' && (
                    <>
                        <h1>유효하지 않은 링크예요</h1>
                        <p>링크가 잘못되었거나 더 이상 사용할 수 없어요.<br />수신 설정은 마이페이지에서도 바꿀 수 있어요.</p>
                    </>
                )}
                {status === 'done' && (
                    <>
                        <h1>수신거부가 처리되었어요</h1>
                        <p>앞으로 광고성 이메일을 보내지 않아요.<br />처리 결과는 메일로 안내드려요.</p>
                    </>
                )}
                {status === 'idle' && (
                    <>
                        <h1>광고성 이메일 수신을 거부할까요?</h1>
                        <p>구독 브랜드의 새 이벤트·마감 알림 메일을 더 이상 받지 않아요.<br />구독한 브랜드 목록은 그대로 유지돼요.</p>
                        {error && <p className="unsubscribe-error" role="alert">{error}</p>}
                        <button type="button" className="unsubscribe-button"
                                onClick={unsubscribe} disabled={submitting}>
                            {submitting ? '처리 중...' : '수신거부'}
                        </button>
                    </>
                )}
                <Link to="/" className="unsubscribe-link">메인으로 가기</Link>
            </main>
        </div>
    );
}

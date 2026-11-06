import { Link } from 'react-router';
import Header from '@/common/components/Header';
import StatusMessage from '@/common/components/StatusMessage.jsx';
import './BrandSubscriptionsPage.css';
import { getFallbackImage } from '@/common/utils/BrandUtil.js';
import { useBrandSubscriptions } from '@/features/subscriptions/hooks/useBrandSubscriptions.js';
import ConsentModal from '@/features/notifications/components/ConsentModal.jsx';

export default function BrandSubscriptionsPage({ user, onBack }) {
    const {
        brands,
        loading,
        loadError,
        reload,
        subscribedIds,
        pendingId,
        toggleError,
        toggleBrandSubscribe,
        consentPrompt,
    } = useBrandSubscriptions(user);

    return (
        <div className="subscriptions-page">
            <Header />
            <main className="subscriptions-container">
                <button type="button" className="subscriptions-back" onClick={onBack}>← 메인으로</button>
                <h1>브랜드 구독</h1>
                <p className="subscriptions-intro">관심 있는 브랜드를 구독하면 새 이벤트 소식을 받아볼 수 있어요.</p>
                {toggleError && <p className="subscriptions-error" role="alert">{toggleError}</p>}
                {loading && <StatusMessage type="loading" message="브랜드 목록을 불러오는 중입니다..." />}
                {!loading && loadError && <StatusMessage type="error" title="브랜드 목록을 불러오지 못했어요" message={loadError} onRetry={reload} />}
                {!loading && !loadError && (brands.length ? (
                    <div className="subscriptions-list">
                        {brands.map((brand) => {
                            const subscribed = subscribedIds.has(brand.id);
                            return (
                                <div className="subscription-row" key={brand.id}>
                                    <Link className="subscription-brand" to={brand.eventsPath}>
                                        <img
                                            className="subscription-brand-img"
                                            src={brand.img || getFallbackImage(brand.name)}
                                            alt={brand.name}
                                            onError={(e) => {
                                                e.currentTarget.onerror = null;
                                                e.currentTarget.src = getFallbackImage(brand.name);
                                            }}
                                        />
                                        <strong>{brand.name}</strong>
                                    </Link>
                                    {brand.url && (
                                        <a
                                            className="subscription-homepage"
                                            href={brand.url}
                                            target="_blank"
                                            rel="noopener noreferrer"
                                        >
                                            홈페이지
                                        </a>
                                    )}
                                    <button
                                        type="button"
                                        className={`subscription-toggle ${subscribed ? 'is-subscribed' : ''}`}
                                        aria-pressed={subscribed}
                                        disabled={pendingId === brand.id}
                                        onClick={() => toggleBrandSubscribe(brand.id)}
                                    >
                                        {subscribed ? '구독 중' : '브랜드 구독'}
                                    </button>
                                </div>
                            );
                        })}
                    </div>
                ) : <p className="subscriptions-empty">구독할 수 있는 브랜드가 없습니다.</p>)}
                {consentPrompt.isOpen && (
                    <ConsentModal
                        draft={consentPrompt.draft}
                        saving={consentPrompt.saving}
                        error={consentPrompt.error}
                        canSave={consentPrompt.canSave}
                        onToggle={consentPrompt.toggleDraft}
                        onSave={consentPrompt.save}
                        onDismiss={consentPrompt.dismiss}
                    />
                )}
            </main>
        </div>
    );
}

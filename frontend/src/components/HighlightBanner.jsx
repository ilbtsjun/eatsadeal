import './HighlightBanner.css';

export default function HighlightBanner() {
    return (
        <div className="banner-container">
            <div className="banner-content">
                <span className="banner-tag">🔥 이번 주 핫한 할인</span>
                <h2>버거킹 와퍼 세트 50% 할인 특가!</h2>
                <p>기간 한정으로 진행되는 단독 할인 기회를 놓치지 마세요.</p>
            </div>
        </div>
    );
}
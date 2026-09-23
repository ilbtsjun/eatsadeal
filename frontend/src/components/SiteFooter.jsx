import './SiteFooter.css';

export default function SiteFooter({ onOpenTerms, onOpenPrivacy }) {
    return(
        <footer className="site-footer">
            <div className="site-footer__brand">이츠어딜!</div>
            <div className="site-footer__links">
                <button type="button" onClick={onOpenTerms}>이용약관</button>
                <button type="button" onClick={onOpenPrivacy}>개인정보 처리방침</button>
            </div>
            <p>포트폴리오 및 학습 목적으로 제작된 서비스입니다.</p>
        </footer>
    );
}

import './LegalPage.css';

export default function TermsPage({ onBack, onPrivacy }) {
    return <div className="legal-page">
        <header className="legal-page__header">
            <a className="legal-page__logo" href="/" onClick={
                (event) => { event.preventDefault(); onBack(); }
            }>이츠어딜!</a>
            <button className="legal-page__home" type="button" onClick={onBack}>메인으로</button>
        </header>
        <main className="legal-page__main">
            <p className="legal-page__eyebrow">EATS a DEAL POLICY</p>
            <h1>이용약관</h1>
            <p className="legal-page__date">시행일: 2026년 9월 22일 · 포트폴리오용 초안</p>
            <div className="legal-page__switch">
                <button className="active" type="button">이용약관</button>
                <button type="button" onClick={onPrivacy}>개인정보 처리방침</button>
            </div>
            <p className="legal-page__notice">EATS a DEAL은 프랜차이즈 외식 브랜드의 이벤트 정보를 모아 보여주는 포트폴리오 및 학습 목적의 서비스입니다. 실제 이벤트 조건은 각 브랜드의 공식 안내를 최종적으로 확인해 주세요.</p>
            <section>
                <h2>제1조 (목적)</h2>
                <p>이 약관은 EATS a DEAL(이하 “서비스”)의 이용조건과 서비스 운영에 관한 기본적인 사항을 정하는 것을 목적으로 합니다.</p>
            </section>
            <section>
                <h2>제2조 (서비스의 내용)</h2>
                <p>서비스는 외식 브랜드의 이벤트 정보를 수집·정리하여 목록, 상세정보, 검색·필터, 댓글 및 즐겨찾기 기능을 제공합니다. 외부 브랜드의 이벤트 기간, 혜택 및 조건은 변경되거나 수집 시점과 달라질 수 있습니다.</p>
            </section>
            <section>
                <h2>제3조 (회원가입과 계정 관리)</h2>
                <p>회원은 정확한 정보를 입력해야 하며 다른 사람의 정보를 사용해서는 안 됩니다. 계정과 비밀번호 관리 책임은 회원에게 있습니다. 서비스는 부정가입, 타인 명의 사용 또는 보안상 위험이 확인되는 경우 이용을 제한할 수 있습니다.</p>
            </section>
            <section>
                <h2>제4조 (회원의 금지행위)</h2>
                <ul>
                    <li>타인의 개인정보 또는 권리를 침해하는 행위</li>
                    <li>허위 정보, 욕설, 명예훼손, 불법·유해 정보를 게시하는 행위</li>
                    <li>댓글 도배, 자동화 요청, 서비스 장애 유발 또는 계정 탈취를 시도하는 행위</li>
                    <li>서비스의 데이터와 화면을 허가 없이 대량 복제·배포하는 행위</li>
                </ul>
                <p>위반 콘텐츠는 사전 통지 후 또는 긴급한 경우 사전 통지 없이 숨김·삭제될 수 있습니다.</p>
            </section>
            <section>
                <h2>제5조 (댓글과 즐겨찾기)</h2>
                <p>댓글은 작성자의 닉네임과 함께 서비스에 표시될 수 있습니다. 신고, 약관 위반, 법적 요청 또는 운영상 필요가 있는 경우 댓글을 숨기거나 삭제할 수 있습니다. 회원 탈퇴 시 게시물의 처리 기준은 개인정보 처리방침 및 운영정책에 따릅니다.</p>
            </section>
            <section>
                <h2>제6조 (외부 링크)</h2>
                <p>서비스는 각 브랜드의 공식 페이지 등 외부 사이트로 연결되는 링크를 제공할 수 있습니다. 외부 사이트의 상품, 이벤트, 개인정보 처리 및 운영 상태에 대해서는 서비스가 보증하지 않습니다.</p>
            </section>
            <section>
                <h2>제7조 (서비스 이용 제한 및 변경)</h2>
                <p>점검, 장애, 보안 위협 또는 기타 운영상 필요한 경우 서비스의 전부 또는 일부가 변경되거나 일시 중단될 수 있습니다. 중요한 변경이 있는 경우 서비스 화면을 통해 안내합니다.</p>
            </section>
            <section>
                <h2>제8조 (개인정보 보호)</h2>
                <p>서비스는 개인정보 보호 관련 법령을 준수하며, 개인정보의 수집·이용·보관·파기에 관한 자세한 사항은 <button className="legal-inline-link" type="button" onClick={onPrivacy}>개인정보 처리방침</button>에서 확인할 수 있습니다.</p>
            </section>
            <section>
                <h2>제9조 (책임의 제한)</h2>
                <p>서비스는 외부 브랜드 이벤트 정보의 정확성, 최신성 또는 외부 사이트의 이용 가능성을 보장하지 않습니다. 서비스의 고의 또는 중대한 과실이 없는 범위에서 일시적인 장애나 외부 정보 변경으로 인한 손해에 대해 책임을 부담하지 않습니다.</p>
            </section>
            <section>
                <h2>제10조 (회원 탈퇴)</h2>
                <p>회원은 서비스에서 제공하는 방법으로 탈퇴할 수 있습니다. 탈퇴한 회원의 개인정보는 개인정보 처리방침과 관계 법령에 따라 삭제 또는 분리 보관됩니다.</p>
            </section>
            <section>
                <h2>부칙</h2>
                <p>이 약관은 2026년 9월 22일부터 적용합니다.</p>
            </section>
            <p className="legal-page__footer">문의: [eatsadealtemp@gmail.com] · 본 페이지는 포트폴리오용 임시 작성된 약관입니다.</p>

        </main>
    </div>;
}

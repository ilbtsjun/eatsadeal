import './LegalPage.css';

export default function PrivacyPage({ onBack, onTerms }) {
    return <div className="legal-page">
        <header className="legal-page__header">
            <a className="legal-page__logo" href="/" onClick={
                (event) => {event.preventDefault(); onBack(); }
            }>이츠어딜!</a>
            <button className="legal-page__home" type="button" onClick={onBack}>메인으로</button>
        </header>
        <main className="legal-page__main">
            <p className="legal-page__eyebrow">EATS a DEAL POLICY</p>
            <h1>개인정보 처리방침</h1>
            <p className="legal-page__date">시행일: 2026년 9월 22일 · 포트폴리오용 초안</p>
            <div className="legal-page__switch">
                <button type="button" onClick={onTerms}>이용약관</button>
                <button className="active" type="button">개인정보 처리방침</button>
            </div>
            <p className="legal-page__notice">EATS a DEAL은 포트폴리오 및 학습 목적의 서비스입니다. 실제 운영 전에는 운영 주체, 수탁업체, 보유기간 및 문의처를 확정하여 이 방침을 갱신해야 합니다.</p>
            <section>
                <h2>1. 개인정보의 처리 목적</h2>
                <p>서비스는 회원 식별과 로그인, 이메일 인증, 비밀번호 재설정, 댓글·즐겨찾기 제공, 부정 이용 방지 및 서비스 운영을 위해 필요한 범위에서 개인정보를 처리합니다.</p>
            </section>
            <section>
                <h2>2. 처리하는 개인정보 항목</h2>
                <h3>회원가입 및 회원관리</h3>
                <p>이메일, 비밀번호, 닉네임을 필수로 처리하며, 이름·전화번호·성별·생년월일은 서비스 설정에 따라 입력될 수 있습니다.</p>
                <h3>인증 및 비밀번호 재설정</h3>
                <p>이메일, 인증번호 및 인증 시도·발송 제한 정보가 처리됩니다. 인증번호는 인증 목적이 끝나거나 유효기간이 지나면 삭제됩니다.</p>
                <h3>서비스 이용 과정에서 생성되는 정보</h3>
                <p>회원 식별자, 로그인 시각, 댓글·즐겨찾기 정보 및 접속 IP·접속 시각·브라우저 정보 등 운영 로그가 생성될 수 있습니다.</p>
            </section>
            <section>
                <h2>3. 개인정보의 보유 및 이용기간</h2>
                <ul>
                    <li>회원정보: 회원 탈퇴 시까지</li>
                    <li>이메일 인증번호 및 비밀번호 재설정 정보: 인증 목적 달성 또는 유효기간 만료 시까지</li>
                </ul>
                <p>관계 법령에 따라 보존이 필요한 정보는 해당 기간 동안 다른 개인정보와 분리하여 보관할 수 있습니다.</p>
            </section>
            <section>
                <h2>4. 개인정보의 제3자 제공</h2>
                <p>서비스는 원칙적으로 개인정보를 외부에 제공하지 않습니다. 법령에 근거가 있거나 이용자의 별도 동의를 받는 경우에는 제공받는 자, 목적, 항목 및 보유기간을 사전에 안내합니다. 외부 브랜드에는 회원의 개인정보를 제공하지 않습니다.</p>
            </section>
            <section>
                <h2>5. 개인정보 처리의 위탁</h2>
                <p>이메일 발송, 서버·데이터베이스 운영 등 외부 업체를 이용하게 되는 경우 수탁자와 위탁업무를 이 방침에 공개합니다. 해외 업체를 이용하는 경우 국외 이전에 관한 사항을 별도로 안내합니다.</p>
            </section>
            <section>
                <h2>6. 개인정보의 파기</h2>
                <p>보유기간이 지나거나 처리 목적이 달성된 개인정보는 지체 없이 파기합니다. 전자파일은 복구하기 어려운 방법으로 삭제하고, 법령상 보존이 필요한 정보는 분리하여 보관합니다.</p>
            </section>
            <section>
                <h2>7. 정보주체의 권리</h2>
                <p>이용자는 자신의 개인정보에 대해 열람, 정정, 삭제, 처리정지 및 동의 철회를 요청할 수 있습니다. 요청은 아래 문의처로 접수할 수 있으며, 본인 확인 후 관계 법령에 따라 처리합니다.</p>
            </section>
            <section>
                <h2>8. 개인정보의 안전성 확보조치</h2>
                <ul>
                    <li>비밀번호를 단방향으로 암호화하여 저장합니다.</li>
                    <li>JWT Secret, DB 비밀번호 등 인증정보를 소스코드에 저장하지 않습니다.</li>
                    <li>운영 환경의 접근권한을 최소화하고 관리자 기능을 별도로 통제합니다.</li>
                    <li>전송 구간 암호화, 입력값 검증, 인증번호 재요청·시도 횟수 제한을 적용합니다.</li>
                    <li>개인정보가 포함될 수 있는 로그의 출력과 접근을 제한합니다.</li>
                </ul>
            </section>
            <section>
                <h2>9. 쿠키 및 로컬 저장소</h2>
                <p>현재 서비스는 로그인 상태 유지를 위해 브라우저 저장소를 사용할 수 있습니다. 브라우저 저장소에는 토큰 또는 로그인 상태와 관련된 정보가 저장될 수 있으므로 공용 기기에서는 로그아웃해야 합니다. 운영 환경에서는 HttpOnly·Secure 쿠키 등 더 안전한 인증 방식을 검토합니다.</p>
            </section>
            <section>
                <h2>10. 개인정보 보호책임자 및 문의</h2>
                <p>
                    이메일: [eatsadealtemp@gmail.com]<br />
                </p>
            </section>
            <section>
                <h2>11. 방침의 변경</h2>
                <p>개인정보 처리 항목, 목적, 보유기간 또는 수탁업체가 변경되는 경우 변경 내용과 시행일을 서비스 화면을 통해 안내합니다.</p>
            </section>
            <p className="legal-page__footer">문의:eatsadealtemp@gmail.com · 본 페이지는 포트폴리오용 임시 작성된 약관입니다.</p>
        </main>
    </div>;
}

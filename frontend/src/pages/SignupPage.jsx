import { useSignup } from '@/features/account/hooks/useSignup.js';
import './SignupPage.css';

export default function SignupPage({
                                     onLoginClick,
                                     onBack,
                                     onSignupSuccess,
                                     onOpenTerms,
                                     onOpenPrivacy }) {
  const {
    step,
    setStep,
    form,
    emailChecked,
    nicknameChecked,
    emailMessage,
    showLoginPrompt,
    nicknameMessage,
    error,
    loading,
    authCode,
    setAuthCode,
    agreeTerms,
    setAgreeTerms,
    agreePrivacy,
    setAgreePrivacy,
    allAgreed,
    updateField,
    handleAgreeAll,
    handleEmailCheck,
    handleNicknameCheck,
    handleNext,
    handleSubmit,
    handleVerify,
    setError,
  } = useSignup({ onSignupSuccess });

  return (
      <main className="signup-page">
        <section className="signup-card" aria-labelledby="signup-title">
          <button type="button" className="signup-back-button" onClick={onBack}>← 메인으로</button>
          <div className="signup-brand">EATS a DEAL</div>
          <h1 id="signup-title">회원가입</h1>
          <p className="signup-description">{step === 1 ? '필수 정보를 입력해주세요.' : '선택 정보를 입력하면 가입이 완료됩니다. 추후 관련 기능이 개발될 예정입니다.'}</p>

          <div className="signup-progress" aria-label={`회원가입 ${step}단계`}>
            <span className={step >= 1 ? 'active' : ''}>1 기본정보</span>
            <i />
            <span className={step >= 2 ? 'active' : ''}>2 선택정보</span>
            <i />
            <span className={step >= 3 ? 'active' : ''}>3 이메일 인증</span>
          </div>

          {step === 1 ? (
              <form className="signup-form" onSubmit={handleNext}>
                <label htmlFor="signup-nickname">닉네임</label>
                <div className="input-with-button">
                  <input id="signup-nickname" name="nickname" value={form.nickname} onChange={updateField} placeholder="닉네임을 입력하세요" required />
                  <button type="button" onClick={handleNicknameCheck}>중복확인</button>
                </div>
                {nicknameMessage && <p className={nicknameChecked ? 'check-message success' : 'check-message'}>{nicknameMessage}</p>}

                <label htmlFor="signup-email">이메일</label>
                <div className="input-with-button">
                  <input id="signup-email" name="email" type="email" value={form.email} onChange={updateField} placeholder="이메일을 입력하세요" required />
                  <button type="button" onClick={handleEmailCheck}>중복확인</button>
                </div>
                {emailMessage && (
                    <p className={emailChecked ? 'check-message success' : 'check-message'}>
                      {emailMessage}
                      {showLoginPrompt && <> <button type="button" className="inline-login-button" onClick={onLoginClick}>로그인</button>해 주세요.</>}
                    </p>
                )}

                <label htmlFor="signup-password">비밀번호</label>
                <input id="signup-password" name="password" type="password" value={form.password} onChange={updateField} placeholder="비밀번호를 입력하세요" required />

                <label htmlFor="signup-password-confirm">비밀번호 확인</label>
                <input id="signup-password-confirm" name="passwordConfirm" type="password" value={form.passwordConfirm} onChange={updateField} placeholder="비밀번호를 다시 입력하세요" required />

                <div className="signup-agreements">
                  <div className="agree-row agree-all">
                    <label htmlFor="agree-all">
                      <input id="agree-all" type="checkbox" checked={allAgreed} onChange={handleAgreeAll} />
                      <span>전체 동의</span>
                    </label>
                  </div>

                  <div className="agree-row">
                    <label htmlFor="agree-terms">
                      <input
                          id="agree-terms"
                          type="checkbox"
                          checked={agreeTerms}
                          onChange={(event) => { setAgreeTerms(event.target.checked); setError(''); }}
                      />
                      <span><em className="agree-required">[필수]</em> 이용약관에 동의합니다</span>
                    </label>
                    <button type="button" className="agree-view-button" onClick={() => onOpenTerms?.()}>보기</button>
                  </div>

                  <div className="agree-row">
                    <label htmlFor="agree-privacy">
                      <input
                          id="agree-privacy"
                          type="checkbox"
                          checked={agreePrivacy}
                          onChange={(event) => { setAgreePrivacy(event.target.checked); setError(''); }}
                      />
                      <span><em className="agree-required">[필수]</em> 개인정보처리방침에 동의합니다</span>
                    </label>
                    <button type="button" className="agree-view-button" onClick={() => onOpenPrivacy?.()}>보기</button>
                  </div>
                </div>

                {error && <p className="signup-error" role="alert">{error}</p>}
                <button type="submit" className="signup-submit">다음</button>
              </form>
          ) : step === 2 ? (
              <form className="signup-form" onSubmit={handleSubmit}>
                <label htmlFor="signup-name">이름 <em>선택</em></label>
                <input id="signup-name" name="name" value={form.name} onChange={updateField} placeholder="이름을 입력하세요" />

                <label htmlFor="signup-phone">전화번호 <em>선택</em></label>
                <input id="signup-phone" name="phoneNumber" value={form.phoneNumber} onChange={updateField} placeholder="전화번호를 입력하세요" />

                <label htmlFor="signup-gender">성별 <em>선택</em></label>
                <select id="signup-gender" name="userGender" value={form.userGender} onChange={updateField}>
                  <option value="UNSPECIFIED">선택하지 않음</option>
                  <option value="MALE">남성</option>
                  <option value="FEMALE">여성</option>
                </select>

                <label htmlFor="signup-birth">생년월일 <em>선택</em></label>
                <input id="signup-birth" name="birth" type="date" value={form.birth} onChange={updateField} />

                {error && <p className="signup-error" role="alert">{error}</p>}
                <div className="signup-step-buttons">
                  <button type="button" className="signup-previous" onClick={() => setStep(1)}>이전</button>
                  <button type="submit" className="signup-submit" disabled={loading}>{loading ? '가입 중...' : '회원가입'}</button>
                </div>
              </form>
          ) : (
              <form className="signup-form" onSubmit={handleVerify}>
                <label htmlFor="signup-auth-code">이메일 인증번호</label>
                <input id="signup-auth-code" value={authCode} onChange={(event) => setAuthCode(event.target.value)} placeholder="이메일로 받은 인증번호를 입력하세요" required />
                {error && <p className="signup-error" role="alert">{error}</p>}
                <div className="signup-step-buttons">
                  <button type="button" className="signup-previous" onClick={() => setStep(2)}>이전</button>
                  <button type="submit" className="signup-submit" disabled={loading}>{loading ? '인증 중...' : '인증하고 가입 완료'}</button>
                </div>
              </form>
          )}

          <p className="login-guide">이미 가입하셨나요? <button type="button" onClick={onLoginClick}>로그인</button></p>
        </section>
      </main>
  );
}
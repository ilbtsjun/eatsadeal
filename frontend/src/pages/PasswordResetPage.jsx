import { usePasswordReset } from '@/features/account/hooks/usePasswordReset.js';
import './LoginPage.css';

export default function PasswordResetPage({ onBack, onLoginClick, onResetSuccess }) {
  const {
    step,
    updateField,
    handlePasswordChangeRequest,
    handlePasswordChange,
    error,
    notice,
    goToStep,
    form,
    loading
  } = usePasswordReset({ onResetSuccess });

  return (
      <main className="login-page">
        <section className="login-card" aria-labelledby="reset-title">
          <button type="button" className="back-button" onClick={onBack}>
            ← 메인으로
          </button>

          <div className="login-brand">EATS a DEAL</div>
          <h1 id="reset-title">비밀번호 재설정</h1>
          <p className="login-description">
            {step === 1
                ? '가입한 이메일로 인증번호를 보내드립니다.'
                : '이메일로 받은 인증번호와 새 비밀번호를 입력해주세요.'}
          </p>

          <div className="reset-progress" aria-label={`비밀번호 재설정 ${step}단계`}>
            <span className={step >= 1 ? 'active' : ''}>1 이메일 확인</span>
            <i />
            <span className={step >= 2 ? 'active' : ''}>2 새 비밀번호</span>
          </div>

          {step === 1 ? (
              <form className="login-form" onSubmit={handlePasswordChangeRequest}>
                <label htmlFor="reset-email">이메일</label>
                <input
                    id="reset-email"
                    name="email"
                    type="email"
                    placeholder="가입한 이메일을 입력하세요"
                    autoComplete="email"
                    value={form.email}
                    onChange={updateField}
                    required
                />

                <p className="login-error" role="alert">{error}</p>

                <button type="submit" className="login-submit" disabled={loading}>
                  {loading ? '전송 중...' : '인증번호 받기'}
                </button>
              </form>
          ) : (
              <form className="login-form" onSubmit={handlePasswordChange}>
                <label htmlFor="reset-code">인증번호</label>
                <div className="reset-code-row">
                  <input
                      id="reset-code"
                      name="inputCode"
                      placeholder="이메일로 받은 인증번호를 입력하세요"
                      autoComplete="one-time-code"
                      onChange={updateField}
                      value={form.inputCode}
                      required
                  />
                  <button type="button" className="reset-resend" onClick={handlePasswordChangeRequest} disabled={loading}>재발송</button>
                </div>

                <label htmlFor="reset-password">새 비밀번호</label>
                <input
                    id="reset-password"
                    name="changePassword"
                    type="password"
                    placeholder="새 비밀번호를 입력하세요"
                    autoComplete="new-password"
                    onChange={updateField}
                    value={form.changePassword}
                    required
                />

                <label htmlFor="reset-password-confirm">새 비밀번호 확인</label>
                <input
                    id="reset-password-confirm"
                    name="passwordConfirm"
                    type="password"
                    placeholder="새 비밀번호를 다시 입력하세요"
                    autoComplete="new-password"
                    onChange={updateField}
                    value={form.passwordConfirm}
                    required
                />

                <p className="login-notice" role="status">{notice}</p>
                <p className="login-error" role="alert">{error}</p>

                <div className="reset-step-buttons">
                  <button type="button" className="reset-previous" onClick={() => goToStep(1)}>이전</button>
                  <button type="submit" className="login-submit" disabled={loading}>
                    {loading ? '변경 중...' : '비밀번호 변경'}
                  </button>
                </div>
              </form>
          )}

          <p className="signup-guide">
            비밀번호가 기억나셨나요?{' '}
            <button type="button" onClick={onLoginClick}>
              로그인
            </button>
          </p>
        </section>
      </main>
  );
}

import { useLogin } from '@/features/account/hooks/useLogin.js';
import './LoginPage.css';

export default function LoginPage({ onLogin, onBack, onSignupClick }) {
  const {
    id,
    setId,
    password,
    setPassword,
    error,
    loading,
    handleSubmit,
  } = useLogin({onLogin});

  return (
      <main className="login-page">
        <section className="login-card" aria-labelledby="login-title">
          <button type="button" className="back-button" onClick={onBack}>
            ← 메인으로
          </button>

          <div className="login-brand">EATS a DEAL</div>
          <h1 id="login-title">로그인</h1>
          <p className="login-description">
            할인 정보를 저장하고 더 편리하게 이용해보세요.
          </p>

          <form onSubmit={handleSubmit} className="login-form">
            <label htmlFor="login-id">이메일</label>
            <input
                id="login-id"
                value={id}
                onChange={(event) => setId(event.target.value)}
                placeholder="이메일을 입력하세요"
                autoComplete="username"
                required
            />

            <label htmlFor="login-password">비밀번호</label>
            <input
                id="login-password"
                type="password"
                value={password}
                onChange={(event) => setPassword(event.target.value)}
                placeholder="비밀번호를 입력하세요"
                autoComplete="current-password"
                required
            />

            {error && <p className="login-error" role="alert">{error}</p>}

            <button type="submit" className="login-submit" disabled={loading}>
              {loading ? '로그인 중...' : '로그인'}
            </button>
          </form>

          <p className="signup-guide">
            아직 회원이 아니신가요?{' '}
            <button type="button" onClick={onSignupClick}>
              회원가입
            </button>
          </p>
        </section>
      </main>
  );
}
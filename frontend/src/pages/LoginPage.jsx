import { useState } from 'react';
import './LoginPage.css';

const TEMP_ACCOUNTS = {
  admin: { password: 'temp', role: 'ADMIN', nickname: '관리자' },
  user: { password: 'user', role: 'USER', nickname: '일반 사용자' },
};

export default function LoginPage({ onLogin, onBack }) {
  const [id, setId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = (event) => {
    event.preventDefault();
    const account = TEMP_ACCOUNTS[id.trim()];
    if (!account || account.password !== password) {
      setError('아이디 또는 비밀번호를 확인해주세요.');
      return;
    }
    setError('');
    onLogin({ id: id.trim(), role: account.role, nickname: account.nickname });
  };

  return (
    <main className="login-page">
      <section className="login-card" aria-labelledby="login-title">
        <button type="button" className="back-button" onClick={onBack}>← 메인으로</button>
        <div className="login-brand">EATS a DEAL</div>
        <h1 id="login-title">로그인</h1>
        <p className="login-description">할인 정보를 저장하고 더 편리하게 이용해보세요.</p>
        <form onSubmit={handleSubmit} className="login-form">
          <label htmlFor="login-id">아이디</label>
          <input id="login-id" value={id} onChange={(event) => setId(event.target.value)} placeholder="아이디를 입력하세요" autoComplete="username" required />
          <label htmlFor="login-password">비밀번호</label>
          <input id="login-password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="비밀번호를 입력하세요" autoComplete="current-password" required />
          {error && <p className="login-error" role="alert">{error}</p>}
          <button type="submit" className="login-submit">로그인</button>
        </form>
        <div className="temporary-account-notice">
          <strong>테스트 계정</strong>
          <span>관리자: admin / temp</span>
          <span>일반 사용자: user / user</span>
        </div>
      </section>
    </main>
  );
}

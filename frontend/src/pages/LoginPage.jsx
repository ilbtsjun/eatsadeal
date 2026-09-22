import { useState } from 'react';
import './LoginPage.css';

async function readJsonResponse(response) {
  const text = await response.text();
  if (!text.trim()) return {};

  try {
    return JSON.parse(text);
  } catch {
    throw new Error(
      response.ok
        ? '서버 응답을 읽을 수 없습니다.'
        : `서버 요청에 실패했습니다. (HTTP ${response.status})`,
    );
  }
}

export default function LoginPage({ onLogin, onBack, onSignupClick }) {
  const [id, setId] = useState('');
  const [password, setPassword] = useState('');
  const [error, setError] = useState('');

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');

    try {
      const response = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: id.trim(), password }),
      });

      const result = await readJsonResponse(response);

      if (!response.ok || String(result.status) !== '200' || !result.token) {
        throw new Error(
          result.message || result.msg || '이메일 또는 비밀번호를 확인해주세요.',
        );
      }

      const userResponse = await fetch('/api/user/me', {
        headers: { Authorization: `Bearer ${result.token}` },
      });
      const user = await readJsonResponse(userResponse);

      if (!userResponse.ok) {
        throw new Error(
          user.message || user.msg || '사용자 정보를 불러오지 못했습니다.',
        );
      }

      onLogin({ ...user, token: result.token });
    } catch (requestError) {
      console.error('로그인 요청 실패:', requestError);
      setError(requestError.message || '로그인에 실패했습니다.');
    }
  };

  return (
    <main className="login-page">
      <section className="login-card" aria-labelledby="login-title">
        <button type="button" className="back-button" onClick={onBack}>← 메인으로</button>
        <div className="login-brand">EATS a DEAL</div>
        <h1 id="login-title">로그인</h1>
        <p className="login-description">할인 정보를 저장하고 더 편리하게 이용해보세요.</p>
        <form onSubmit={handleSubmit} className="login-form">
          <label htmlFor="login-id">이메일</label>
          <input id="login-id" value={id} onChange={(event) => setId(event.target.value)} placeholder="이메일을 입력하세요" autoComplete="username" required />
          <label htmlFor="login-password">비밀번호</label>
          <input id="login-password" type="password" value={password} onChange={(event) => setPassword(event.target.value)} placeholder="비밀번호를 입력하세요" autoComplete="current-password" required />
          {error && <p className="login-error" role="alert">{error}</p>}
          <button type="submit" className="login-submit">로그인</button>
        </form>

        <p className="signup-guide">아직 회원이 아니신가요? <button type="button" onClick={onSignupClick}>회원가입</button></p>
      </section>
    </main>
  );
}

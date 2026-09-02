import { useState } from 'react';
import './SignupPage.css';

const INITIAL_FORM = {
  nickname: '',
  email: '',
  password: '',
  passwordConfirm: '',
  name: '',
  phoneNumber: '',
  userGender: 'UNSPECIFIED',
  birth: '',
};

async function checkDuplicate(path, value) {
  const response = await fetch(`${path}/${encodeURIComponent(value)}`);
  if (!response.ok) throw new Error('중복확인에 실패했습니다.');
  return response.json();
}

export default function SignupPage({ onLoginClick, onBack, onSignupSuccess }) {
  const [step, setStep] = useState(1);
  const [form, setForm] = useState(INITIAL_FORM);
  const [emailChecked, setEmailChecked] = useState(false);
  const [nicknameChecked, setNicknameChecked] = useState(false);
  const [emailMessage, setEmailMessage] = useState('');
  const [showLoginPrompt, setShowLoginPrompt] = useState(false);
  const [nicknameMessage, setNicknameMessage] = useState('');
  const [error, setError] = useState('');
  const [loading, setLoading] = useState(false);

  const updateField = (event) => {
    const { name, value } = event.target;
    setForm((current) => ({ ...current, [name]: value }));
    if (name === 'email') {
      setEmailChecked(false);
      setEmailMessage('');
      setShowLoginPrompt(false);
    }
    if (name === 'nickname') {
      setNicknameChecked(false);
      setNicknameMessage('');
    }
    setError('');
  };

  const handleEmailCheck = async () => {
    if (!form.email.trim()) {
      setEmailMessage('이메일을 입력해주세요.');
      return;
    }
    try {
      const exists = await checkDuplicate('/user/email', form.email.trim());
      setEmailChecked(!exists);
      setShowLoginPrompt(exists);
      setEmailMessage(exists ? '이미 가입된 이메일입니다.' : '사용 가능한 이메일입니다.');
    } catch (requestError) {
      setEmailChecked(false);
      setShowLoginPrompt(false);
      setEmailMessage(requestError.message);
    }
  };

  const handleNicknameCheck = async () => {
    if (!form.nickname.trim()) {
      setNicknameMessage('닉네임을 입력해주세요.');
      return;
    }
    try {
      const exists = await checkDuplicate('/user/nickname', form.nickname.trim());
      setNicknameChecked(!exists);
      setNicknameMessage(exists ? '이미 사용 중인 닉네임입니다.' : '사용 가능한 닉네임입니다.');
    } catch (requestError) {
      setNicknameChecked(false);
      setNicknameMessage(requestError.message);
    }
  };

  const handleNext = (event) => {
    event.preventDefault();
    setError('');
    if (!nicknameChecked || !emailChecked) {
      setError('닉네임과 이메일 중복확인을 완료해주세요.');
      return;
    }
    if (form.password !== form.passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다.');
      return;
    }
    setStep(2);
  };

  const handleSubmit = async (event) => {
    event.preventDefault();
    setError('');
    setLoading(true);
    try {
      const response = await fetch('/user/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({
          name: form.name.trim() || null,
          email: form.email.trim(),
          password: form.password,
          nickname: form.nickname.trim(),
          phoneNumber: form.phoneNumber.trim() || null,
          userGender: form.userGender || 'UNSPECIFIED',
          birth: form.birth || null,
        }),
      });
      const result = await response.json().catch(() => ({}));
      if (!response.ok) {
        const duplicateEmail = `${result.msg || ''} ${result.message || ''}`.includes('이메일');
        if (duplicateEmail) {
          setStep(1);
          setEmailChecked(false);
          setEmailMessage('이미 가입된 이메일입니다.');
          setShowLoginPrompt(true);
          throw new Error('이미 가입된 이메일입니다.');
        }
        throw new Error(result.msg || result.message || '회원가입에 실패했습니다.');
      }
      onSignupSuccess();
    } catch (requestError) {
      setError(requestError.message || '회원가입에 실패했습니다.');
    } finally {
      setLoading(false);
    }
  };

  return (
    <main className="signup-page">
      <section className="signup-card" aria-labelledby="signup-title">
        <button type="button" className="signup-back-button" onClick={onBack}>← 메인으로</button>
        <div className="signup-brand">EATS a DEAL</div>
        <h1 id="signup-title">회원가입</h1>
        <p className="signup-description">{step === 1 ? '필수 정보를 입력해주세요.' : '선택 정보를 입력하면 가입이 완료됩니다.'}</p>

        <div className="signup-progress" aria-label={`회원가입 ${step}단계`}>
          <span className={step >= 1 ? 'active' : ''}>1 기본정보</span>
          <i />
          <span className={step >= 2 ? 'active' : ''}>2 선택정보</span>
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

            {error && <p className="signup-error" role="alert">{error}</p>}
            <button type="submit" className="signup-submit">다음</button>
          </form>
        ) : (
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
        )}

        <p className="login-guide">이미 가입하셨나요? <button type="button" onClick={onLoginClick}>로그인</button></p>
      </section>
    </main>
  );
}

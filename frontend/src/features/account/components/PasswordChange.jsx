import { usePasswordChange } from '../hooks/usePasswordChange.js';

export default function PasswordChange(){
    const{
        updatePassword,
        passwordForm,
        handlePasswordChange,
        passwordMessage,
        passwordError,
    } = usePasswordChange();

    return (
        <section className="mypage-panel">
            <h2>보안 설정</h2>
            <form className="mypage-form" onSubmit={updatePassword}>
                <label>현재 비밀번호
                    <input
                        name="currentPassword"
                        type="password"
                        value={passwordForm.currentPassword}
                        onChange={handlePasswordChange}
                        required
                    />
                </label>
                <label>새 비밀번호
                    <input
                        name="updatePassword"
                        type="password"
                        value={passwordForm.updatePassword}
                        onChange={handlePasswordChange}
                        required
                    />
                </label>
                <label>새 비밀번호 확인
                    <input
                        name="passwordConfirm"
                        type="password"
                        value={passwordForm.passwordConfirm}
                        onChange={handlePasswordChange}
                        required
                    />
                </label>
                {(passwordMessage || passwordError) && (
                    <p
                        className={passwordError ? 'mypage-error' : 'mypage-message'}
                        role="status"
                    >
                        {passwordError || passwordMessage}
                    </p>
                )}
                <button type="submit">비밀번호 변경</button>
            </form>
        </section>
    )
}
import { useState } from "react";
import { updatePassword as updatePasswordRequest } from '@/api/index.js'

const INITIAL_PASSWORD_FORM = {
    currentPassword: '',
    updatePassword: '',
    passwordConfirm: '',
};

export function usePasswordChange(){
    const [passwordForm, setPasswordForm] = useState(INITIAL_PASSWORD_FORM);
    const [passwordMessage, setPasswordMessage] = useState('');
    const [passwordError, setPasswordError] = useState('');

    const updatePassword = async (event) => {
        event.preventDefault();
        setPasswordError('');
        setPasswordMessage('');

        if (passwordForm.updatePassword !== passwordForm.passwordConfirm) {
            setPasswordError('새 비밀번호가 일치하지 않습니다.');
            return;
        }

        try {
            await updatePasswordRequest(passwordForm);
            setPasswordForm({ ...INITIAL_PASSWORD_FORM });
            setPasswordMessage('비밀번호가 변경되었습니다.');
        } catch (requestError) {
            setPasswordError(requestError.message || '비밀번호 변경에 실패했습니다.');
        }
    };

    const handlePasswordChange = (event) => {
        const { name, value } = event.target;
        setPasswordForm((current) => ({ ...current, [name]: value }));
        setPasswordMessage('');
        setPasswordError('');
    };

    return {
        updatePassword,
        passwordForm,
        handlePasswordChange,
        passwordMessage,
        passwordError,
    };
}
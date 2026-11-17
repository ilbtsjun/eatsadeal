import { useState } from 'react';
import { requestPasswordReset, confirmPasswordReset, } from '@/api/index.js';
import { usePending } from '@/common/hooks/usePending.js';

const INITIAL_FORM = {
    email:'',
    changePassword:'',
    passwordConfirm:'',
    inputCode:''
}

export function usePasswordReset({ onResetSuccess }) {
    const [step, setStep] = useState(1);
    const [form, setForm] = useState(INITIAL_FORM);
    const [error, setError] = useState('');
    const [notice, setNotice] = useState('');
    const [loading, withLoading] = usePending();

    const updateField = (event) => {
        const { name, value } = event.target;
        setForm((current) => ({ ...current, [name]: value }));
        setError('');
    };

    const goToStep = (nextStep) => {
        setError('');
        setStep(nextStep);
    };

    const handlePasswordChangeRequest = async (event) => {
        event.preventDefault();
        setError('');
        setNotice('');
        await withLoading(async () => {
            try{
                await requestPasswordReset({ email: form.email.trim() });
                setStep(2);
                setNotice('가입된 이메일이라면 인증번호가 발송되었습니다.');
            } catch (requestError) {
                setError(requestError.message || '인증번호 요청에 실패했습니다.');
            }
        });
    };

    const handlePasswordChange = async (event) => {
        event.preventDefault();
        setError('');

        if (form.changePassword !== form.passwordConfirm) {
            setError('새 비밀번호가 일치하지 않습니다.');
            return;
        }

        await withLoading(async () => {
            try {
                await confirmPasswordReset({
                    email: form.email.trim(),
                    inputCode: form.inputCode.trim(),
                    changePassword: form.changePassword,
                    passwordConfirm: form.passwordConfirm,
                });
                onResetSuccess?.();
            } catch (requestError) {
                setError(requestError.message || '비밀번호 변경에 실패했습니다.');
            }
        });
    };

    return {
        step,
        updateField,
        handlePasswordChangeRequest,
        handlePasswordChange,
        error,
        notice,
        goToStep,
        form,
        loading,
    };
}

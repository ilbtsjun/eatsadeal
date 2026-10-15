import { useState } from 'react';
import {
    deleteCurrentUser,
    updatePassword as updatePasswordRequest,
} from '@/api/index.js';

const INITIAL_PASSWORD_FORM = {
    currentPassword: '',
    updatePassword: '',
    passwordConfirm: '',
};

export function useMyPage({ onLogout, onBack }) {
    const [passwordForm, setPasswordForm] = useState(INITIAL_PASSWORD_FORM);
    const [showQuitModal, setShowQuitModal] = useState(false);
    const [quitPassword, setQuitPassword] = useState('');
    const [quitError, setQuitError] = useState('');
    const [quitLoading, setQuitLoading] = useState(false);
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

    const openQuitModal = () => {
        setQuitPassword('');
        setQuitError('');
        setShowQuitModal(true);
    };

    const closeQuitModal = () => {
        if (quitLoading) return;
        setShowQuitModal(false);
        setQuitPassword('');
        setQuitError('');
    };

    const handleQuit = async (event) => {
        event.preventDefault();

        if (!quitPassword) {
            setQuitError('비밀번호를 입력해주세요.');
            return;
        }

        setQuitLoading(true);
        setQuitError('');

        try {
            await deleteCurrentUser(quitPassword);
            setShowQuitModal(false);
            window.alert('회원탈퇴가 완료되었습니다.');
            onLogout?.();
            onBack?.();
        } catch (requestError) {
            setQuitError(requestError.message || '회원탈퇴에 실패했습니다.');
        } finally {
            setQuitLoading(false);
        }
    };

    const handlePasswordChange = (event) => {
        const { name, value } = event.target;
        setPasswordForm((current) => ({ ...current, [name]: value }));
        setPasswordMessage('');
        setPasswordError('');
    };

    return {
        passwordForm,
        showQuitModal,
        quitPassword,
        setQuitPassword,
        quitError,
        setQuitError,
        quitLoading,
        updatePassword,
        openQuitModal,
        closeQuitModal,
        handleQuit,
        passwordMessage,
        passwordError,
        handlePasswordChange,
    };
}
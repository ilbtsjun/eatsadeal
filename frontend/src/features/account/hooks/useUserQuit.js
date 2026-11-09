import { useState } from 'react';
import { deleteCurrentUser } from '@/api/index.js';
import { usePending } from '@/common/hooks/usePending.js';

export function useUserQuit({ onLogout, onBack }){
    const [showQuitModal, setShowQuitModal] = useState(false);
    const [quitPassword, setQuitPassword] = useState('');
    const [quitError, setQuitError] = useState('');
    const [quitLoading, withQuitLoading] = usePending();

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

        await withQuitLoading(async () => {
            setQuitError('');

            try {
                await deleteCurrentUser(quitPassword);
                setShowQuitModal(false);
                window.alert('회원탈퇴가 완료되었습니다.');
                onLogout?.();
                onBack?.();
            } catch (requestError) {
                setQuitError(requestError.message || '회원탈퇴에 실패했습니다.');
            }
        });
    };

    return {
        showQuitModal,
        quitPassword,
        setQuitPassword,
        quitError,
        setQuitError,
        quitLoading,
        openQuitModal,
        closeQuitModal,
        handleQuit
    }
}
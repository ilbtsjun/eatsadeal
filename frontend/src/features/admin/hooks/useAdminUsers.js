import { useState } from 'react';
import { getUserInfo, getUserComments, suspendUser } from '@/api/index.js'

export function useAdminUsers() {
    const [selectedUser, setSelectedUser] = useState(null);
    const [userNickname, setUserNickname] = useState('');
    const [userComments, setUserComments] = useState([]);
    const [commentsLoading, setCommentsLoading] = useState(false);
    const [userMessage, setUserMessage] = useState('');
    const [suspendModal, setSuspendModal] = useState(null);
    const [suspendError, setSuspendError] = useState('');
    const [suspendSubmitting, setSuspendSubmitting] = useState(false);

    const findUser = async (event) => {
        event.preventDefault();
        setUserMessage('');
        setSelectedUser(null);
        setUserComments([]);

        const keyword = userNickname.trim();
        if (!keyword) {
            setUserMessage('검색할 회원 ID 또는 닉네임을 입력해주세요.');
            return;
        }

        const queries = /^\d+$/.test(keyword)
            ? [`id=${keyword}`, `nickname=${encodeURIComponent(keyword)}`]
            : [`nickname=${encodeURIComponent(keyword)}`];

        let result = null;
        let lastError = null;

        for (const query of queries) {
            try {
                const data = await getUserInfo(query);
                result = Array.isArray(data) ? data[0] : data;
                if (result) break;
            } catch (error) {
                lastError = error;
            }
        }

        if (!result) {
            setUserMessage(lastError?.message || '일치하는 회원이 없습니다.');
            return;
        }

        setSelectedUser(result);
        setCommentsLoading(true);

        try {
            const comments = await getUserComments(result.id);
            setUserComments(Array.isArray(comments) ? comments : []);
        } catch (error) {
            setUserMessage(`회원 정보는 조회했지만 댓글을 불러오지 못했습니다: ${error.message}`);
        } finally {
            setCommentsLoading(false);
        }
    };

    const openSuspendModal = (release) => {
        if (!selectedUser?.id) return;

        setSuspendError('');
        setSuspendModal({
            mode: release ? 'release' : 'suspend',
            days: '7',
            reason: release ? '관리자에 의한 정지 해제' : '',
        });
    };

    const closeSuspendModal = () => {
        if (suspendSubmitting) return;
        setSuspendModal(null);
        setSuspendError('');
    };

    const handleSuspendDaysChange = (event) => {
        setSuspendModal((current) =>
            current ? { ...current, days: event.target.value } : current
        );
        setSuspendError('');
    };

    const handleSuspendReasonChange = (event) => {
        setSuspendModal((current) =>
            current ? { ...current, reason: event.target.value } : current
        );
        setSuspendError('');
    };

    const submitSuspension = async (event) => {
        event.preventDefault();
        if (!selectedUser?.id || !suspendModal) return;

        const release = suspendModal.mode === 'release';
        const reason = suspendModal.reason.trim();
        let suspendTime = 1;

        if (!release) {
            suspendTime = Number(suspendModal.days);
            if (!Number.isInteger(suspendTime) || suspendTime < 1) {
                setSuspendError('정지 기간은 1일 이상의 정수로 입력해주세요.');
                return;
            }
        }

        if (!reason) {
            setSuspendError(release ? '해제 사유를 입력해주세요.' : '정지 사유를 입력해주세요.');
            return;
        }

        setSuspendSubmitting(true);
        setSuspendError('');

        try {
            await suspendUser(selectedUser.id, suspendTime, reason, release);

            setSelectedUser(await getUserInfo(`id=${selectedUser.id}`));
            setSuspendModal(null);
            setUserMessage(release ? '회원 정지가 해제되었습니다.' : '회원이 정지되었습니다.');
        } catch (error) {
            setSuspendError(error.message);
        } finally {
            setSuspendSubmitting(false);
        }
    };

    return {
        selectedUser,
        userNickname,
        setUserNickname,
        userComments,
        commentsLoading,
        userMessage,
        suspendModal,
        suspendError,
        suspendSubmitting,
        findUser,
        openSuspendModal,
        closeSuspendModal,
        handleSuspendDaysChange,
        handleSuspendReasonChange,
        submitSuspension,
    };
}
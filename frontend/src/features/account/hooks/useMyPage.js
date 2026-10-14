import { useEffect, useState } from 'react';
import { getEvent,
    deleteComment as deleteCommentRequest,
    getMyComments,
    deleteCurrentUser,
    updatePassword as updatePasswordRequest,
} from '@/api/index.js';

const COMMENTS_PER_PAGE = 10;

const getCommentId = (comment) => {
    const id = comment.id ?? comment.commentId ?? comment.comment_id;
    return id === null || id === undefined || id === '' ? null : String(id);
};



const INITIAL_PASSWORD_FORM = {
    currentPassword: '',
    updatePassword: '',
    passwordConfirm: '',
};

export function useMyPage({ user, onLogout, onBack }) {
    const [passwordForm, setPasswordForm] = useState(INITIAL_PASSWORD_FORM);
    const [comments, setComments] = useState([]);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const [commentToDelete, setCommentToDelete] = useState(null);
    const [eventTitles, setEventTitles] = useState({});
    const [commentPage, setCommentPage] = useState(0);
    const [showQuitModal, setShowQuitModal] = useState(false);
    const [quitPassword, setQuitPassword] = useState('');
    const [quitError, setQuitError] = useState('');
    const [quitLoading, setQuitLoading] = useState(false);
    const [passwordMessage, setPasswordMessage] = useState('');
    const [passwordError, setPasswordError] = useState('');

    useEffect(() => {
        let cancelled = false;

        const loadComments = async () => {
            try {
                const data = await getMyComments();
                if (cancelled) return;

                const serverComments = Array.isArray(data)
                    ? data
                    : data?.comments || data?.content || [];

                setComments(serverComments);
            } catch (requestError) {
                if (cancelled) return;

                setComments([]);
                setError(requestError.message || '댓글을 불러오지 못했습니다.');
            }
        };

        loadComments();

        return () => {
            cancelled = true;
        };
    }, [user]);

    useEffect(() => {
        let cancelled = false;
        const eventIds = [
            ...new Set(comments.map((comment) => comment.eventId).filter(Boolean)),
        ];

        if (!eventIds.length) {
            setEventTitles({});
            return undefined;
        }

        Promise.all(eventIds.map(async (eventId) => {
            try {
                const event = await getEvent(eventId);
                return [String(eventId), event.title || `이벤트 ${eventId}`];
            } catch {
                return [String(eventId), `이벤트 ${eventId}`];
            }
        })).then((entries) => {
            if (!cancelled) {
                setEventTitles(Object.fromEntries(entries));
            }
        });

        return () => {
            cancelled = true;
        };
    }, [comments]);

    const currentCommentPage = Math.min(
        commentPage,
        Math.max(0, Math.ceil(comments.length / COMMENTS_PER_PAGE) - 1)
    );
    const commentPageCount = Math.ceil(comments.length / COMMENTS_PER_PAGE);

    const update = (setForm, clearLocalFeedback) => (event) => {
        const { name, value } = event.target;
        setForm((current) => ({ ...current, [name]: value }));
        setError('');
        setMessage('');
        clearLocalFeedback?.();
    };

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

    const deleteComment = async (comment) => {
        const commentId = getCommentId(comment);
        if (!commentId) return;

        setError('');
        setMessage('');

        try {
            await deleteCommentRequest(commentId);
            setComments((current) =>
                current.filter((item) => getCommentId(item) !== commentId)
            );
            setCommentPage((current) =>
                Math.min(
                    current,
                    Math.max(
                        0,
                        Math.ceil((comments.length - 1) / COMMENTS_PER_PAGE) - 1
                    )
                )
            );
            setMessage('댓글이 삭제되었습니다.');
        } catch (requestError) {
            setError(requestError.message || '댓글 삭제에 실패했습니다.');
        } finally {
            setCommentToDelete(null);
        }
    };

    return {
        passwordForm,
        setPasswordForm,
        comments,
        commentToDelete,
        setCommentToDelete,
        eventTitles,
        setCommentPage,
        currentCommentPage,
        commentPageCount,
        showQuitModal,
        quitPassword,
        setQuitPassword,
        quitError,
        setQuitError,
        quitLoading,
        update,
        updatePassword,
        openQuitModal,
        closeQuitModal,
        handleQuit,
        deleteComment,
        getCommentId,
        COMMENTS_PER_PAGE,
        passwordMessage,
        passwordError,
        setPasswordMessage,
        setPasswordError,
    };
}
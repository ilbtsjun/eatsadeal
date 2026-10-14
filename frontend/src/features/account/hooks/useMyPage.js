import { useEffect, useState } from 'react';
import { getEvent,
    deleteComment as deleteCommentRequest,
    getMyComments,
    deleteCurrentUser,
    getCurrentUser,
    updateCurrentUser,
    updatePassword as updatePasswordRequest,
} from '@/api/index.js';

const COMMENTS_PER_PAGE = 10;

const getCommentId = (comment) => {
    const id = comment.id ?? comment.commentId ?? comment.comment_id;
    return id === null || id === undefined || id === '' ? null : String(id);
};

const toProfileForm = (profile = {}) => ({
    nickname: profile.nickname || '',
    name: profile.name || '',
    phoneNumber: profile.phoneNumber || '',
    birth: profile.birth || '',
});

const INITIAL_PASSWORD_FORM = {
    currentPassword: '',
    updatePassword: '',
    passwordConfirm: '',
};

export function useMyPage({ user, onLogout, onBack, onUserUpdate }) {
    const [profile, setProfile] = useState(user || {});
    const [profileForm, setProfileForm] = useState(() => toProfileForm(user));
    const [passwordForm, setPasswordForm] = useState(INITIAL_PASSWORD_FORM);
    const [comments, setComments] = useState([]);
    const [message, setMessage] = useState('');
    const [error, setError] = useState('');
    const [editingProfile, setEditingProfile] = useState(false);
    const [commentToDelete, setCommentToDelete] = useState(null);
    const [eventTitles, setEventTitles] = useState({});
    const [commentPage, setCommentPage] = useState(0);
    const [showQuitModal, setShowQuitModal] = useState(false);
    const [quitPassword, setQuitPassword] = useState('');
    const [quitError, setQuitError] = useState('');
    const [quitLoading, setQuitLoading] = useState(false);
    const [profileMessage, setProfileMessage] = useState('');
    const [profileError, setProfileError] = useState('');
    const [passwordMessage, setPasswordMessage] = useState('');
    const [passwordError, setPasswordError] = useState('');

    useEffect(() => {
        let cancelled = false;

        const loadPageData = async () => {
            const results = await Promise.allSettled([
                getCurrentUser(),
                getMyComments(),
            ]);

            if (cancelled) return;

            const [profileResult, commentsResult] = results;

            if (profileResult.status === 'fulfilled') {
                const data = profileResult.value;
                setProfile(data);
                setProfileForm(toProfileForm(data));
            }

            if (profileResult.status === 'rejected') {
                setProfileError(profileResult.reason?.message || '회원정보를 불러오지 못했습니다.');
            }

            if (commentsResult.status === 'fulfilled') {
                const data = commentsResult.value;
                const serverComments = Array.isArray(data)
                    ? data
                    : data?.comments || data?.content || [];
                setComments(serverComments);
            } else {
                setComments([]);
                setError((current) => current || commentsResult.reason?.message || '댓글을 불러오지 못했습니다.');
            }
        };

        loadPageData();

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

    const startEditingProfile = () => {
        setEditingProfile(true);
        setProfileMessage('정보를 수정한 뒤 완료를 눌러 저장하세요.');
        setProfileError('');
    };

    const cancelEditingProfile = () => {
        setEditingProfile(false);
        setProfileForm(toProfileForm(profile));
        setProfileError('');
        setProfileMessage('');
    };

    const updateProfile = async (event) => {
        event.preventDefault();
        setProfileError('');
        setProfileMessage('');

        try {
            await updateCurrentUser({
                nickname: profileForm.nickname.trim(),
                name: profileForm.name.trim(),
                phoneNumber: profileForm.phoneNumber.trim(),
                birth: profileForm.birth || null,
            });

            const saved = await getCurrentUser();
            setProfile(saved);
            setProfileForm(toProfileForm(saved));

            if (saved.nickname && saved.nickname !== user?.nickname) {
                onUserUpdate?.({ nickname: saved.nickname });
            }

            setEditingProfile(false);
            setProfileMessage('회원정보가 수정되었습니다.');
        } catch (requestError) {
            setProfileError(requestError.message || '회원정보 수정에 실패했습니다.');
        }
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
        profile,
        profileForm,
        setProfileForm,
        passwordForm,
        setPasswordForm,
        comments,
        message,
        error,
        setError,
        setMessage,
        editingProfile,
        setEditingProfile,
        commentToDelete,
        setCommentToDelete,
        eventTitles,
        commentPage,
        setCommentPage,
        currentCommentPage,
        commentPageCount,
        showQuitModal,
        quitPassword,
        setQuitPassword,
        quitError,
        quitLoading,
        update,
        startEditingProfile,
        cancelEditingProfile,
        updateProfile,
        updatePassword,
        openQuitModal,
        closeQuitModal,
        handleQuit,
        deleteComment,
        getCommentId,
        COMMENTS_PER_PAGE,
        setQuitError,
        profileMessage,
        profileError,
        passwordMessage,
        passwordError,
        setProfileMessage,
        setProfileError,
        setPasswordMessage,
        setPasswordError,
    };
}
import { useMemo, useState } from 'react';
import {
    getListComments,
    createComment as requestCreateComment,
    updateComment,
    deleteComment,
    hideComment,
    unhideComment,
} from '@/api/index.js';
import { getCommentId } from '../utils/commentTree.js';
import { useFetch } from '@/common/hooks/useFetch.js';
import { usePending } from '@/common/hooks/usePending.js';

const COMMENT_ACTIONS = {
    delete: {
        request: deleteComment,
        dialog: { title: '댓글 삭제', message: '이 댓글을 삭제하시겠습니까?', confirmLabel: '삭제' },
    },
    hide: {
        request: hideComment,
        dialog: { title: '댓글 숨김', message: '이 댓글을 숨김 처리하시겠습니까?', confirmLabel: '숨김' },
    },
    unhide: {
        request: unhideComment,
        dialog: { title: '숨김 해제', message: '이 댓글의 숨김을 해제하시겠습니까?', confirmLabel: '해제' },
    },
};

const REFRESH_FAILED_MESSAGE = '저장되었지만 댓글 목록을 새로고침하지 못했습니다. 잠시 후 다시 시도해주세요.';

const fetchComments = (eventId) =>
    getListComments(eventId).then((data) => (Array.isArray(data) ? data : []));

export function useComments(eventId, user, onLoginClick) {
    const request = useMemo(() => () => fetchComments(eventId), [eventId]);
    const {
        data: comments,
        loading: commentLoading,
        loadError,
        reload,
        refresh,
    } = useFetch(request, { initialData: [], fallbackMessage: '댓글을 불러오지 못했습니다.' });
    const [commentText, setCommentText] = useState('');
    const [writeError, setWriteError] = useState('');
    const [submitting, withSubmitting] = usePending();
    const [editingId, setEditingId] = useState(null);
    const [editText, setEditText] = useState('');
    const [editError, setEditError] = useState('');
    const [replyTargetId, setReplyTargetId] = useState(null);
    const [replyText, setReplyText] = useState('');
    const [actionTarget, setActionTarget] = useState(null);
    const [actionError, setActionError] = useState('');
    const [actionRunning, withAction] = usePending();

    const isAdmin = user?.role === 'ADMIN';

    const isMine = (comment) => Boolean(user) && comment.isMine === true;

    const refreshAfterWrite = async () => {
        try {
            await refresh();
        } catch {
            setWriteError(REFRESH_FAILED_MESSAGE);
        }
    };

    const createComment = (content, parentId = null) => withSubmitting(async () => {
        setWriteError('');
        try {
            await requestCreateComment(eventId, content, parentId);
        } catch (error) {
            setWriteError(error.message);
            return false;
        }
        await refreshAfterWrite();
        return true;
    });

    const handleCommentSubmit = async (event) => {
        event.preventDefault();
        const content = commentText.trim();
        if (!user || !content) return;

        if (await createComment(content)) setCommentText('');
    };

    const openReply = (comment) => {
        if (!user) {
            onLoginClick?.();
            return;
        }

        setEditingId(null);
        setReplyTargetId(getCommentId(comment));
        setReplyText('');
    };

    const closeReply = () => {
        setReplyTargetId(null);
        setReplyText('');
    };

    const handleReplySubmit = async (event, parentComment) => {
        event.preventDefault();
        const content = replyText.trim();
        if (!user || !content) return;

        if (await createComment(content, getCommentId(parentComment))) closeReply();
    };

    const openEdit = (comment) => {
        setReplyTargetId(null);
        setEditError('');
        setEditingId(getCommentId(comment));
        setEditText(comment.content || comment.comment || '');
    };

    const closeEdit = () => {
        setEditingId(null);
        setEditError('');
    };

    const handleEditSubmit = async (event, comment) => {
        event.preventDefault();
        const content = editText.trim();
        if (!content) return;

        await withSubmitting(async () => {
            setEditError('');
            try {
                await updateComment(getCommentId(comment), content);
            } catch (error) {
                setEditError(error.message);
                return;
            }
            setEditingId(null);
            await refreshAfterWrite();
        });
    };

    const openAction = (kind, comment) => {
        const id = getCommentId(comment);
        if (!id || !COMMENT_ACTIONS[kind]) return;
        setActionError('');
        setActionTarget({ kind, id });
    };

    const closeAction = () => {
        if (!actionRunning) setActionTarget(null);
    };

    const confirmAction = async () => {
        if (!actionTarget) return;

        await withAction(async () => {
            try {
                await COMMENT_ACTIONS[actionTarget.kind].request(actionTarget.id);
            } catch (error) {
                setActionError(error.message);
                return;
            }
            setActionTarget(null);
            await refreshAfterWrite();
        });
    };

    const actionDialog = actionTarget ? COMMENT_ACTIONS[actionTarget.kind].dialog : null;

    return {
        comments,
        commentLoading,
        loadError,
        reload,
        commentText,
        setCommentText,
        writeError,
        submitting,
        editingId,
        editText,
        setEditText,
        editError,
        replyTargetId,
        replyText,
        setReplyText,
        isAdmin,
        isMine,
        handleCommentSubmit,
        openReply,
        closeReply,
        handleReplySubmit,
        openEdit,
        closeEdit,
        handleEditSubmit,
        openAction,
        closeAction,
        confirmAction,
        actionDialog,
        actionError,
        actionRunning,
    };
}

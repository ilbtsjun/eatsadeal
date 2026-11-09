import { useCallback, useEffect, useState, useRef } from 'react';
import {
    getListComments,
    createComment as requestCreateComment,
    updateComment,
    deleteComment,
    hideComment,
    unhideComment,
} from '@/api/index.js';
import { getCommentId } from '../utils/commentTree.js';
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

export function useComments(eventId, user, onLoginClick) {
    const [comments, setComments] = useState([]);
    const [commentLoading, setCommentLoading] = useState(true);
    const [commentText, setCommentText] = useState('');
    const [commentError, setCommentError] = useState('');
    const [submitting, withSubmitting] = usePending();
    const [actionTarget, setActionTarget] = useState(null);
    const [actionError, setActionError] = useState('');
    const [actionRunning, withAction] = usePending();
    const [editingId, setEditingId] = useState(null);
    const [editText, setEditText] = useState('');
    const [replyTargetId, setReplyTargetId] = useState(null);
    const [replyText, setReplyText] = useState('');
    const [prevEventId, setPrevEventId] = useState(eventId);

    if (prevEventId !== eventId) {
        setPrevEventId(eventId);
        setCommentLoading(true);
    }

    const isAdmin = user?.role === 'ADMIN';

    const isMine = useCallback((comment) => (
        Boolean(user) && (
            comment.isMine === true
            || (comment.userId != null && comment.userId === user.id)
            || (comment.nickname != null && comment.nickname === user.nickname)
        )
    ), [user]);

    const latestRequestId = useRef(0);

    const loadComments = useCallback(() => {
        const requestId = ++latestRequestId.current;
        const isLatest = () => requestId === latestRequestId.current;

        return getListComments(eventId)
            .then((data) => {
                if (isLatest()) {
                    setComments(Array.isArray(data) ? data : []);
                    setCommentError('');
                }
            })
            .catch((error) => {
                if (isLatest()) {
                    setCommentError(error.message);
                }
            })
            .finally(() => {
                if (isLatest()) {
                    setCommentLoading(false);
                }
            });
    }, [eventId]);

    useEffect(() => {
        loadComments();

        return () => {
            latestRequestId.current += 1;
        };
    }, [loadComments]);

    const createComment = async (content, parentId = null) => {
        return withSubmitting(async () => {
            try {
                await requestCreateComment(eventId, content, parentId);
                setCommentError('');
                await loadComments();
                return true;
            } catch (error) {
                setCommentError(error.message);
                return false;
            }
        });
    };

    const handleCommentSubmit = async (event) => {
        event.preventDefault();
        const content = commentText.trim();
        if (!user || !content || submitting) return;

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

    const handleReplySubmit = async (event, parentComment) => {
        event.preventDefault();
        const content = replyText.trim();
        if (!user || !content || submitting) return;

        if (await createComment(content, getCommentId(parentComment))) {
            setReplyTargetId(null);
            setReplyText('');
        }
    };

    const openEdit = (comment) => {
        setReplyTargetId(null);
        setEditingId(getCommentId(comment));
        setEditText(comment.content || comment.comment || '');
    };

    const handleEditSubmit = async (event, comment) => {
        event.preventDefault();
        const content = editText.trim();
        if (!content || submitting) return;

        await withSubmitting(async () => {
            try {
                await updateComment(getCommentId(comment), content);
                setEditingId(null);
                setCommentError('');
                await loadComments();
            } catch (error) {
                setCommentError(error.message);
            }
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
                setActionTarget(null);
                setCommentError('');
                await loadComments();
            } catch (error) {
                setActionError(error.message);
            }
        });
    };

    const actionDialog = actionTarget ? COMMENT_ACTIONS[actionTarget.kind].dialog : null;

    return {
        comments,
        commentLoading,
        commentText,
        setCommentText,
        commentError,
        submitting,
        editingId,
        setEditingId,
        editText,
        setEditText,
        replyTargetId,
        setReplyTargetId,
        replyText,
        setReplyText,
        isAdmin,
        isMine,
        loadComments,
        handleCommentSubmit,
        openReply,
        handleReplySubmit,
        openEdit,
        handleEditSubmit,
        openAction,
        closeAction,
        confirmAction,
        actionDialog,
        actionError,
        actionRunning,
    };
}
import { useCallback, useEffect, useState } from 'react';
import {
    getListComments,
    createComment as requestCreateComment,
    updateComment,
    deleteComment,
    hideComment,
    unhideComment,
} from '@/api/index.js';
import { getCommentId } from '../utils/commentTree.js';

export function useComments(eventId, user, onLoginClick) {
    const [comments, setComments] = useState([]);
    const [commentLoading, setCommentLoading] = useState(true);
    const [commentText, setCommentText] = useState('');
    const [commentError, setCommentError] = useState('');
    const [submitting, setSubmitting] = useState(false);
    const [editingId, setEditingId] = useState(null);
    const [editText, setEditText] = useState('');
    const [replyTargetId, setReplyTargetId] = useState(null);
    const [replyText, setReplyText] = useState('');

    const isAdmin = user?.role === 'ADMIN';

    const isMine = useCallback((comment) => (
        Boolean(user) && (
            comment.isMine === true
            || (comment.userId != null && comment.userId === user.id)
            || (comment.nickname != null && comment.nickname === user.nickname)
        )
    ), [user]);

    const loadComments = useCallback(async () => {
        try {
            const data = await getListComments(eventId);
            setComments(Array.isArray(data) ? data : []);
        } catch (error) {
            setCommentError(error.message);
        } finally {
            setCommentLoading(false);
        }
    }, [eventId]);

    useEffect(() => {
        setCommentLoading(true);
        loadComments();
    }, [loadComments]);

    const createComment = async (content, parentId = null) => {
        setSubmitting(true);
        try {
            await requestCreateComment(eventId, content, parentId);
            setCommentError('');
            await loadComments();
            return true;
        } catch (error) {
            setCommentError(error.message);
            return false;
        } finally {
            setSubmitting(false);
        }
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

        setSubmitting(true);
        try {
            await updateComment(getCommentId(comment), content);
            setEditingId(null);
            setCommentError('');
            await loadComments();
        } catch (error) {
            setCommentError(error.message);
        } finally {
            setSubmitting(false);
        }
    };

    const deleteCommentAction = async (comment) => {
        const id = getCommentId(comment);
        if (!id || !window.confirm('이 댓글을 삭제하시겠습니까?')) return;

        try {
            await deleteComment(id);
            setCommentError('');
            await loadComments();
        } catch (error) {
            setCommentError(error.message);
        }
    };

    const hideCommentAction = async (comment) => {
        const id = getCommentId(comment);
        if (!id || !window.confirm('이 댓글을 숨김 처리하시겠습니까?')) return;

        try {
            await hideComment(id);
            setCommentError('');
            await loadComments();
        } catch (error) {
            setCommentError(error.message);
        }
    };

    const unhideCommentAction = async (comment) => {
        const id = getCommentId(comment);
        if (!id || !window.confirm('이 댓글의 숨김을 해제하시겠습니까?')) return;

        try {
            await unhideComment(id);
            setCommentError('');
            await loadComments();
        } catch (error) {
            setCommentError(error.message);
        }
    };

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
        deleteCommentAction,
        hideCommentAction,
        unhideCommentAction,
    };
}
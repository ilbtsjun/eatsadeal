import { useEffect, useMemo, useState } from 'react';
import {
    getEvent,
    getMyComments,
    deleteComment as deleteCommentRequest
} from '@/api/index.js';
import { useFetch } from '@/common/hooks/useFetch.js';

const COMMENTS_PER_PAGE = 10;

const getCommentId = (comment) => comment?.commentId ? String(comment.commentId) : null;

const toCommentList = (data) => (Array.isArray(data) ? data : data?.comments || data?.content || []);

export function useUserComments({ user }) {
    const request = useMemo(() => (user ? () => getMyComments().then(toCommentList) : null), [user]);
    const {
        data: comments,
        setData: setComments,
        loading,
        loadError,
        reload,
    } = useFetch(request, { initialData: [], fallbackMessage: '댓글을 불러오지 못했습니다.' });
    const [commentPage, setCommentPage] = useState(0);
    const [commentToDelete, setCommentToDelete] = useState(null);
    const [deleting, setDeleting] = useState(false);
    const [deleteError, setDeleteError] = useState('');
    const [eventTitles, setEventTitles] = useState({});

    useEffect(() => {
        let cancelled = false;
        const eventIds = [
            ...new Set(comments.map((comment) => comment.eventId).filter(Boolean)),
        ];
        if (!eventIds.length) return undefined;

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

    const commentPageCount = Math.ceil(comments.length / COMMENTS_PER_PAGE);
    const currentCommentPage = Math.min(commentPage, Math.max(0, commentPageCount - 1));
    const pageStart = currentCommentPage * COMMENTS_PER_PAGE;
    const pagedComments = comments.slice(pageStart, pageStart + COMMENTS_PER_PAGE);

    const getEventTitle = (eventId) => eventTitles[String(eventId)] || `이벤트 ${eventId || ''}`;

    const goPrevPage = () => setCommentPage(Math.max(0, currentCommentPage - 1));
    const goNextPage = () => setCommentPage(Math.min(commentPageCount - 1, currentCommentPage + 1));

    const openDelete = (comment) => {
        setDeleteError('');
        setCommentToDelete(comment);
    };

    const closeDelete = () => {
        if (!deleting) setCommentToDelete(null);
    };

    const deleteComment = async () => {
        const commentId = getCommentId(commentToDelete);
        if (!commentId || deleting) return;

        setDeleting(true);
        setDeleteError('');
        try {
            await deleteCommentRequest(commentId);
            setComments((current) =>
                current.map((item) =>
                    getCommentId(item) === commentId
                        ? { ...item, status: 'DELETED', content: '삭제된 댓글입니다.' }
                        : item
                )
            );
            setCommentToDelete(null);
        } catch (requestError) {
            setDeleteError(requestError.message || '댓글을 삭제하지 못했습니다.');
        } finally {
            setDeleting(false);
        }
    };

    return {
        pagedComments,
        pageStart,
        loading,
        loadError,
        reload,
        currentCommentPage,
        commentPageCount,
        goPrevPage,
        goNextPage,
        getEventTitle,
        commentToDelete,
        deleting,
        deleteError,
        openDelete,
        closeDelete,
        deleteComment,
    };
}

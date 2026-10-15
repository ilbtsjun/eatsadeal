import {useEffect, useState} from "react";
import {
    getEvent,
    getMyComments,
    deleteComment as deleteCommentRequest
} from '@/api/index.js';

const COMMENTS_PER_PAGE = 10;
const getCommentId = (comment) => comment?.commentId ? String(comment.commentId) : null;

export function useUserComments({ user, }){
    const [comments, setComments] = useState([]);
    const [commentPage, setCommentPage] = useState(0);
    const [commentToDelete, setCommentToDelete] = useState(null);
    const [eventTitles, setEventTitles] = useState({});

    const currentCommentPage = Math.min(commentPage, Math.max(0, Math.ceil(comments.length / COMMENTS_PER_PAGE) - 1));

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
                console.error(requestError);
                throw(requestError);
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

    const deleteComment = async (comment) => {
        const commentId = getCommentId(comment);
        if (!commentId) return;

        try {
            await deleteCommentRequest(commentId);
            setComments((current) =>
                current.map((item) =>
                    getCommentId(item) === commentId
                        ? { ...item, status: 'DELETED', content: '삭제된 댓글입니다.' }
                        : item
                )
            );
        } catch (requestError) {
            console.error(requestError);
            throw(requestError);
        } finally {
            setCommentToDelete(null);
        }
    };

    return {
        COMMENTS_PER_PAGE,
        comments,
        deleteComment,
        currentCommentPage,
        setCommentPage,
        getCommentId,
        commentToDelete,
        setCommentToDelete,
        commentPageCount,
        eventTitles,
    }
}
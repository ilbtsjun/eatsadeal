import { useUserComments } from '../hooks/useUserComments.js'
import { formatDateTime } from '@/utils/Dateutil.js';
import Modal from '@/common/components/Modal.jsx';

export default function UserComments({ user, onOpenEvent }){
    const {
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
    } = useUserComments({ user });

    return(
        <>
            <section className="mypage-panel">
                <h2>내가 작성한 댓글</h2>
                {comments.length ? <>
                    {comments.slice(
                        currentCommentPage * COMMENTS_PER_PAGE,
                        (currentCommentPage + 1) * COMMENTS_PER_PAGE)
                        .map((comment, index) => {
                        const number = currentCommentPage * COMMENTS_PER_PAGE + index + 1;
                        const eventId = comment.eventId;
                        return <article className="my-comment"
                                        key={getCommentId(comment) || `${eventId}-${comment.createdAt}`}>
                            <div className="my-comment-meta">
                                <span className="my-comment-number">{number}.</span>
                                <span>{formatDateTime(comment.createdAt)}</span>
                                { comment.status !== 'DELETED'
                                    && <button type="button"
                                        onClick={() => setCommentToDelete(comment)}>삭제</button>
                                }
                            </div>
                            <button type="button"
                                    className="my-comment-event"
                                    onClick={() => eventId && onOpenEvent?.({
                                        id: eventId,
                                        title: eventTitles[String(eventId)] || `이벤트 ${eventId}` })}>
                                {eventTitles[String(eventId)] || `이벤트 ${eventId || ''}`}</button>
                            <p>{comment.content || comment.comment}</p>
                        </article>;
                    })}
                    {commentPageCount > 1
                        && <div className="comment-pagination">
                            <button type="button"
                                    disabled={currentCommentPage === 0}
                                    onClick={() => setCommentPage((page) => page - 1)}>이전</button>
                            <span>{currentCommentPage + 1} / {commentPageCount}</span>
                            <button type="button" disabled={currentCommentPage >= commentPageCount - 1}
                                    onClick={() => setCommentPage((page) => page + 1)}>다음</button>
                        </div>}
                </> : <p className="mypage-empty">작성한 댓글이 없습니다.</p>}</section>
            {commentToDelete &&
                <Modal backdropClassName="confirm-backdrop" onClose={() => setCommentToDelete(null)}>
                    <div className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="my-comment-delete-title">
                        <h2 id="my-comment-delete-title">댓글 삭제</h2>
                        <p>이 댓글을 삭제하시겠습니까?</p>
                        <div className="confirm-dialog-actions">
                            <button type="button"
                                    className="confirm-cancel"
                                    onClick={() => setCommentToDelete(null)}>취소</button>
                            <button type="button"
                                    className="confirm-delete"
                                    onClick={() => deleteComment(commentToDelete)}>삭제</button>
                        </div>
                    </div>
                </Modal>
            }
        </>
    )
}
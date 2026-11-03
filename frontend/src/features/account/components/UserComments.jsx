import { useUserComments } from '../hooks/useUserComments.js';
import { formatDateTime } from '@/common/utils/DateUtil.js';
import Modal from '@/common/components/Modal.jsx';
import StatusMessage from '@/common/components/StatusMessage.jsx';

export default function UserComments({ user, onOpenEvent }){
    const {
        pagedComments,
        pageStart,
        loading,
        error,
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
    } = useUserComments({ user });

    return(
        <>
            <section className="mypage-panel">
                <h2>내가 작성한 댓글</h2>
                {loading ? (
                    <StatusMessage type="loading" size="inline" message="댓글을 불러오는 중입니다." />
                ) : error ? (
                    <StatusMessage
                        type="error"
                        size="inline"
                        title="댓글을 불러오지 못했어요"
                        message={error}
                        onRetry={reload}
                    />
                ) : pagedComments.length ? <>
                    {pagedComments.map((comment, index) => {
                        const eventId = comment.eventId;
                        return <article className="my-comment"
                                        key={comment.commentId ?? `${eventId}-${comment.createdAt}`}>
                            <div className="my-comment-meta">
                                <span className="my-comment-number">{pageStart + index + 1}.</span>
                                <span>{formatDateTime(comment.createdAt)}</span>
                                { comment.status !== 'DELETED'
                                    && <button type="button"
                                        onClick={() => openDelete(comment)}>삭제</button>
                                }
                            </div>
                            <button type="button"
                                    className="my-comment-event"
                                    onClick={() => eventId && onOpenEvent?.({
                                        id: eventId,
                                        title: getEventTitle(eventId) })}>
                                {getEventTitle(eventId)}</button>
                            <p>{comment.content || comment.comment}</p>
                        </article>;
                    })}
                    {commentPageCount > 1
                        && <div className="comment-pagination">
                            <button type="button"
                                    disabled={currentCommentPage === 0}
                                    onClick={goPrevPage}>이전</button>
                            <span>{currentCommentPage + 1} / {commentPageCount}</span>
                            <button type="button" disabled={currentCommentPage >= commentPageCount - 1}
                                    onClick={goNextPage}>다음</button>
                        </div>}
                </> : <p className="mypage-empty">작성한 댓글이 없습니다.</p>}</section>
            {commentToDelete &&
                <Modal backdropClassName="confirm-backdrop" onClose={closeDelete} locked={deleting}>
                    <div className="confirm-dialog" role="dialog" aria-modal="true" aria-labelledby="my-comment-delete-title">
                        <h2 id="my-comment-delete-title">댓글 삭제</h2>
                        <p>이 댓글을 삭제하시겠습니까?</p>
                        {deleteError && <p className="confirm-error" role="alert">{deleteError}</p>}
                        <div className="confirm-dialog-actions">
                            <button type="button"
                                    className="confirm-cancel"
                                    disabled={deleting}
                                    onClick={closeDelete}>취소</button>
                            <button type="button"
                                    className="confirm-delete"
                                    disabled={deleting}
                                    onClick={deleteComment}>{deleting ? '삭제 중...' : '삭제'}</button>
                        </div>
                    </div>
                </Modal>
            }
        </>
    )
}

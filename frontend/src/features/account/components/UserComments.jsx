import { useUserComments } from '../hooks/useUserComments.js';
import { formatDateTime } from '@/common/utils/DateUtil.js';
import ConfirmDialog from '@/common/components/ConfirmDialog.jsx';
import StatusMessage from '@/common/components/StatusMessage.jsx';

export default function UserComments({ user, onOpenEvent }){
    const {
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
    } = useUserComments({ user });

    return(
        <>
            <section className="mypage-panel">
                <h2>내가 작성한 댓글</h2>
                {loading ? (
                    <StatusMessage type="loading" size="inline" message="댓글을 불러오는 중입니다." />
                ) : loadError ? (
                    <StatusMessage
                        type="error"
                        size="inline"
                        title="댓글을 불러오지 못했어요"
                        message={loadError}
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
                                        title: getEventTitle(comment) })}>
                                {getEventTitle(comment)}</button>
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
                <ConfirmDialog
                    title="댓글 삭제"
                    message="이 댓글을 삭제하시겠습니까?"
                    confirmLabel="삭제"
                    pendingLabel="삭제 중..."
                    pending={deleting}
                    error={deleteError}
                    onConfirm={deleteComment}
                    onClose={closeDelete}
                />
            }
        </>
    )
}

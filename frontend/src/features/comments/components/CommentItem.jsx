import { formatDateTime } from '@/common/utils/DateUtil.js';
import { getCommentId, isDeletedComment, isHiddenComment } from '../utils/commentTree.js';
import CommentForm from './CommentForm.jsx';

const SUPPORTS_REPLIES = false;

export default function CommentItem({ comment, isReply = false, comments }) {
    const {
        isAdmin,
        isMine,
        submitting,
        editingId,
        editText,
        setEditText,
        editError,
        replyTargetId,
        replyText,
        setReplyText,
        openEdit,
        closeEdit,
        handleEditSubmit,
        openReply,
        closeReply,
        handleReplySubmit,
        openAction,
    } = comments;

    const commentId = getCommentId(comment);
    const hidden = isHiddenComment(comment);
    const deleted = isDeletedComment(comment);
    const active = !hidden && !deleted;
    const mine = isMine(comment);
    const editing = editingId === commentId;
    const replying = replyTargetId === commentId;
    const classNames = ['comment-item'];

    if (isReply)
        classNames.push('is-reply');
    if (hidden)
        classNames.push('is-hidden');
    if (deleted)
        classNames.push('is-deleted');

    return (
        <div className={classNames.join(' ')}>
            <div className="comment-meta">
                <strong>
                    {comment.nickname || '익명'}
                </strong>
                <span>{formatDateTime(comment.createdAt)}</span>
                {hidden && isAdmin && (
                    <span className="comment-hidden-badge">숨김 처리됨</span>
                )}
            </div>

            {editing ? (
                <>
                    <CommentForm
                        className="comment-edit-form"
                        value={editText}
                        onChange={setEditText}
                        onSubmit={(event) => handleEditSubmit(event, comment)}
                        submitLabel="저장"
                        submitting={submitting}
                        onCancel={closeEdit}
                        autoFocus
                    />
                    {editError && <p className="comment-error" role="alert">{editError}</p>}
                </>
            ) : (
                <p className={active ? undefined : 'comment-hidden-text'}>
                    {comment.content}
                </p>
            )}

            {!editing && (
                <div className="comment-actions">
                    {SUPPORTS_REPLIES && !isReply && active && (
                        <button
                            type="button"
                            className="comment-action-button"
                            onClick={() => openReply(comment)}
                        >
                            답글
                        </button>
                    )}
                    {mine && active && (
                        <button
                            type="button"
                            className="comment-action-button"
                            onClick={() => openEdit(comment)}
                        >
                            수정
                        </button>
                    )}
                    {mine && !deleted && (
                        <button
                            type="button"
                            className="comment-action-button comment-delete-button"
                            onClick={() => openAction('delete', comment)}
                        >
                            삭제
                        </button>
                    )}
                    {isAdmin && active && (
                        <button
                            type="button"
                            className="comment-action-button comment-hide-button"
                            onClick={() => openAction('hide', comment)}
                        >
                            숨김
                        </button>
                    )}
                    {isAdmin && hidden && (
                        <button
                            type="button"
                            className="comment-action-button comment-unhide-button"
                            onClick={() => openAction('unhide', comment)}
                        >
                            숨김 해제
                        </button>
                    )}
                </div>
            )}

            {replying && (
                <CommentForm
                    className="comment-reply-form"
                    value={replyText}
                    onChange={setReplyText}
                    onSubmit={(event) => handleReplySubmit(event, comment)}
                    placeholder={`${comment.nickname || '익명'}님에게 답글을 남겨주세요.`}
                    submitLabel="답글 작성"
                    submitting={submitting}
                    onCancel={closeReply}
                    autoFocus
                />
            )}
        </div>
    );
}

import { useComments } from '../hooks/useComments.js';
import { buildCommentTree, getCommentId, isHiddenComment, isDeletedComment,} from '../utils/commentTree.js';
import { formatDateTime } from '@/utils/DateUtil.jsx';

const COMMENT_MAX = 300;
const SUPPORTS_REPLIES = false;

export default function CommentSection({ eventId, user, onLoginClick }) {
    const {
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
        handleCommentSubmit,
        openReply,
        handleReplySubmit,
        openEdit,
        handleEditSubmit,
        deleteCommentAction,
        hideCommentAction,
        unhideCommentAction,
    } = useComments(eventId, user, onLoginClick);

    const commentTree = buildCommentTree(comments);
    const totalComments = commentTree.reduce((count, node) => count + 1 + node.replies.length, 0,);

    const renderComment = (comment, isReply = false) => {
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
            <div className={classNames.join(' ')} key={commentId}>
                <div className="comment-meta">
                    <strong>
                        {comment.nickname || comment.author || comment.userNickname || '익명'}
                    </strong>
                    <span>{formatDateTime(comment.createdAt)}</span>
                    {hidden && isAdmin && (
                        <span className="comment-hidden-badge">숨김 처리됨</span>
                    )}
                </div>

                {editing ? (
                    <form
                        className="comment-edit-form"
                        onSubmit={(event) => handleEditSubmit(event, comment)}
                    >
            <textarea
                value={editText}
                onChange={(event) => setEditText(event.target.value)}
                maxLength={COMMENT_MAX}
                required
                autoFocus
            />
                        <div className="comment-form-footer">
                            <span>{editText.length}/{COMMENT_MAX}</span>
                            <div className="comment-inline-buttons">
                                <button
                                    type="button"
                                    className="comment-action-button"
                                    onClick={() => setEditingId(null)}
                                >
                                    취소
                                </button>
                                <button type="submit" disabled={submitting}>저장</button>
                            </div>
                        </div>
                    </form>
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
                                onClick={() => deleteCommentAction(comment)}
                            >
                                삭제
                            </button>
                        )}
                        {isAdmin && active && (
                            <button
                                type="button"
                                className="comment-action-button comment-hide-button"
                                onClick={() => hideCommentAction(comment)}
                            >
                                숨김
                            </button>
                        )}
                        {isAdmin && hidden && (
                            <button
                                type="button"
                                className="comment-action-button comment-unhide-button"
                                onClick={() => unhideCommentAction(comment)}
                            >
                                숨김 해제
                            </button>
                        )}
                    </div>
                )}

                {replying && (
                    <form
                        className="comment-reply-form"
                        onSubmit={(event) => handleReplySubmit(event, comment)}
                    >
            <textarea
                value={replyText}
                onChange={(event) => setReplyText(event.target.value)}
                placeholder={`${comment.nickname || '익명'}님에게 답글을 남겨주세요.`}
                maxLength={COMMENT_MAX}
                required
                autoFocus
            />
                        <div className="comment-form-footer">
                            <span>{replyText.length}/{COMMENT_MAX}</span>
                            <div className="comment-inline-buttons">
                                <button
                                    type="button"
                                    className="comment-action-button"
                                    onClick={() => setReplyTargetId(null)}
                                >
                                    취소
                                </button>
                                <button type="submit" disabled={submitting}>답글 작성</button>
                            </div>
                        </div>
                    </form>
                )}
            </div>
        );
    };

    return (
        <section className="comments-section" aria-labelledby="comments-title">
            <div className="comments-heading">
                <h2 id="comments-title">댓글</h2>
                <span>{totalComments}개</span>
            </div>

            {user ? (
                <form className="comment-form" onSubmit={handleCommentSubmit}>
          <textarea
              value={commentText}
              onChange={(event) => setCommentText(event.target.value)}
              placeholder="이벤트에 대한 의견을 남겨주세요."
              maxLength={COMMENT_MAX}
              required
          />
                    <div className="comment-form-footer">
                        <span>{commentText.length}/{COMMENT_MAX}</span>
                        <button type="submit" disabled={submitting}>댓글 작성</button>
                    </div>
                </form>
            ) : (
                <div className="comment-login-guide">
                    <p>댓글을 작성하려면 로그인이 필요합니다.</p>
                    <button type="button" onClick={onLoginClick}>로그인하기</button>
                </div>
            )}

            {commentError && (
                <p className="comment-error" role="alert">{commentError}</p>
            )}

            <div className="comment-list">
                {commentLoading ? (
                    <p className="no-comments">댓글을 불러오는 중입니다...</p>
                ) : commentTree.length === 0 ? (
                    <p className="no-comments">첫 번째 댓글을 작성해보세요.</p>
                ) : (
                    commentTree.map(({ comment, replies }) => (
                        <div className="comment-thread" key={getCommentId(comment)}>
                            {renderComment(comment)}
                            {replies.length > 0 && (
                                <div className="comment-replies">
                                    {replies.map((reply) => renderComment(reply, true))}
                                </div>
                            )}
                        </div>
                    ))
                )}
            </div>
        </section>
    );
}
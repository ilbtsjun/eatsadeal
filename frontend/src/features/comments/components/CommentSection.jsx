import StatusMessage from '@/common/components/StatusMessage.jsx';
import ConfirmDialog from '@/common/components/ConfirmDialog.jsx';
import { useComments } from '../hooks/useComments.js';
import { buildCommentTree, getCommentId } from '../utils/commentTree.js';
import CommentForm from './CommentForm.jsx';
import CommentItem from './CommentItem.jsx';

export default function CommentSection({ eventId, user, onLoginClick }) {
    const commentState = useComments(eventId, user, onLoginClick);
    const {
        comments,
        commentLoading,
        loadError,
        reload,
        commentText,
        setCommentText,
        writeError,
        submitting,
        handleCommentSubmit,
        actionDialog,
        actionError,
        actionRunning,
        confirmAction,
        closeAction,
    } = commentState;

    const commentTree = buildCommentTree(comments);
    const totalComments = commentTree.reduce((count, node) => count + 1 + node.replies.length, 0);

    return (
        <section className="comments-section" aria-labelledby="comments-title">
            <div className="comments-heading">
                <h2 id="comments-title">댓글</h2>
                <span>{totalComments}개</span>
            </div>

            {user ? (
                <CommentForm
                    className="comment-form"
                    value={commentText}
                    onChange={setCommentText}
                    onSubmit={handleCommentSubmit}
                    placeholder="이벤트에 대한 의견을 남겨주세요."
                    submitLabel="댓글 작성"
                    submitting={submitting}
                />
            ) : (
                <div className="comment-login-guide">
                    <p>댓글을 작성하려면 로그인이 필요합니다.</p>
                    <button type="button" onClick={onLoginClick}>로그인하기</button>
                </div>
            )}

            {writeError && (
                <p className="comment-error" role="alert">{writeError}</p>
            )}

            <div className="comment-list">
                {commentLoading ? (
                    <StatusMessage type="loading" size="inline" message="댓글을 불러오는 중입니다." />
                ) : loadError ? (
                    <StatusMessage type="error" size="inline" message={loadError} onRetry={reload} />
                ) : commentTree.length === 0 ? (
                    <p className="no-comments">첫 번째 댓글을 작성해보세요.</p>
                ) : (
                    commentTree.map(({ comment, replies }) => (
                        <div className="comment-thread" key={getCommentId(comment)}>
                            <CommentItem comment={comment} comments={commentState} />
                            {replies.length > 0 && (
                                <div className="comment-replies">
                                    {replies.map((reply) => (
                                        <CommentItem
                                            key={getCommentId(reply)}
                                            comment={reply}
                                            isReply
                                            comments={commentState}
                                        />
                                    ))}
                                </div>
                            )}
                        </div>
                    ))
                )}
            </div>

            {actionDialog && (
                <ConfirmDialog
                    {...actionDialog}
                    pending={actionRunning}
                    error={actionError}
                    onConfirm={confirmAction}
                    onClose={closeAction}
                />
            )}
        </section>
    );
}

import StatusMessage from '@/common/components/StatusMessage.jsx';

export default function UserComments({
                                         comments,
                                         loading,
                                         error,
                                     }) {
    return (
        <div className="user-comments">
            <div className="user-comments-header">
                <h3>작성한 댓글</h3>
                <span>{comments.length}개</span>
            </div>

            {loading ? (
                <StatusMessage type="loading" size="inline" message="댓글을 불러오는 중입니다..." />
            ) : error ? (
                <StatusMessage type="error" size="inline" title="댓글을 불러오지 못했어요" message={error} />
            ) : comments.length === 0 ? (
                <p className="admin-empty">
                    작성한 댓글이 없습니다.
                </p>
            ) : (
                <div className="user-comment-list">
                    {comments.map((comment, index) => (
                        <div
                            className="user-comment-item"
                            key={`comment-${comment.commentId ?? comment.id ?? index}`}
                        >
                            <div className="user-comment-content">
                                {comment.content}
                            </div>
                            <div className="user-comment-meta">
                <span>
                  {comment.status ?? comment.commentStatus}
                </span>
                            </div>
                        </div>
                    ))}
                </div>
            )}
        </div>
    );
}
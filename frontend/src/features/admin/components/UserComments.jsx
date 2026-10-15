export default function UserComments({
                                         comments,
                                         loading,
                                     }) {
    return (
        <div className="user-comments">
            <div className="user-comments-header">
                <h3>작성한 댓글</h3>
                <span>{comments.length}개</span>
            </div>

            {loading ? (
                <p className="admin-empty">
                    댓글을 불러오는 중입니다...
                </p>
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
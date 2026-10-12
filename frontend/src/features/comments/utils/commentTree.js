export function getCommentId(comment) {return comment.commentId ?? comment.id;}
export function getParentId(comment) {return comment.parentId ?? comment.parentCommentId ?? null;}
export function getInlineReplies(comment) {return comment.replies ?? comment.children ?? [];}
export function isHiddenComment(comment) {return comment.status === 'HIDDEN';}
export function isDeletedComment(comment) {return comment.status === 'DELETED';}

export function buildCommentTree(list) {
    const repliesByParent = new Map();

    list.forEach((comment) => {
        const parentId = getParentId(comment);

        if (parentId != null) {
            const replies = repliesByParent.get(parentId) || [];
            repliesByParent.set(parentId, [...replies, comment]);
        }
    });

    return list
        .filter((comment) => getParentId(comment) == null)
        .map((comment) => ({
            comment,
            replies: [
                ...getInlineReplies(comment),
                ...(repliesByParent.get(getCommentId(comment)) || []),
            ],
        }));
}
package kwh.PublicCookedFood.notification.service;

public record CommentTargetKey(long boardId, long commentId) {

    public CommentTargetKey {
        if (boardId <= 0) {
            throw new IllegalArgumentException("boardId must be positive");
        }
        if (commentId <= 0) {
            throw new IllegalArgumentException("commentId must be positive");
        }
    }

    public static CommentTargetKey of(long boardId, long commentId) {
        return new CommentTargetKey(boardId, commentId);
    }
}

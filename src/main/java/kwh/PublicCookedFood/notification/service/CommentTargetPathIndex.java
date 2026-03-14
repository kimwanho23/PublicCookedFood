package kwh.PublicCookedFood.notification.service;

import kwh.PublicCookedFood.board.service.comment.CommentTargetPath;

import java.util.Map;
import java.util.Optional;

public record CommentTargetPathIndex(Map<CommentTargetKey, CommentTargetPath> values) {

    private static final CommentTargetPathIndex EMPTY = new CommentTargetPathIndex(Map.of());

    public CommentTargetPathIndex {
        values = values == null || values.isEmpty() ? Map.of() : Map.copyOf(values);
    }

    public static CommentTargetPathIndex empty() {
        return EMPTY;
    }

    public static CommentTargetPathIndex of(Map<CommentTargetKey, CommentTargetPath> values) {
        return new CommentTargetPathIndex(values);
    }

    public Optional<CommentTargetPath> find(long boardId, long commentId) {
        if (values.isEmpty()) {
            return Optional.empty();
        }
        return Optional.ofNullable(values.get(CommentTargetKey.of(boardId, commentId)));
    }
}

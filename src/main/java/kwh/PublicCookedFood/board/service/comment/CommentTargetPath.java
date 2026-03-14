package kwh.PublicCookedFood.board.service.comment;

import java.util.Objects;

public final class CommentTargetPath {

    private final String value;

    public CommentTargetPath(String value) {
        if (value == null || value.trim().isEmpty()) {
            throw new IllegalArgumentException("comment target path must not be blank");
        }
        this.value = value.trim();
    }

    public static CommentTargetPath of(String value) {
        return new CommentTargetPath(value);
    }

    public String value() {
        return value;
    }

    @Override
    public boolean equals(Object other) {
        if (this == other) {
            return true;
        }
        if (!(other instanceof CommentTargetPath)) {
            return false;
        }
        CommentTargetPath that = (CommentTargetPath) other;
        return Objects.equals(value, that.value);
    }

    @Override
    public int hashCode() {
        return Objects.hash(value);
    }
}

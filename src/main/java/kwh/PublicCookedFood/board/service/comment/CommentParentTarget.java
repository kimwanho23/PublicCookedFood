package kwh.PublicCookedFood.board.service.comment;

public interface CommentParentTarget {

    static CommentParentTarget root() {
        return Root.INSTANCE;
    }

    static CommentParentTarget reply(Long parentId) {
        return new Reply(parentId);
    }

    boolean isReply();

    long requireParentId();

    final class Root implements CommentParentTarget {

        private static final Root INSTANCE = new Root();

        private Root() {
        }

        @Override
        public boolean isReply() {
            return false;
        }

        @Override
        public long requireParentId() {
            throw new IllegalStateException("root target does not have a parent id");
        }
    }

    final class Reply implements CommentParentTarget {

        private final long parentId;

        private Reply(Long parentId) {
            if (parentId == null || parentId.longValue() <= 0L) {
                throw new IllegalArgumentException("부모 댓글 ID는 양수여야 합니다.");
            }
            this.parentId = parentId.longValue();
        }

        public long parentId() {
            return parentId;
        }

        @Override
        public boolean isReply() {
            return true;
        }

        @Override
        public long requireParentId() {
            return parentId;
        }
    }
}

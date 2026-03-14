package kwh.PublicCookedFood.account.audit;

public record BoardCommentAuditPayload(Long boardId,
                                       Long commentId,
                                       ParentReference parentReference) {

    public BoardCommentAuditPayload {
        if (boardId == null || boardId <= 0) {
            throw new IllegalArgumentException("boardId must be positive");
        }
        if (commentId == null || commentId <= 0) {
            throw new IllegalArgumentException("commentId must be positive");
        }
        parentReference = parentReference == null ? ParentReference.root() : parentReference;
    }

    public static BoardCommentAuditPayload root(Long boardId, Long commentId) {
        return new BoardCommentAuditPayload(boardId, commentId, ParentReference.root());
    }

    public static BoardCommentAuditPayload reply(Long boardId, Long commentId, Long parentId) {
        return new BoardCommentAuditPayload(boardId, commentId, ParentReference.reply(parentId));
    }

    public String detailText() {
        return "boardId=" + boardId + ",commentId=" + commentId + ",parentId=" + parentReference.auditValue();
    }

    public sealed interface ParentReference permits ParentReference.Root, ParentReference.Reply {
        static Root root() {
            return Root.INSTANCE;
        }

        static Reply reply(Long parentId) {
            return new Reply(parentId);
        }

        String auditValue();

        final class Root implements ParentReference {
            private static final Root INSTANCE = new Root();

            private Root() {
            }

            @Override
            public String auditValue() {
                return "-";
            }
        }

        record Reply(Long parentId) implements ParentReference {
            public Reply {
                if (parentId == null || parentId <= 0) {
                    throw new IllegalArgumentException("parentId must be positive");
                }
            }

            @Override
            public String auditValue() {
                return String.valueOf(parentId);
            }
        }
    }
}

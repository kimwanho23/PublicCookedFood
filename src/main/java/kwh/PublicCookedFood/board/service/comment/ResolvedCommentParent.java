package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;

import java.util.Objects;

public interface ResolvedCommentParent {

    static Root root() {
        return Root.INSTANCE;
    }

    static Reply reply(Comments parent) {
        return new Reply(parent);
    }

    default void applyTo(Comments.CommentsBuilder builder) {
        Objects.requireNonNull(builder, "builder");
        builder.parent(null);
        builder.rootParentId(null);
        builder.depth(0);
    }

    default long initialRootParentId(long savedCommentId) {
        return savedCommentId;
    }

    default String pathPrefix(CommentPathResolver pathResolver) {
        Objects.requireNonNull(pathResolver, "pathResolver");
        return "";
    }

    default void validateParent(long boardId) {
    }

    default void validateReplyActorVisibility(long actorId, AccountBlockService accountBlockService) {
    }

    final class Root implements ResolvedCommentParent {
        private static final Root INSTANCE = new Root();

        private Root() {
        }
    }

    final class Reply implements ResolvedCommentParent {

        private final Comments parent;

        public Reply(Comments parent) {
            this.parent = Objects.requireNonNull(parent, "parent");
        }

        @Override
        public void applyTo(Comments.CommentsBuilder builder) {
            Objects.requireNonNull(builder, "builder");
            builder.parent(parent);
            builder.rootParentId(parent.getEffectiveRootParentId());
            builder.depth(parent.getDepth() + 1);
        }

        @Override
        public long initialRootParentId(long savedCommentId) {
            return parent.getEffectiveRootParentId();
        }

        @Override
        public String pathPrefix(CommentPathResolver pathResolver) {
            Objects.requireNonNull(pathResolver, "pathResolver");
            return pathResolver.resolvePath(parent);
        }

        @Override
        public void validateParent(long boardId) {
            Board parentBoard = parent.getBoard();
            if (parentBoard == null || parentBoard.getId() == null || parentBoard.getId() != boardId) {
                throw new AppException(CommonErrorCode.INVALID_REQUEST, "부모 댓글 정보가 올바르지 않습니다.");
            }
            if (parent.getState() != SoftDeleteState.ACTIVE) {
                throw new AppException(CommonErrorCode.INVALID_REQUEST, "삭제된 댓글에는 답글을 작성할 수 없습니다.");
            }
        }

        @Override
        public void validateReplyActorVisibility(long actorId, AccountBlockService accountBlockService) {
            Objects.requireNonNull(accountBlockService, "accountBlockService");
            if (parent.getAccount() != null
                    && accountBlockService.isEitherBlocked(actorId, parent.getAccount().getId())) {
                throw new AppException(BoardErrorCode.BOARD_COMMENT_BLOCKED);
            }
        }

        public Comments parent() {
            return parent;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof Reply)) {
                return false;
            }
            Reply that = (Reply) other;
            return Objects.equals(parent, that.parent);
        }

        @Override
        public int hashCode() {
            return Objects.hash(parent);
        }
    }
}

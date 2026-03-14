package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentThreadContextUnitTest {

    @Test
    void forCreate_buildsActiveRootComment() {
        Account actor = Account.builder()
                .id(1L)
                .name("writer")
                .build();
        Board board = Board.builder()
                .id(10L)
                .title("title")
                .contents("contents")
                .build();

        CommentThreadContext context = CommentThreadContext.forCreate(
                CommentActorBoardContext.of(actor, board),
                ResolvedCommentParent.root()
        );

        Comments comment = context.newComment("hello");

        assertThat(comment.getAccount()).isEqualTo(actor);
        assertThat(comment.getBoard()).isEqualTo(board);
        assertThat(comment.getContents()).isEqualTo("hello");
        assertThat(comment.getState()).isEqualTo(SoftDeleteState.ACTIVE);
        assertThat(comment.getDepth()).isZero();
    }

    @Test
    void forCreate_rejectsNullActorBoard() {
        assertThatThrownBy(() -> CommentThreadContext.forCreate(null, ResolvedCommentParent.root()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("actorBoard");
    }

    @Test
    void forCreate_rejectsNullParent() {
        Account actor = Account.builder()
                .id(1L)
                .name("writer")
                .build();
        Board board = Board.builder()
                .id(10L)
                .title("title")
                .contents("contents")
                .build();

        assertThatThrownBy(() -> CommentThreadContext.forCreate(CommentActorBoardContext.of(actor, board), null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("parent");
    }
}

package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.board.application.query.view.CommentNodeView;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentTreeAssemblerUnitTest {

    private final CommentTreeAssembler commentTreeAssembler = new CommentTreeAssembler();

    @Test
    void toResponse_throwsWhenCommentIsNull() {
        assertThatThrownBy(() -> commentTreeAssembler.toView(null, Map.of(), 1L))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("comment");
    }

    @Test
    void toResponse_buildsNestedReplyTree() {
        Board board = Board.builder().id(10L).build();
        Comments parent = comment(1L, 100L, "parent", board, null, SoftDeleteState.ACTIVE);
        Comments child = comment(2L, 200L, "child", board, parent, SoftDeleteState.ACTIVE);

        CommentNodeView response = commentTreeAssembler.toView(
                parent,
                Map.of(1L, List.of(child)),
                10L
        );

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getBoardId()).isEqualTo(10L);
        assertThat(response.getReplies()).hasSize(1);
        assertThat(response.getReplies().get(0).getId()).isEqualTo(2L);
        assertThat(response.getReplies().get(0).getParentId()).isEqualTo(1L);
    }

    @Test
    void toResponse_computesAllRepliesDeletedWithoutPostProcessing() {
        Board board = Board.builder().id(10L).build();
        Comments parent = comment(1L, 100L, "deleted-parent", board, null, SoftDeleteState.DELETED);
        Comments child = comment(2L, 200L, "deleted-child", board, parent, SoftDeleteState.DELETED);

        CommentNodeView response = commentTreeAssembler.toView(
                parent,
                Map.of(1L, List.of(child)),
                10L
        );

        assertThat(response.isDeleted()).isTrue();
        assertThat(response.areAllRepliesDeleted()).isTrue();
    }

    private Comments comment(Long id,
                             Long accountId,
                             String contents,
                             Board board,
                             Comments parent,
                             SoftDeleteState state) {
        Account account = Account.builder()
                .id(accountId)
                .name("writer-" + accountId)
                .build();
        return Comments.builder()
                .id(id)
                .account(account)
                .board(board)
                .contents(contents)
                .parent(parent)
                .state(state)
                .build();
    }
}

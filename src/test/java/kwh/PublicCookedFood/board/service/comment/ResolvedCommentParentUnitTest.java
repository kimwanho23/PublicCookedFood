package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ResolvedCommentParentUnitTest {

    @Test
    void reply_applyTo_setsParentRootAndDepth() {
        Comments parentComment = parentComment(10L, 3, 10L, SoftDeleteState.ACTIVE, 20L);
        ResolvedCommentParent parent = ResolvedCommentParent.reply(parentComment);

        Comments child = Comments.builder()
                .contents("reply")
                .state(SoftDeleteState.ACTIVE)
                .build();
        Comments.CommentsBuilder builder = Comments.builder()
                .contents("reply")
                .state(SoftDeleteState.ACTIVE);
        parent.applyTo(builder);
        child = builder.build();

        assertThat(child.getParent()).isEqualTo(parentComment);
        assertThat(child.getRootParentId()).isEqualTo(10L);
        assertThat(child.getDepth()).isEqualTo(4);
        assertThat(parent.initialRootParentId(99L)).isEqualTo(10L);
    }

    @Test
    void reply_validateParent_rejectsDeletedParent() {
        Comments deletedParent = parentComment(10L, 0, null, SoftDeleteState.DELETED, 20L);

        assertThatThrownBy(() -> ResolvedCommentParent.reply(deletedParent).validateParent(1L))
                .isInstanceOf(AppException.class)
                .hasMessageContaining("삭제된 댓글에는 답글");
    }

    @Test
    void reply_validateReplyActorVisibility_checksBlockedParentAuthor() {
        AccountBlockService accountBlockService = Mockito.mock(AccountBlockService.class);
        Comments parentComment = parentComment(10L, 0, null, SoftDeleteState.ACTIVE, 20L);
        when(accountBlockService.isEitherBlocked(1L, 20L)).thenReturn(true);

        assertThatThrownBy(() -> ResolvedCommentParent.reply(parentComment)
                .validateReplyActorVisibility(1L, accountBlockService))
                .isInstanceOf(AppException.class);
        verify(accountBlockService).isEitherBlocked(1L, 20L);
    }

    private Comments parentComment(Long id,
                                   int depth,
                                   Long rootParentId,
                                   SoftDeleteState state,
                                   Long accountId) {
        Account account = Account.builder()
                .id(accountId)
                .name("writer")
                .build();
        Board board = Board.builder()
                .id(1L)
                .title("title")
                .contents("contents")
                .build();
        return Comments.builder()
                .id(id)
                .account(account)
                .board(board)
                .contents("parent")
                .rootParentId(rootParentId)
                .depth(depth)
                .state(state)
                .build();
    }
}

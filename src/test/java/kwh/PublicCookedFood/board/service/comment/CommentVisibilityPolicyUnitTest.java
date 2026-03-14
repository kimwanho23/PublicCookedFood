package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class CommentVisibilityPolicyUnitTest {

    private final CommentVisibilityPolicy policy = new CommentVisibilityPolicy();

    @Test
    void shouldDisplay_deletedCommentWhenActiveDescendantExists() {
        Comments parent = comment(1L, SoftDeleteState.DELETED, null, 10L);
        Comments child = comment(2L, SoftDeleteState.ACTIVE, parent, 11L);

        boolean visible = policy.shouldDisplay(parent, Map.of(1L, List.of(child)));

        assertThat(visible).isTrue();
    }

    @Test
    void shouldDisplay_deletedLeafCommentAsHidden() {
        Comments parent = comment(1L, SoftDeleteState.DELETED, null, 10L);

        boolean visible = policy.shouldDisplay(parent, Map.of());

        assertThat(visible).isFalse();
    }

    @Test
    void isReachable_requiresVisibleAncestorChain() {
        Comments root = comment(1L, SoftDeleteState.ACTIVE, null, 10L);
        Comments reply = comment(2L, SoftDeleteState.ACTIVE, root, 11L);
        Comments nestedReply = comment(3L, SoftDeleteState.ACTIVE, reply, 12L);

        boolean reachable = policy.isReachable(
                nestedReply,
                Map.of(
                        1L, List.of(reply),
                        2L, List.of(nestedReply)
                )
        );

        assertThat(reachable).isTrue();
    }

    @Test
    void isBlockedAuthor_checksAuthorIdMembership() {
        Comments comment = comment(1L, SoftDeleteState.ACTIVE, null, 15L);

        assertThat(policy.isBlockedAuthor(comment, Set.of(15L))).isTrue();
        assertThat(policy.isBlockedAuthor(comment, Set.of(99L))).isFalse();
    }

    private Comments comment(Long id, SoftDeleteState state, Comments parent, Long accountId) {
        Account account = Account.builder()
                .id(accountId)
                .name("tester")
                .build();
        return Comments.builder()
                .id(id)
                .account(account)
                .contents("contents")
                .parent(parent)
                .state(state)
                .depth(parent == null ? 0 : parent.getDepth() + 1)
                .build();
    }
}

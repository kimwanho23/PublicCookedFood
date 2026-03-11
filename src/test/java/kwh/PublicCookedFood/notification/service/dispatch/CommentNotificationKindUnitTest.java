package kwh.PublicCookedFood.notification.service.dispatch;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class CommentNotificationKindUnitTest {

    @Test
    void higherThan_usesExplicitPriority() {
        assertThat(CommentNotificationKind.REPLY.higherThan(CommentNotificationKind.BOARD_COMMENT)).isTrue();
        assertThat(CommentNotificationKind.BOARD_COMMENT.higherThan(CommentNotificationKind.MENTION)).isTrue();
        assertThat(CommentNotificationKind.MENTION.higherThan(CommentNotificationKind.REPLY)).isFalse();
    }
}

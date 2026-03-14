package kwh.PublicCookedFood.board.service.comment;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class CommentCreateCommandUnitTest {

    @Test
    void from_resolvesRootTargetWhenParentIdMissing() {
        CommentCreateCommand command = CommentCreateCommand.of(1L, 10L, "hello", null);

        assertThat(command.target()).isInstanceOf(CommentParentTarget.Root.class);
        assertThat(command.hasParent()).isFalse();
    }

    @Test
    void from_resolvesReplyTargetWhenParentIdPresent() {
        CommentCreateCommand command = CommentCreateCommand.of(1L, 10L, "hello", 99L);

        assertThat(command.target()).isInstanceOf(CommentParentTarget.Reply.class);
        assertThat(command.hasParent()).isTrue();
        assertThat(command.requiredParentId()).isEqualTo(99L);
        assertThat(((CommentParentTarget.Reply) command.target()).parentId()).isEqualTo(99L);
    }

    @Test
    void from_rejectsBlankContents() {
        assertThatThrownBy(() -> CommentCreateCommand.of(1L, 10L, "   ", null))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("댓글 내용은 필수입니다.");
    }
}

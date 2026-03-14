package kwh.PublicCookedFood.board.service.command;

import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardPolicyUpdateCommandUnitTest {

    @Test
    void fromForm_createsFullUpdateWhenBothValuesExist() {
        BoardPolicyUpdateCommand command = BoardPolicyUpdateCommand.fromForm(5, BoardThumbnailDisplayMode.HOVER, 10L);

        assertThat(command).isInstanceOf(BoardPolicyUpdateCommand.FullUpdate.class);
    }

    @Test
    void fromForm_createsThresholdUpdateWhenOnlyThresholdExists() {
        BoardPolicyUpdateCommand command = BoardPolicyUpdateCommand.fromForm(5, null, 10L);

        assertThat(command).isInstanceOf(BoardPolicyUpdateCommand.FeaturedThresholdUpdate.class);
    }

    @Test
    void fromForm_createsThumbnailModeUpdateWhenOnlyThumbnailModeExists() {
        BoardPolicyUpdateCommand command = BoardPolicyUpdateCommand.fromForm(null, BoardThumbnailDisplayMode.LEFT, 10L);

        assertThat(command).isInstanceOf(BoardPolicyUpdateCommand.ThumbnailDisplayModeUpdate.class);
    }

    @Test
    void fromForm_rejectsEmptyChangeSet() {
        assertThatThrownBy(() -> BoardPolicyUpdateCommand.fromForm(null, null, 10L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("변경할 게시판 정책이 없습니다.");
    }
}

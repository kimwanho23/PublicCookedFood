package kwh.PublicCookedFood.board.service.command;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardSaveCommandFactoryUnitTest {

    @Test
    void forCreate_buildsCreateCommandWithDefaultMetrics() {
        BoardSaveCommand command = BoardSaveCommand.forCreate("제목", "내용", 33L);

        assertThat(command.title()).isEqualTo("제목");
        assertThat(command.contents()).isEqualTo("내용");
        assertThat(command.sectionId()).isEqualTo(33L);
    }

    @Test
    void updateCommand_fromBuildsCommandWithoutExistingReadDto() {
        BoardUpdateCommand command = BoardUpdateCommand.of(5L, 10L, 9L, "수정 제목", "수정 내용", 44L);

        assertThat(command.boardId()).isEqualTo(10L);
        assertThat(command.actorAccountId()).isEqualTo(5L);
        assertThat(command.version()).isEqualTo(9L);
        assertThat(command.title()).isEqualTo("수정 제목");
        assertThat(command.contents()).isEqualTo("수정 내용");
        assertThat(command.sectionId()).isEqualTo(44L);
    }
}

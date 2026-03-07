package kwh.PublicCookedFood.board.dto.request;

import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardSaveRequestFactoryUnitTest {

    @Test
    void forCreate_buildsCreateCommandWithDefaultMetrics() {
        BoardWriteRequest writeRequest = new BoardWriteRequest();
        writeRequest.setTitle("제목");
        writeRequest.setContents("내용");
        writeRequest.setSectionId(33L);

        BoardSaveRequest command = BoardSaveRequest.forCreate(writeRequest, 7L);

        assertThat(command.getId()).isNull();
        assertThat(command.getTitle()).isEqualTo("제목");
        assertThat(command.getContents()).isEqualTo("내용");
        assertThat(command.getAccountId()).isEqualTo(7L);
        assertThat(command.getSectionId()).isEqualTo(33L);
        assertThat(command.getViews()).isEqualTo(0L);
        assertThat(command.getLikesCount()).isEqualTo(0L);
        assertThat(command.getCommentsCount()).isEqualTo(0L);
        assertThat(command.getState()).isEqualTo(SoftDeleteState.ACTIVE);
    }

    @Test
    void forUpdate_buildsUpdateCommandFromExistingBoardAndUpdateRequest() {
        BoardUpdateRequest updateRequest = BoardUpdateRequest.builder()
                .title("수정 제목")
                .contents("수정 내용")
                .sectionId(44L)
                .build();

        BoardDetailResponse existingBoard = BoardDetailResponse.builder()
                .id(10L)
                .accountId(5L)
                .views(111L)
                .likesCount(12L)
                .commentsCount(3L)
                .state(SoftDeleteState.ACTIVE)
                .build();

        BoardSaveRequest command = BoardSaveRequest.forUpdate(updateRequest, existingBoard);

        assertThat(command.getId()).isEqualTo(10L);
        assertThat(command.getTitle()).isEqualTo("수정 제목");
        assertThat(command.getContents()).isEqualTo("수정 내용");
        assertThat(command.getAccountId()).isEqualTo(5L);
        assertThat(command.getSectionId()).isEqualTo(44L);
        assertThat(command.getViews()).isEqualTo(111L);
        assertThat(command.getLikesCount()).isEqualTo(12L);
        assertThat(command.getCommentsCount()).isEqualTo(3L);
        assertThat(command.getState()).isEqualTo(SoftDeleteState.ACTIVE);
    }
}

package kwh.PublicCookedFood.board.dto.response;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.user.domain.Role;
import kwh.PublicCookedFood.user.domain.Users;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardDetailResponseFactoryUnitTest {

    @Test
    void from_mapsBoardFields() {
        Users user = Users.builder()
                .id(7L)
                .name("작성자")
                .email("writer@test.com")
                .authority(Role.USER)
                .loginMethod("Current")
                .profileImageUrl("https://example.com/profile.png")
                .build();

        BoardSection section = BoardSection.builder()
                .id(3L)
                .sectionKey("free")
                .sectionName("자유게시판")
                .build();

        Board board = Board.builder()
                .id(11L)
                .title("제목")
                .contents("내용")
                .user(user)
                .section(section)
                .views(22L)
                .likeCount(33L)
                .commentCount(44L)
                .state(SoftDeleteState.ACTIVE)
                .build();

        BoardDetailResponse response = BoardDetailResponse.from(board);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getTitle()).isEqualTo("제목");
        assertThat(response.getContents()).isEqualTo("내용");
        assertThat(response.getUserId()).isEqualTo(7L);
        assertThat(response.getUserName()).isEqualTo("작성자");
        assertThat(response.getUserProfileImageUrl()).isEqualTo("https://example.com/profile.png");
        assertThat(response.getSectionId()).isEqualTo(3L);
        assertThat(response.getSectionKey()).isEqualTo("free");
        assertThat(response.getSectionName()).isEqualTo("자유게시판");
        assertThat(response.getViews()).isEqualTo(22L);
        assertThat(response.getLikesCount()).isEqualTo(33L);
        assertThat(response.getCommentsCount()).isEqualTo(44L);
        assertThat(response.getState()).isEqualTo(SoftDeleteState.ACTIVE);
    }
}

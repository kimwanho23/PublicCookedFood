package kwh.PublicCookedFood.board.dto.response;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardDetailResponseFactoryUnitTest {

    @Test
    void from_mapsBoardFields() {
        Account account = Account.builder()
                .id(7L)
                .name("writer")
                .email("writer@test.com")
                .authority(Role.USER)
                .loginMethod("Current")
                .profileImageUrl("https://example.com/profile.png")
                .build();

        BoardSection section = BoardSection.builder()
                .id(3L)
                .sectionKey("free")
                .sectionName("free board")
                .build();

        Board board = Board.builder()
                .id(11L)
                .title("title")
                .contents("contents")
                .account(account)
                .section(section)
                .version(2L)
                .state(SoftDeleteState.ACTIVE)
                .build();

        BoardDetailResponse response = BoardDetailResponse.from(board);

        assertThat(response.getId()).isEqualTo(11L);
        assertThat(response.getTitle()).isEqualTo("title");
        assertThat(response.getContents()).isEqualTo("contents");
        assertThat(response.getAccountId()).isEqualTo(7L);
        assertThat(response.getAccountName()).isEqualTo("writer");
        assertThat(response.getAccountProfileImageUrl()).isEqualTo("https://example.com/profile.png");
        assertThat(response.getSectionId()).isEqualTo(3L);
        assertThat(response.getSectionKey()).isEqualTo("free");
        assertThat(response.getSectionName()).isEqualTo("free board");
        assertThat(response.getViews()).isNull();
        assertThat(response.getVersion()).isEqualTo(2L);
        assertThat(response.getState()).isEqualTo(SoftDeleteState.ACTIVE);
    }
}

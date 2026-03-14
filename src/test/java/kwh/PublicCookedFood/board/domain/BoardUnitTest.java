package kwh.PublicCookedFood.board.domain;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class BoardUnitTest {

    @Test
    void create_requiresNonBlankTitleAndContents() {
        assertThatThrownBy(() -> Board.create("   ", "<p>contents</p>", account(), section()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시글 제목이 비어 있습니다.");

        assertThatThrownBy(() -> Board.create("title", "   ", account(), section()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시글 내용이 비어 있습니다.");
    }

    @Test
    void edit_updatesBoardWhenInputsAreValid() {
        Board board = Board.create("before", "<p>before</p>", account(), section());
        BoardSection updatedSection = BoardSection.builder()
                .id(2L)
                .sectionKey("tips")
                .sectionName("팁")
                .displayOrder(1)
                .active(true)
                .build();

        board.edit("after", "<p>after</p>", updatedSection);

        assertThat(board.getTitle()).isEqualTo("after");
        assertThat(board.getContents()).isEqualTo("<p>after</p>");
        assertThat(board.getSection()).isSameAs(updatedSection);
    }

    @Test
    void edit_rejectsBlankContents() {
        Board board = Board.create("title", "<p>contents</p>", account(), section());

        assertThatThrownBy(() -> board.edit("title", "   ", section()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("게시글 내용이 비어 있습니다.");
    }

    private Account account() {
        return Account.builder()
                .id(7L)
                .email("writer@test.com")
                .name("writer")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }

    private BoardSection section() {
        return BoardSection.builder()
                .id(1L)
                .sectionKey("general")
                .sectionName("일반")
                .displayOrder(0)
                .active(true)
                .build();
    }
}

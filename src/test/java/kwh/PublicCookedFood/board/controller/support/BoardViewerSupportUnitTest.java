package kwh.PublicCookedFood.board.controller.support;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BoardViewerSupportUnitTest {

    private final BoardViewerSupport boardViewerSupport = new BoardViewerSupport();

    @Test
    void requiresLogin_returnsTrueForAnonymousViewer() {
        assertThat(boardViewerSupport.requiresLogin(BoardViewer.anonymous())).isTrue();
        assertThat(boardViewerSupport.loginRedirect()).isEqualTo("redirect:/u/login");
    }

    @Test
    void viewer_returnsAuthenticatedViewerWhenAccountIdExists() {
        Account account = Account.builder()
                .id(10L)
                .email("viewer@test.com")
                .name("viewer")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();

        BoardViewer viewer = BoardViewer.from(account);

        assertThat(viewer.isAuthenticated()).isTrue();
        assertThat(viewer.maybeAccountId()).contains(10L);
    }
}

package kwh.PublicCookedFood.board.policy;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardAuthorizationPolicyUnitTest {

    @Mock
    private AccountBlockService accountBlockService;

    @InjectMocks
    private BoardAuthorizationPolicy boardAuthorizationPolicy;

    @Test
    void canManageBoard_returnsTrueWhenAccountOwnsBoard() {
        boolean allowed = boardAuthorizationPolicy.canManageBoard(10L, 10L);

        assertThat(allowed).isTrue();
    }

    @Test
    void canManageBoard_returnsFalseWhenAccountDoesNotOwnBoard() {
        boolean allowed = boardAuthorizationPolicy.canManageBoard(10L, 20L);

        assertThat(allowed).isFalse();
    }

    @Test
    void canManageBoard_returnsFalseForAnonymousViewer() {
        boolean allowed = boardAuthorizationPolicy.canManageBoard(BoardViewer.anonymous(), 10L);

        assertThat(allowed).isFalse();
    }

    @Test
    void canManageComment_returnsTrueWhenAccountOwnsCommentAndBoardMatches() {
        Comments comment = Comments.builder()
                .account(createAccount(1L))
                .board(Board.builder().id(99L).build())
                .contents("body")
                .build();

        boolean allowed = boardAuthorizationPolicy.canManageComment(1L, 99L, comment);

        assertThat(allowed).isTrue();
    }

    @Test
    void canManageComment_returnsFalseWhenBoardIdDoesNotMatch() {
        Comments comment = Comments.builder()
                .account(createAccount(1L))
                .board(Board.builder().id(55L).build())
                .contents("body")
                .build();

        boolean allowed = boardAuthorizationPolicy.canManageComment(1L, 99L, comment);

        assertThat(allowed).isFalse();
    }

    @Test
    void isViewRestricted_delegatesToAccountBlockService() {
        when(accountBlockService.isEitherBlocked(2L, 3L)).thenReturn(true);

        boolean restricted = boardAuthorizationPolicy.isViewRestricted(BoardViewer.authenticated(2L), 3L);

        assertThat(restricted).isTrue();
        verify(accountBlockService).isEitherBlocked(2L, 3L);
    }

    @Test
    void isAuthorBlockedByViewer_returnsFalseWhenSameAccount() {
        boolean blocked = boardAuthorizationPolicy.isAuthorBlockedByViewer(BoardViewer.authenticated(5L), 5L);

        assertThat(blocked).isFalse();
    }

    private Account createAccount(Long id) {
        return Account.builder()
                .id(id)
                .email("account-" + id + "@test.com")
                .name("author")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}

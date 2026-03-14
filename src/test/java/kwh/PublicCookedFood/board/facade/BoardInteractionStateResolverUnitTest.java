package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.board.policy.BoardAuthorizationPolicy;
import kwh.PublicCookedFood.board.service.BoardScrapService;
import kwh.PublicCookedFood.board.service.LikeService;
import kwh.PublicCookedFood.board.service.query.BoardReportQueryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardInteractionStateResolverUnitTest {

    @Mock
    private LikeService likeService;

    @Mock
    private BoardScrapService boardScrapService;

    @Mock
    private BoardReportQueryService boardReportQueryService;

    @Mock
    private BoardAuthorizationPolicy boardAuthorizationPolicy;

    private BoardInteractionStateResolver boardInteractionStateResolver;

    @BeforeEach
    void setUp() {
        boardInteractionStateResolver = new BoardInteractionStateResolver(
                likeService,
                boardScrapService,
                boardReportQueryService,
                boardAuthorizationPolicy
        );
    }

    @Test
    void resolve_returnsAnonymousStateWhenViewerMissing() {
        BoardInteractionState state = boardInteractionStateResolver.resolve(10L, BoardViewer.anonymous(), 1L);

        assertThat(state.maybeCurrentAccountId()).isEmpty();
        assertThat(state.authenticated()).isFalse();
        assertThat(state.myLike()).isFalse();
        assertThat(state.myScrap()).isFalse();
    }

    @Test
    void resolve_buildsAuthenticatedState() {
        Account account = Account.builder()
                .id(99L)
                .email("viewer@test.com")
                .name("viewer")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();

        when(likeService.hasLike(10L, 99L)).thenReturn(true);
        when(boardScrapService.isScrapped(10L, 99L)).thenReturn(true);
        when(boardReportQueryService.hasReported(10L, 99L)).thenReturn(false);
        when(boardAuthorizationPolicy.isAuthorBlockedByViewer(org.mockito.ArgumentMatchers.eq(BoardViewer.authenticated(99L)), org.mockito.ArgumentMatchers.eq(1L)))
                .thenReturn(true);

        BoardInteractionState state = boardInteractionStateResolver.resolve(10L, BoardViewer.from(account), 1L);

        assertThat(state.maybeCurrentAccountId()).contains(99L);
        assertThat(state.authenticated()).isTrue();
        assertThat(state.myLike()).isTrue();
        assertThat(state.myScrap()).isTrue();
        assertThat(state.myReport()).isFalse();
        assertThat(state.myBlockedAuthor()).isTrue();
        assertThat(state.boardInteractionBlocked()).isTrue();
    }
}

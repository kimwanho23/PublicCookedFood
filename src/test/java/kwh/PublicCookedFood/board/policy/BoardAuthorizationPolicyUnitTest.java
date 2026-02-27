package kwh.PublicCookedFood.board.policy;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.dto.response.BoardDetailResponse;
import kwh.PublicCookedFood.user.domain.Role;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.service.UserBlockService;
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
    private UserBlockService userBlockService;

    @InjectMocks
    private BoardAuthorizationPolicy boardAuthorizationPolicy;

    @Test
    void canManageBoard_returnsTrueWhenUserOwnsBoard() {
        Users owner = createUser(10L);
        BoardDetailResponse board = BoardDetailResponse.builder().userId(10L).build();

        boolean allowed = boardAuthorizationPolicy.canManageBoard(owner, board);

        assertThat(allowed).isTrue();
    }

    @Test
    void canManageBoard_returnsFalseWhenUserDoesNotOwnBoard() {
        Users actor = createUser(10L);
        BoardDetailResponse board = BoardDetailResponse.builder().userId(20L).build();

        boolean allowed = boardAuthorizationPolicy.canManageBoard(actor, board);

        assertThat(allowed).isFalse();
    }

    @Test
    void canManageComment_returnsTrueWhenUserOwnsCommentAndBoardMatches() {
        Users actor = createUser(1L);
        Comments comment = Comments.builder()
                .user(createUser(1L))
                .board(Board.builder().id(99L).build())
                .contents("댓글")
                .build();

        boolean allowed = boardAuthorizationPolicy.canManageComment(actor, 99L, comment);

        assertThat(allowed).isTrue();
    }

    @Test
    void canManageComment_returnsFalseWhenBoardIdDoesNotMatch() {
        Users actor = createUser(1L);
        Comments comment = Comments.builder()
                .user(createUser(1L))
                .board(Board.builder().id(55L).build())
                .contents("댓글")
                .build();

        boolean allowed = boardAuthorizationPolicy.canManageComment(actor, 99L, comment);

        assertThat(allowed).isFalse();
    }

    @Test
    void isViewRestricted_delegatesToUserBlockService() {
        Users viewer = createUser(2L);
        when(userBlockService.isEitherBlocked(2L, 3L)).thenReturn(true);

        boolean restricted = boardAuthorizationPolicy.isViewRestricted(viewer, 3L);

        assertThat(restricted).isTrue();
        verify(userBlockService).isEitherBlocked(2L, 3L);
    }

    @Test
    void isAuthorBlockedByViewer_returnsFalseWhenSameUser() {
        boolean blocked = boardAuthorizationPolicy.isAuthorBlockedByViewer(5L, 5L);

        assertThat(blocked).isFalse();
    }

    private Users createUser(Long id) {
        return Users.builder()
                .id(id)
                .email("user-" + id + "@test.com")
                .name("테스터")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}

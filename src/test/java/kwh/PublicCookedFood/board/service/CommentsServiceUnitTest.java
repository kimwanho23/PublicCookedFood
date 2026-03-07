package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.dto.request.CommentCreateRequest;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentsServiceUnitTest {

    @Mock
    private CommentsRepository commentsRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private AccountBlockService accountBlockService;

    @InjectMocks
    private CommentsService commentsService;

    @Test
    void createComment_throwsWhenBoardIsNotVisible() {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setBoardId(10L);
        request.setAccountId(1L);
        request.setContents("hello");

        when(accountRepository.findById(1L)).thenReturn(Optional.of(loginAccount(1L)));
        when(boardRepository.findByIdWithAccountAndState(10L, SoftDeleteState.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentsService.createComment(request))
                .isInstanceOf(IllegalArgumentException.class);

        verify(commentsRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(notificationService, never()).notifyOnNewComment(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void createComment_throwsAppExceptionWhenBoardAuthorIsBlocked() {
        CommentCreateRequest request = new CommentCreateRequest();
        request.setBoardId(10L);
        request.setAccountId(1L);
        request.setContents("hello");

        Account loginAccount = loginAccount(1L);
        Account boardAuthor = loginAccount(2L);
        Board board = Board.builder()
                .id(10L)
                .account(boardAuthor)
                .state(SoftDeleteState.ACTIVE)
                .build();

        when(accountRepository.findById(1L)).thenReturn(Optional.of(loginAccount));
        when(boardRepository.findByIdWithAccountAndState(10L, SoftDeleteState.ACTIVE)).thenReturn(Optional.of(board));
        when(accountBlockService.isEitherBlocked(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> commentsService.createComment(request))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoardErrorCode.BOARD_COMMENT_BLOCKED));

        verify(commentsRepository, never()).save(org.mockito.ArgumentMatchers.any());
        verify(notificationService, never()).notifyOnNewComment(org.mockito.ArgumentMatchers.any());
    }

    private Account loginAccount(Long accountId) {
        return Account.builder()
                .id(accountId)
                .email("account@test.com")
                .name("tester")
                .authority(Role.USER)
                .loginMethod("Current")
                .build();
    }
}

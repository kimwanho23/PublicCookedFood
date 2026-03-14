package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.error.BoardErrorCode;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.LikesRepository;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.account.service.AccountBlockService;
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
class LikeServiceUnitTest {

    @Mock
    private LikesRepository likesRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private AccountBlockService accountBlockService;

    @InjectMocks
    private LikeService likeService;

    @Test
    void saveLikes_throwsWhenBoardIsNotVisible() {
        when(likesRepository.existsByBoardIdAndAccountId(10L, 1L)).thenReturn(false);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(loginAccount(1L)));
        when(boardRepository.findByIdWithAccountAndState(10L, SoftDeleteState.ACTIVE)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> likeService.saveLikes(10L, 1L))
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
                    assertThat(e).hasMessageContaining("유효하지 않은 게시글");
                });

        verify(likesRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void saveLikes_returnsAlreadyExistsResultWhenLikeAlreadyPresent() {
        when(likesRepository.existsByBoardIdAndAccountId(10L, 1L)).thenReturn(true);

        LikeSaveResult result = likeService.saveLikes(10L, 1L);

        assertThat(result).isInstanceOf(LikeSaveResult.AlreadyExists.class);
        assertThat(result.created()).isFalse();
        assertThat(result.alreadyExisted()).isTrue();
        assertThat(result.likeId()).isEmpty();
        verify(likesRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
    }

    @Test
    void saveLikes_throwsAppExceptionWhenBoardAuthorIsBlocked() {
        Account loginAccount = loginAccount(1L);
        Account boardAuthor = loginAccount(2L);
        Board board = Board.builder()
                .id(10L)
                .account(boardAuthor)
                .state(SoftDeleteState.ACTIVE)
                .build();

        when(likesRepository.existsByBoardIdAndAccountId(10L, 1L)).thenReturn(false);
        when(accountRepository.findById(1L)).thenReturn(Optional.of(loginAccount));
        when(boardRepository.findByIdWithAccountAndState(10L, SoftDeleteState.ACTIVE)).thenReturn(Optional.of(board));
        when(accountBlockService.isEitherBlocked(1L, 2L)).thenReturn(true);

        assertThatThrownBy(() -> likeService.saveLikes(10L, 1L))
                .isInstanceOfSatisfying(AppException.class, e ->
                        assertThat(e.getErrorCode()).isEqualTo(BoardErrorCode.BOARD_LIKE_BLOCKED));

        verify(likesRepository, never()).saveAndFlush(org.mockito.ArgumentMatchers.any());
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


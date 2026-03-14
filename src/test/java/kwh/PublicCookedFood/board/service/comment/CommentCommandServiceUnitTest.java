package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import kwh.PublicCookedFood.notification.service.NotificationService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CommentCommandServiceUnitTest {

    @Mock
    private CommentsRepository commentsRepository;

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private AccountRepository accountRepository;

    @Mock
    private NotificationService notificationService;

    @Mock
    private CommentCreatePolicy commentCreatePolicy;

    @Mock
    private CommentPathResolver commentPathResolver;

    @InjectMocks
    private CommentCommandService commentCommandService;

    @Test
    void createComment_throwsAppExceptionWhenParentCommentDoesNotExist() {
        CommentCreateCommand command = CommentCreateCommand.of(1L, 10L, "reply", 99L);
        org.mockito.Mockito.when(commentsRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentCommandService.createComment(command))
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
                    assertThat(e).hasMessageContaining("부모 댓글을 찾을 수 없습니다.");
                });
    }
}

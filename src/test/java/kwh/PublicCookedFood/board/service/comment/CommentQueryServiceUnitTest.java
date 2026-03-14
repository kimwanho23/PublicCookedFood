package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@ExtendWith(MockitoExtension.class)
class CommentQueryServiceUnitTest {

    @Mock
    private CommentsRepository commentsRepository;

    @Mock
    private AccountBlockService accountBlockService;

    @Mock
    private CommentReplyLoader commentReplyLoader;

    @Mock
    private CommentTreeAssembler commentTreeAssembler;

    @Mock
    private VisibleRootCommentScanner visibleRootCommentScanner;

    @InjectMocks
    private CommentQueryService commentQueryService;

    @Test
    void getComment_throwsAppExceptionWhenCommentDoesNotExist() {
        org.mockito.Mockito.when(commentsRepository.findById(10L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> commentQueryService.getComment(10L))
                .isInstanceOfSatisfying(AppException.class, e -> {
                    assertThat(e.getErrorCode()).isEqualTo(CommonErrorCode.RESOURCE_NOT_FOUND);
                    assertThat(e).hasMessageContaining("댓글을 찾을 수 없습니다.");
                });
    }
}

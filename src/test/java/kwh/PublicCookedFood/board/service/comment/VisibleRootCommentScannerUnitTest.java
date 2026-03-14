package kwh.PublicCookedFood.board.service.comment;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.service.support.BoardVisibilityCriteria;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class VisibleRootCommentScannerUnitTest {

    @Mock
    private kwh.PublicCookedFood.board.repository.CommentsRepository commentsRepository;

    @Mock
    private CommentReplyLoader commentReplyLoader;

    @Mock
    private CommentVisibilityPolicy commentVisibilityPolicy;

    private VisibleRootCommentScanner visibleRootCommentScanner;

    @BeforeEach
    void setUp() {
        visibleRootCommentScanner = new VisibleRootCommentScanner(
                commentsRepository,
                commentReplyLoader,
                commentVisibilityPolicy
        );
    }

    @Test
    void scan_selectsPagedVisibleRootsAndCountsTotalVisibleRoots() {
        Comments first = rootComment(10L);
        Comments hidden = rootComment(11L);
        Comments second = rootComment(12L);

        when(commentsRepository.findParentCommentsPageWithAccountByBoardIdOrderByRegTimeAsc(
                eq(1L),
                eq(SoftDeleteState.ACTIVE),
                eq(SoftDeleteState.DELETED),
                eq(false),
                any(),
                eq(PageRequest.of(0, 100))
        )).thenReturn(new PageImpl<>(List.of(first, hidden, second), PageRequest.of(0, 100), 3));
        when(commentReplyLoader.loadRepliesByRootParentIds(eq(1L), eq(List.of(first, hidden, second)), any()))
                .thenReturn(Map.of());
        when(commentVisibilityPolicy.shouldDisplay(first, Map.of())).thenReturn(true);
        when(commentVisibilityPolicy.shouldDisplay(hidden, Map.of())).thenReturn(false);
        when(commentVisibilityPolicy.shouldDisplay(second, Map.of())).thenReturn(true);

        VisibleRootCommentScanResult result = visibleRootCommentScanner.scan(
                VisibleRootCommentScanQuery.page(1L, BoardVisibilityCriteria.of(Set.of()), 1L, 2L)
        );

        assertThat(result.selectedVisibleRootComments()).containsExactly(second);
        assertThat(result.totalVisibleRootCount()).isEqualTo(2L);
    }

    @Test
    void scan_stopsEarlyWhenTrackedRootsAreResolvedAndFullCountIsNotRequired() {
        Comments tracked = rootComment(10L);

        when(commentsRepository.findParentCommentsPageWithAccountByBoardIdOrderByRegTimeAsc(
                eq(1L),
                eq(SoftDeleteState.ACTIVE),
                eq(SoftDeleteState.DELETED),
                eq(false),
                any(),
                eq(PageRequest.of(0, 100))
        )).thenReturn(new PageImpl<>(List.of(tracked), PageRequest.of(0, 100), 200));
        when(commentReplyLoader.loadRepliesByRootParentIds(eq(1L), eq(List.of(tracked)), any()))
                .thenReturn(Map.of());
        when(commentVisibilityPolicy.shouldDisplay(tracked, Map.of())).thenReturn(true);

        VisibleRootCommentScanResult result = visibleRootCommentScanner.scan(
                VisibleRootCommentScanQuery.track(1L, BoardVisibilityCriteria.of(Set.of()), Set.of(10L))
        );

        assertThat(result.trackedVisibleParentIndexById()).containsEntry(10L, 0);
        assertThat(result.trackedVisibleRootCommentsById()).containsEntry(10L, tracked);
        verify(commentsRepository, times(1))
                .findParentCommentsPageWithAccountByBoardIdOrderByRegTimeAsc(
                        eq(1L),
                        eq(SoftDeleteState.ACTIVE),
                        eq(SoftDeleteState.DELETED),
                        eq(false),
                        any(),
                        any()
                );
    }

    private Comments rootComment(Long id) {
        return Comments.builder()
                .id(id)
                .board(Board.builder().id(1L).title("title").contents("contents").build())
                .account(Account.builder().id(2L).name("user").build())
                .contents("root-" + id)
                .state(SoftDeleteState.ACTIVE)
                .depth(0)
                .build();
    }
}

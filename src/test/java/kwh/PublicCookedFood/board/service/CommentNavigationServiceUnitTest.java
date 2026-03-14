package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.Comments;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.board.service.comment.CommentNavigationTargetResolver;
import kwh.PublicCookedFood.board.service.comment.CommentReplyLoader;
import kwh.PublicCookedFood.board.service.comment.CommentTargetPath;
import kwh.PublicCookedFood.board.service.comment.CommentTargetPathsQuery;
import kwh.PublicCookedFood.board.service.comment.CommentVisibilityPolicy;
import kwh.PublicCookedFood.board.service.comment.VisibleRootCommentIndexResolver;
import kwh.PublicCookedFood.board.service.comment.VisibleRootCommentScanner;
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
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class CommentNavigationServiceUnitTest {

    @Mock
    private CommentsRepository commentsRepository;

    @Mock
    private AccountBlockService accountBlockService;

    @Mock
    private CommentReplyLoader commentReplyLoader;

    @Mock
    private CommentVisibilityPolicy commentVisibilityPolicy;

    private CommentNavigationService commentNavigationService;

    @BeforeEach
    void setUp() {
        commentNavigationService = new CommentNavigationService(
                accountBlockService,
                commentReplyLoader,
                commentVisibilityPolicy,
                new CommentNavigationTargetResolver(commentsRepository, commentVisibilityPolicy),
                new VisibleRootCommentIndexResolver(
                        new VisibleRootCommentScanner(commentsRepository, commentReplyLoader, commentVisibilityPolicy)
                )
        );
    }

    @Test
    void buildCommentTargetPaths_returnsBoardCommentsPathWhenRootParentIsNotVisible() {
        Comments parent = rootComment(10L);
        Comments reply = replyComment(20L, parent);

        when(commentsRepository.findWithBoardAndParentByIdIn(Set.of(20L))).thenReturn(List.of(reply));
        when(commentsRepository.findParentCommentsPageWithAccountByBoardIdOrderByRegTimeAsc(
                eq(1L),
                eq(SoftDeleteState.ACTIVE),
                eq(SoftDeleteState.DELETED),
                eq(false),
                any(),
                any()
        )).thenReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));
        when(commentReplyLoader.loadRepliesByRootParentIds(eq(1L), eq(List.of()), any()))
                .thenReturn(Map.of());
        when(commentVisibilityPolicy.isBlockedAuthor(reply, Set.of())).thenReturn(false);
        when(commentVisibilityPolicy.isReachable(reply, Map.of())).thenReturn(true);
        when(commentVisibilityPolicy.shouldDisplay(reply, Map.of())).thenReturn(true);

        Map<Long, CommentTargetPath> result = commentNavigationService.buildCommentTargetPaths(
                CommentTargetPathsQuery.of(1L, List.of(20L), BoardViewer.anonymous(), 20)
        );

        assertThat(result).containsEntry(20L, CommentTargetPath.of("/boards/1#board-comments"));
    }

    @Test
    void buildCommentTargetPaths_returnsPagedCommentPathWhenRootParentIsVisible() {
        Comments parent = rootComment(10L);
        Comments reply = replyComment(20L, parent);
        Map<Long, List<Comments>> repliesByParentId = Map.of(10L, List.of(reply));

        when(commentsRepository.findWithBoardAndParentByIdIn(Set.of(20L))).thenReturn(List.of(reply));
        when(commentsRepository.findParentCommentsPageWithAccountByBoardIdOrderByRegTimeAsc(
                eq(1L),
                eq(SoftDeleteState.ACTIVE),
                eq(SoftDeleteState.DELETED),
                eq(false),
                any(),
                any()
        )).thenReturn(new PageImpl<>(List.of(parent), PageRequest.of(0, 100), 1));
        when(commentReplyLoader.loadRepliesByRootParentIds(eq(1L), eq(List.of(parent)), any()))
                .thenReturn(repliesByParentId);
        when(commentVisibilityPolicy.shouldDisplay(parent, repliesByParentId)).thenReturn(true);
        when(commentVisibilityPolicy.isBlockedAuthor(reply, Set.of())).thenReturn(false);
        when(commentVisibilityPolicy.isReachable(reply, repliesByParentId)).thenReturn(true);
        when(commentVisibilityPolicy.shouldDisplay(reply, repliesByParentId)).thenReturn(true);

        Map<Long, CommentTargetPath> result = commentNavigationService.buildCommentTargetPaths(
                CommentTargetPathsQuery.of(1L, List.of(20L), BoardViewer.anonymous(), 20)
        );

        assertThat(result).containsEntry(20L, CommentTargetPath.of("/boards/1?commentPage=0&commentSize=20#comment-20"));
    }

    private Comments rootComment(Long id) {
        return Comments.builder()
                .id(id)
                .board(board())
                .account(account(2L))
                .contents("root")
                .state(SoftDeleteState.ACTIVE)
                .depth(0)
                .build();
    }

    private Comments replyComment(Long id, Comments parent) {
        return Comments.builder()
                .id(id)
                .board(board())
                .account(account(3L))
                .contents("reply")
                .parent(parent)
                .rootParentId(parent.getId())
                .depth(1)
                .state(SoftDeleteState.ACTIVE)
                .build();
    }

    private Board board() {
        return Board.builder()
                .id(1L)
                .title("title")
                .contents("contents")
                .build();
    }

    private Account account(Long id) {
        return Account.builder()
                .id(id)
                .name("user-" + id)
                .build();
    }
}

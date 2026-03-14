package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.service.comment.CommentTargetPath;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardNotificationCommentTargetPathServiceUnitTest {

    @Mock
    private CommentNavigationService commentNavigationService;

    @InjectMocks
    private BoardNotificationCommentTargetPathService boardNotificationCommentTargetPathService;

    @Test
    void buildCommentTargetPaths_convertsResolvedPathsWithoutExtraOrderingLogic() {
        when(commentNavigationService.buildCommentTargetPaths(argThat(query ->
                query.boardId() == 10L
                        && query.viewer().equals(BoardViewer.authenticated(1L))
                        && query.commentIds().equals(java.util.Set.of(30L, 31L))
        ))).thenReturn(Map.of(
                30L, CommentTargetPath.of("/boards/10#comment-30"),
                31L, CommentTargetPath.of("/boards/10#comment-31")
        ));

        Map<Long, CommentTargetPath> result =
                boardNotificationCommentTargetPathService.buildCommentTargetPaths(10L, List.of(30L, 31L), 1L);

        assertThat(result).containsEntry(30L, CommentTargetPath.of("/boards/10#comment-30"));
        assertThat(result).containsEntry(31L, CommentTargetPath.of("/boards/10#comment-31"));
    }
}

package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.CommentsRepository;
import kwh.PublicCookedFood.board.repository.LikesRepository;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import kwh.PublicCookedFood.board.service.comment.CommentQueryService;
import kwh.PublicCookedFood.board.repository.BoardStatsRepository;
import kwh.PublicCookedFood.board.service.BoardStatsMutationService;
import kwh.PublicCookedFood.board.service.BoardViewCounterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BoardCounterService {

    private final CommentsRepository commentsRepository;
    private final LikesRepository likesRepository;
    private final CommentQueryService commentQueryService;
    private final BoardStatsRepository boardStatsRepository;
    private final BoardViewCounterService viewCounterService;
    private final BoardStatsMutationService boardStatsMutationService;

    @Transactional
    public long increaseViewsAndGet(long boardId) {
        return viewCounterService.increaseBoardViewAndGet(boardId);
    }

    @Transactional(readOnly = true)
    public long getViews(long boardId) {
        return viewCounterService.getBoardViewCount(boardId);
    }

    @Transactional(readOnly = true)
    public Map<Long, Long> getViews(Collection<Long> boardIds) {
        return viewCounterService.getBoardViewCounts(boardIds);
    }

    @Transactional
    public BoardCounters getDetailCounters(long boardId,
                                           BoardViewer viewer,
                                           boolean increaseViews) {
        long views = increaseViews
                ? viewCounterService.increaseBoardViewAndGet(boardId)
                : viewCounterService.getBoardViewCount(boardId);
        long likes = getLikes(boardId);
        long commentsCount = getCommentCount(boardId, viewer);
        return new BoardCounters(views, likes, commentsCount);
    }

    @Transactional(readOnly = true)
    public long getLikes(long boardId) {
        return boardStatsRepository.findTotalLikesByBoardId(boardId)
                .orElseGet(() -> likesRepository.countByBoardId(boardId));
    }

    @Transactional(readOnly = true)
    public long getCommentCount(long boardId, BoardViewer viewer) {
        return commentQueryService.getCommentsCount(boardId, viewer);
    }

    @Transactional
    public void refreshLikes(long boardId) {
        long likeCount = likesRepository.countByBoardId(boardId);
        boardStatsMutationService.syncLikeCount(boardId, likeCount);
    }

    @Transactional
    public void refreshCommentCount(long boardId) {
        long commentCount = commentsRepository.countByBoardIdAndState(boardId, SoftDeleteState.ACTIVE);
        boardStatsMutationService.syncCommentCount(boardId, commentCount);
    }
}

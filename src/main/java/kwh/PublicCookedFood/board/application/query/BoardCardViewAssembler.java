package kwh.PublicCookedFood.board.application.query;

import kwh.PublicCookedFood.board.application.query.view.BoardCardView;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummary;
import kwh.PublicCookedFood.board.service.support.BoardThumbnailExtractor;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Component
public class BoardCardViewAssembler {

    public Page<BoardCardView> toPage(Page<Board> boards,
                                      Map<Long, BoardStatsSummary> boardStatsMap) {
        return toPage(boards, boardStatsMap, BoardThumbnailExtractor.ThumbnailData.empty(), 0);
    }

    public Page<BoardCardView> toPage(Page<Board> boards,
                                      Map<Long, BoardStatsSummary> boardStatsMap,
                                      BoardThumbnailExtractor.ThumbnailData thumbnailData,
                                      int featuredLikeThreshold) {
        return boards.map(board -> toView(board, boardStatsMap, thumbnailData, featuredLikeThreshold));
    }

    public List<BoardCardView> toList(List<Board> boards,
                                      Map<Long, BoardStatsSummary> boardStatsMap) {
        return toList(boards, boardStatsMap, BoardThumbnailExtractor.ThumbnailData.empty(), 0);
    }

    public List<BoardCardView> toList(List<Board> boards,
                                      Map<Long, BoardStatsSummary> boardStatsMap,
                                      BoardThumbnailExtractor.ThumbnailData thumbnailData,
                                      int featuredLikeThreshold) {
        if (boards == null || boards.isEmpty()) {
            return List.of();
        }
        return boards.stream()
                .map(board -> toView(board, boardStatsMap, thumbnailData, featuredLikeThreshold))
                .toList();
    }

    private BoardCardView toView(Board board,
                                 Map<Long, BoardStatsSummary> boardStatsMap,
                                 BoardThumbnailExtractor.ThumbnailData thumbnailData,
                                 int featuredLikeThreshold) {
        if (board == null) {
            throw new IllegalArgumentException("board must not be null");
        }
        Long boardId = board.getId();
        BoardStatsSummary stats = resolveStats(boardId, boardStatsMap);
        String thumbnailUrl = boardId == null ? null : safeThumbnailData(thumbnailData).thumbnailUrlByBoardId().get(boardId);
        boolean hasImage = boardId != null && Boolean.TRUE.equals(safeThumbnailData(thumbnailData).hasImageByBoardId().get(boardId));
        boolean featured = featuredLikeThreshold > 0 && stats.likes() >= featuredLikeThreshold;
        return BoardCardView.of(board, stats, thumbnailUrl, hasImage, featured);
    }

    private BoardStatsSummary resolveStats(Long boardId,
                                           Map<Long, BoardStatsSummary> boardStatsMap) {
        if (boardId == null || boardStatsMap == null) {
            return BoardStatsSummary.ZERO;
        }
        return boardStatsMap.getOrDefault(boardId, BoardStatsSummary.ZERO);
    }

    private BoardThumbnailExtractor.ThumbnailData safeThumbnailData(BoardThumbnailExtractor.ThumbnailData thumbnailData) {
        return thumbnailData == null ? BoardThumbnailExtractor.ThumbnailData.empty() : thumbnailData;
    }
}

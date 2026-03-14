package kwh.PublicCookedFood.board.application.query.view;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.dto.response.BoardAuthorView;
import kwh.PublicCookedFood.board.dto.response.BoardSectionView;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummary;
import lombok.Getter;

import java.time.LocalDateTime;
import java.util.Objects;

@Getter
public final class BoardCardView {

    private final Long boardId;
    private final String title;
    private final BoardAuthorView author;
    private final BoardSectionView section;
    private final LocalDateTime regTime;
    private final long views;
    private final long likes;
    private final long commentsCount;
    private final String thumbnailUrl;
    private final boolean hasImage;
    private final boolean featured;

    public BoardCardView(Long boardId,
                         String title,
                         BoardAuthorView author,
                         BoardSectionView section,
                         LocalDateTime regTime,
                         long views,
                         long likes,
                         long commentsCount,
                         String thumbnailUrl,
                         boolean hasImage,
                         boolean featured) {
        this.boardId = boardId;
        this.title = title;
        this.author = author == null ? BoardAuthorView.anonymous() : author;
        this.section = section == null ? BoardSectionView.unassigned() : section;
        this.regTime = regTime;
        if (views < 0L) {
            throw new IllegalArgumentException("views must not be negative");
        }
        if (likes < 0L) {
            throw new IllegalArgumentException("likes must not be negative");
        }
        if (commentsCount < 0L) {
            throw new IllegalArgumentException("commentsCount must not be negative");
        }
        this.views = views;
        this.likes = likes;
        this.commentsCount = commentsCount;
        this.thumbnailUrl = normalizeThumbnailUrl(thumbnailUrl);
        this.hasImage = hasImage;
        this.featured = featured;
    }

    public static BoardCardView of(Board board,
                                   BoardStatsSummary stats,
                                   String thumbnailUrl,
                                   boolean hasImage,
                                   boolean featured) {
        Objects.requireNonNull(board, "board");
        BoardStatsSummary safeStats = stats == null ? BoardStatsSummary.ZERO : stats;
        return new BoardCardView(
                board.getId(),
                board.getTitle(),
                BoardAuthorView.from(board.getAccount()),
                BoardSectionView.from(board.getSection()),
                board.getRegTime(),
                safeStats.views(),
                safeStats.likes(),
                safeStats.commentsCount(),
                thumbnailUrl,
                hasImage,
                featured
        );
    }

    public static BoardCardView of(Board board, BoardStatsSummary stats) {
        return of(board, stats, null, false, false);
    }

    public Long boardId() {
        return boardId;
    }

    public String title() {
        return title;
    }

    public BoardAuthorView author() {
        return author;
    }

    public BoardSectionView section() {
        return section;
    }

    public LocalDateTime regTime() {
        return regTime;
    }

    public long views() {
        return views;
    }

    public long likes() {
        return likes;
    }

    public long commentsCount() {
        return commentsCount;
    }

    public String thumbnailUrl() {
        return thumbnailUrl;
    }

    public boolean hasImage() {
        return hasImage;
    }

    public boolean getHasImage() {
        return hasImage;
    }

    public boolean featured() {
        return featured;
    }

    private static String normalizeThumbnailUrl(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

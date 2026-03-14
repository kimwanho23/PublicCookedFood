package kwh.PublicCookedFood.board.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Objects;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_stats")
public class BoardStats {

    @Id
    @Column(name = "board_id")
    private Long boardId;

    @Column(name = "total_views", nullable = false)
    private Long totalViews;

    @Column(name = "total_likes", nullable = false)
    private Long totalLikes;

    @Column(name = "total_comments", nullable = false)
    private Long totalComments;

    @Column(name = "score", nullable = false, precision = 18, scale = 6)
    private BigDecimal score;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public BoardStats(Long boardId,
                      Long totalViews,
                      Long totalLikes,
                      Long totalComments,
                      BigDecimal score,
                      LocalDateTime updatedAt) {
        this.boardId = boardId;
        this.totalViews = Objects.requireNonNullElse(totalViews, 0L);
        this.totalLikes = Objects.requireNonNullElse(totalLikes, 0L);
        this.totalComments = Objects.requireNonNullElse(totalComments, 0L);
        this.score = score == null ? BigDecimal.ZERO : score;
        this.updatedAt = updatedAt == null ? LocalDateTime.now() : updatedAt;
    }

    public static BoardStats initialize(Long boardId, long totalViews) {
        return initialize(boardId, totalViews, 0L, 0L);
    }

    public static BoardStats initialize(Long boardId,
                                        long totalViews,
                                        long totalLikes,
                                        long totalComments) {
        long normalizedViews = Math.max(0L, totalViews);
        long normalizedLikes = Math.max(0L, totalLikes);
        long normalizedComments = Math.max(0L, totalComments);
        return BoardStats.builder()
                .boardId(boardId)
                .totalViews(normalizedViews)
                .totalLikes(normalizedLikes)
                .totalComments(normalizedComments)
                .score(BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now())
                .build();
    }
}

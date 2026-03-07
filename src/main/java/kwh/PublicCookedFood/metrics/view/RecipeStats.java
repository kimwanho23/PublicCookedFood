package kwh.PublicCookedFood.metrics.view;

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
@Table(name = "recipe_stats")
public class RecipeStats {

    @Id
    @Column(name = "recipe_id")
    private Long recipeId;

    @Column(name = "total_views", nullable = false)
    private Long totalViews;

    @Column(name = "total_likes", nullable = false)
    private Long totalLikes;

    @Column(name = "total_bookmarks", nullable = false)
    private Long totalBookmarks;

    @Column(name = "score", nullable = false, precision = 18, scale = 6)
    private BigDecimal score;

    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    @Builder
    public RecipeStats(Long recipeId,
                       Long totalViews,
                       Long totalLikes,
                       Long totalBookmarks,
                       BigDecimal score,
                       LocalDateTime updatedAt) {
        this.recipeId = recipeId;
        this.totalViews = Objects.requireNonNullElse(totalViews, 0L);
        this.totalLikes = Objects.requireNonNullElse(totalLikes, 0L);
        this.totalBookmarks = Objects.requireNonNullElse(totalBookmarks, 0L);
        this.score = score == null ? BigDecimal.ZERO : score;
        this.updatedAt = updatedAt == null ? LocalDateTime.now() : updatedAt;
    }

    public static RecipeStats initialize(Long recipeId, long totalViews) {
        long normalizedViews = Math.max(0L, totalViews);
        return RecipeStats.builder()
                .recipeId(recipeId)
                .totalViews(normalizedViews)
                .totalLikes(0L)
                .totalBookmarks(0L)
                .score(BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now())
                .build();
    }

    public void updateBookmarksAndScore(long totalBookmarks, BigDecimal score) {
        this.totalBookmarks = Math.max(0L, totalBookmarks);
        this.score = score == null ? BigDecimal.ZERO : score;
    }

    public void updateScore(BigDecimal score) {
        this.score = score == null ? BigDecimal.ZERO : score;
    }
}

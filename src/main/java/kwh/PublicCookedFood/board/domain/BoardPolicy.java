package kwh.PublicCookedFood.board.domain;

import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_policy")
public class BoardPolicy {

    private static final int DEFAULT_FEATURED_LIKE_THRESHOLD = 10;
    private static final BoardThumbnailDisplayMode DEFAULT_THUMBNAIL_DISPLAY_MODE = BoardThumbnailDisplayMode.LEFT;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer featuredLikeThreshold;

    @Enumerated(EnumType.STRING)
    @Column(name = "thumbnail_display_mode", nullable = false, length = 20)
    private BoardThumbnailDisplayMode thumbnailDisplayMode;

    @Builder
    public BoardPolicy(Long id, Integer featuredLikeThreshold, BoardThumbnailDisplayMode thumbnailDisplayMode) {
        this.id = id;
        this.featuredLikeThreshold = featuredLikeThreshold == null
                ? DEFAULT_FEATURED_LIKE_THRESHOLD
                : featuredLikeThreshold;
        this.thumbnailDisplayMode = thumbnailDisplayMode == null
                ? DEFAULT_THUMBNAIL_DISPLAY_MODE
                : thumbnailDisplayMode;
    }

    public static BoardPolicy createDefault() {
        return BoardPolicy.builder()
                .featuredLikeThreshold(DEFAULT_FEATURED_LIKE_THRESHOLD)
                .thumbnailDisplayMode(DEFAULT_THUMBNAIL_DISPLAY_MODE)
                .build();
    }

    public void updateFeaturedLikeThreshold(Integer featuredLikeThreshold) {
        this.featuredLikeThreshold = featuredLikeThreshold;
    }

    public void updateThumbnailDisplayMode(BoardThumbnailDisplayMode thumbnailDisplayMode) {
        this.thumbnailDisplayMode = thumbnailDisplayMode == null
                ? DEFAULT_THUMBNAIL_DISPLAY_MODE
                : thumbnailDisplayMode;
    }

    @PrePersist
    protected void prePersist() {
        if (thumbnailDisplayMode == null) {
            thumbnailDisplayMode = DEFAULT_THUMBNAIL_DISPLAY_MODE;
        }
    }
}

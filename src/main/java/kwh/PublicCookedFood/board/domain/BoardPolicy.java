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

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private Integer featuredLikeThreshold;

    @Builder
    public BoardPolicy(Long id, Integer featuredLikeThreshold) {
        this.id = id;
        this.featuredLikeThreshold = featuredLikeThreshold == null
                ? DEFAULT_FEATURED_LIKE_THRESHOLD
                : featuredLikeThreshold;
    }

    public static BoardPolicy createDefault() {
        return BoardPolicy.builder()
                .featuredLikeThreshold(DEFAULT_FEATURED_LIKE_THRESHOLD)
                .build();
    }

    public void updateFeaturedLikeThreshold(Integer featuredLikeThreshold) {
        this.featuredLikeThreshold = featuredLikeThreshold;
    }
}

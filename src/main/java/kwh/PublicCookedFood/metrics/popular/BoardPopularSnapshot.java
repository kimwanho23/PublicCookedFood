package kwh.PublicCookedFood.metrics.popular;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "board_featured_ranking")
public class BoardPopularSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "ranking_type", nullable = false, length = 40)
    private String rankingType;

    @Column(name = "section_key", nullable = false, length = 40)
    private String sectionKey;

    @Column(name = "rank_no", nullable = false)
    private Integer rankNo;

    @Column(name = "board_id", nullable = false)
    private Long boardId;

    @Column(name = "score", nullable = false, precision = 18, scale = 6)
    private BigDecimal score;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    public BoardPopularSnapshot(Long id,
                                String rankingType,
                                String sectionKey,
                                Integer rankNo,
                                Long boardId,
                                BigDecimal score,
                                LocalDateTime generatedAt,
                                LocalDateTime expiresAt) {
        this.id = id;
        this.rankingType = rankingType;
        this.sectionKey = sectionKey == null ? "" : sectionKey;
        this.rankNo = rankNo;
        this.boardId = boardId;
        this.score = score == null ? BigDecimal.ZERO : score;
        this.generatedAt = generatedAt;
        this.expiresAt = expiresAt;
    }
}

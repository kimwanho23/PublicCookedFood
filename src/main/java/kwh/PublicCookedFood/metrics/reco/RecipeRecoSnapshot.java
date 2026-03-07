package kwh.PublicCookedFood.metrics.reco;

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
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "recipe_slot_recommendation")
public class RecipeRecoSnapshot {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "slot_date", nullable = false)
    private LocalDate slotDate;

    @Column(name = "slot_type", nullable = false, length = 20)
    private String slotType;

    @Column(name = "rank_no", nullable = false)
    private Integer rankNo;

    @Column(name = "recipe_id", nullable = false)
    private Long recipeId;

    @Column(name = "score", nullable = false, precision = 18, scale = 6)
    private BigDecimal score;

    @Column(name = "generated_at", nullable = false)
    private LocalDateTime generatedAt;

    @Column(name = "expires_at", nullable = false)
    private LocalDateTime expiresAt;

    @Builder
    public RecipeRecoSnapshot(Long id,
                              LocalDate slotDate,
                              String slotType,
                              Integer rankNo,
                              Long recipeId,
                              BigDecimal score,
                              LocalDateTime generatedAt,
                              LocalDateTime expiresAt) {
        this.id = id;
        this.slotDate = slotDate;
        this.slotType = slotType;
        this.rankNo = rankNo;
        this.recipeId = recipeId;
        this.score = score == null ? BigDecimal.ZERO : score;
        this.generatedAt = generatedAt;
        this.expiresAt = expiresAt;
    }
}

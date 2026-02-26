package kwh.PublicCookedFood.food.entity;

import jakarta.persistence.*;
import kwh.PublicCookedFood.common.BaseTimeEntity;
import kwh.PublicCookedFood.user.domain.Users;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "recipe_review", uniqueConstraints = {
        @UniqueConstraint(name = "uk_recipe_review_user_recipe", columnNames = {"user_id", "recipe_row_num"})
}, indexes = {
        @Index(name = "idx_recipe_review_recipe_regtime", columnList = "recipe_row_num, regTime"),
        @Index(name = "idx_recipe_review_recipe_rating", columnList = "recipe_row_num, rating"),
        @Index(name = "idx_recipe_review_user_regtime", columnList = "user_id, regTime")
})
public class RecipeReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Users user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_row_num", referencedColumnName = "row_NUM", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Recipe_INFO recipe;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 500)
    private String contents;

    @Builder
    public RecipeReview(Long id, Users user, Recipe_INFO recipe, Integer rating, String contents) {
        this.id = id;
        this.user = user;
        this.recipe = recipe;
        this.rating = normalizeRating(rating);
        this.contents = normalizeText(contents);
    }

    public void update(Integer rating, String contents) {
        this.rating = normalizeRating(rating);
        this.contents = normalizeText(contents);
    }

    private int normalizeRating(Integer rawRating) {
        if (rawRating == null) {
            return 5;
        }
        if (rawRating < 1) {
            return 1;
        }
        if (rawRating > 5) {
            return 5;
        }
        return rawRating;
    }

    private String normalizeText(String rawText) {
        if (rawText == null) {
            return null;
        }
        String trimmed = rawText.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

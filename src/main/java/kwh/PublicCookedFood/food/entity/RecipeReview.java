package kwh.PublicCookedFood.food.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;
import kwh.PublicCookedFood.common.BaseTimeEntity;
import kwh.PublicCookedFood.account.domain.Account;
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
        @UniqueConstraint(name = "uk_recipe_review_account_recipe", columnNames = {"account_id", "recipe_row_num"})
}, indexes = {
        @Index(name = "idx_recipe_review_recipe_regtime", columnList = "recipe_row_num, regTime"),
        @Index(name = "idx_recipe_review_recipe_rating", columnList = "recipe_row_num, rating"),
        @Index(name = "idx_recipe_review_account_regtime", columnList = "account_id, regTime")
})
public class RecipeReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_row_num", referencedColumnName = "row_NUM", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Recipe_INFO recipe;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 500)
    private String contents;

    @Builder(builderMethodName = "builder")
    private static RecipeReview create(Long id, Account account, Recipe_INFO recipe, Integer rating, String contents) {
        RecipeReview review = new RecipeReview();
        review.id = id;
        review.account = account;
        review.recipe = recipe;
        review.rating = validateRatingValue(rating);
        review.contents = normalizeTextValue(contents);
        return review;
    }

    public void update(Integer rating, String contents) {
        this.rating = validateRatingValue(rating);
        this.contents = normalizeTextValue(contents);
    }

    private static Integer validateRatingValue(Integer rawRating) {
        if (rawRating == null) {
            throw new IllegalArgumentException("리뷰 평점을 선택해주세요.");
        }
        if (rawRating < 1) {
            throw new IllegalArgumentException("리뷰 평점은 1점부터 5점 사이여야 합니다.");
        }
        if (rawRating > 5) {
            throw new IllegalArgumentException("리뷰 평점은 1점부터 5점 사이여야 합니다.");
        }
        return rawRating;
    }

    private static String normalizeTextValue(String rawText) {
        if (rawText == null) {
            return null;
        }
        String trimmed = rawText.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}

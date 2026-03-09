package kwh.PublicCookedFood.userrecipe.domain;

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
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.common.BaseTimeEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_recipe_review", uniqueConstraints = {
        @UniqueConstraint(name = "uk_user_recipe_review_account_recipe", columnNames = {"account_id", "recipe_id"})
}, indexes = {
        @Index(name = "idx_user_recipe_review_recipe_regtime", columnList = "recipe_id, regTime"),
        @Index(name = "idx_user_recipe_review_recipe_rating", columnList = "recipe_id, rating"),
        @Index(name = "idx_user_recipe_review_account_regtime", columnList = "account_id, regTime")
})
public class UserRecipeReview extends BaseTimeEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserRecipe recipe;

    @Column(nullable = false)
    private Integer rating;

    @Column(length = 500)
    private String contents;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder(builderMethodName = "builder")
    private static UserRecipeReview create(Long id,
                                           Account account,
                                           UserRecipe recipe,
                                           Integer rating,
                                           String contents,
                                           String imageUrl) {
        UserRecipeReview review = new UserRecipeReview();
        review.id = id;
        review.account = account;
        review.recipe = recipe;
        review.rating = validateRatingValue(rating);
        review.contents = normalizeTextValue(contents, 500, "리뷰 내용");
        review.imageUrl = normalizeTextValue(imageUrl, 500, "리뷰 이미지");
        return review;
    }

    public void update(Integer rating, String contents, String imageUrl) {
        this.rating = validateRatingValue(rating);
        this.contents = normalizeTextValue(contents, 500, "리뷰 내용");
        this.imageUrl = normalizeTextValue(imageUrl, 500, "리뷰 이미지");
    }

    private static Integer validateRatingValue(Integer rawRating) {
        if (rawRating == null) {
            throw new IllegalArgumentException("리뷰 평점을 선택해주세요.");
        }
        if (rawRating < 1 || rawRating > 5) {
            throw new IllegalArgumentException("리뷰 평점은 1점부터 5점 사이여야 합니다.");
        }
        return rawRating;
    }

    private static String normalizeTextValue(String rawText, int maxLength, String fieldLabel) {
        if (rawText == null) {
            return null;
        }
        String trimmed = rawText.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldLabel + " 길이가 너무 깁니다.");
        }
        return trimmed;
    }
}

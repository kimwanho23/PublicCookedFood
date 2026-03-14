package kwh.PublicCookedFood.userrecipe.domain;

import jakarta.persistence.CascadeType;
import jakarta.persistence.Column;
import jakarta.persistence.Convert;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.OneToMany;
import jakarta.persistence.Table;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.common.persistence.SoftDeleteStateConverter;
import kwh.PublicCookedFood.common.BaseEntity;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.BatchSize;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_recipe", indexes = {
        @Index(name = "idx_user_recipe_account_regtime", columnList = "account_id, regTime"),
        @Index(name = "idx_user_recipe_state_regtime", columnList = "state, regTime"),
        @Index(name = "idx_user_recipe_state_title", columnList = "state, title"),
        @Index(name = "idx_user_recipe_state_account_regtime", columnList = "state, account_id, regTime")
})
public class UserRecipe extends BaseEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "account_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private Account account;

    @Column(nullable = false, length = 200)
    private String title;

    @Column(length = 500)
    private String summary;

    @Column(name = "thumbnail_url", nullable = false, length = 500)
    private String thumbnailUrl;

    @Column(name = "cooking_time", length = 50)
    private String cookingTime;

    @Column(length = 50)
    private String servings;

    @Column(length = 20)
    private String difficulty;

    @Column(nullable = false, length = 1)
    @Convert(converter = SoftDeleteStateConverter.class)
    private SoftDeleteState state = SoftDeleteState.ACTIVE;

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "recipe", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRecipeIngredient> ingredients = new ArrayList<>();

    @BatchSize(size = 100)
    @OneToMany(mappedBy = "recipe", fetch = FetchType.LAZY, cascade = CascadeType.ALL, orphanRemoval = true)
    private List<UserRecipeStep> steps = new ArrayList<>();

    @Builder(builderMethodName = "builder")
    private static UserRecipe create(Long id,
                                     Account account,
                                     String title,
                                     String summary,
                                     String thumbnailUrl,
                                     String cookingTime,
                                     String servings,
                                     String difficulty,
                                     SoftDeleteState state,
                                     List<UserRecipeIngredient> ingredients,
                                     List<UserRecipeStep> steps) {
        UserRecipe recipe = new UserRecipe();
        recipe.id = id;
        recipe.account = account;
        recipe.title = normalizeRequiredText(title, 200, "레시피 제목");
        recipe.summary = normalizeOptionalText(summary, 500, "레시피 소개");
        recipe.thumbnailUrl = normalizeRequiredText(thumbnailUrl, 500, "대표 이미지");
        recipe.cookingTime = normalizeOptionalText(cookingTime, 50, "조리 시간");
        recipe.servings = normalizeOptionalText(servings, 50, "인분");
        recipe.difficulty = normalizeOptionalText(difficulty, 20, "난이도");
        recipe.state = state == null ? SoftDeleteState.ACTIVE : state;
        recipe.replaceIngredients(ingredients);
        recipe.replaceSteps(steps);
        return recipe;
    }

    public List<UserRecipeIngredient> getIngredients() {
        return Collections.unmodifiableList(ingredients);
    }

    public List<UserRecipeStep> getSteps() {
        return Collections.unmodifiableList(steps);
    }

    public void updateRecipe(String title,
                             String summary,
                             String thumbnailUrl,
                             String cookingTime,
                             String servings,
                             String difficulty) {
        this.title = normalizeRequiredText(title, 200, "레시피 제목");
        this.summary = normalizeOptionalText(summary, 500, "레시피 소개");
        this.thumbnailUrl = normalizeRequiredText(thumbnailUrl, 500, "대표 이미지");
        this.cookingTime = normalizeOptionalText(cookingTime, 50, "조리 시간");
        this.servings = normalizeOptionalText(servings, 50, "인분");
        this.difficulty = normalizeOptionalText(difficulty, 20, "난이도");
    }

    public void replaceIngredients(List<UserRecipeIngredient> newIngredients) {
        this.ingredients.clear();
        if (newIngredients == null) {
            return;
        }
        newIngredients.forEach(this::addIngredient);
    }

    public void replaceSteps(List<UserRecipeStep> newSteps) {
        this.steps.clear();
        if (newSteps == null) {
            return;
        }
        newSteps.forEach(this::addStep);
    }

    public void markDeleted() {
        this.state = SoftDeleteState.DELETED;
    }

    private void addIngredient(UserRecipeIngredient ingredient) {
        if (ingredient == null) {
            return;
        }
        ingredient.assignRecipe(this);
        this.ingredients.add(ingredient);
    }

    private void addStep(UserRecipeStep step) {
        if (step == null) {
            return;
        }
        step.assignRecipe(this);
        this.steps.add(step);
    }

    private static String normalizeRequiredText(String rawValue, int maxLength, String fieldLabel) {
        String normalized = normalizeOptionalText(rawValue, maxLength, fieldLabel);
        if (normalized == null) {
            throw new IllegalArgumentException(fieldLabel + "는 필수입니다.");
        }
        return normalized;
    }

    private static String normalizeOptionalText(String rawValue, int maxLength, String fieldLabel) {
        if (rawValue == null) {
            return null;
        }
        String trimmed = rawValue.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldLabel + " 길이가 너무 깁니다.");
        }
        return trimmed;
    }
}

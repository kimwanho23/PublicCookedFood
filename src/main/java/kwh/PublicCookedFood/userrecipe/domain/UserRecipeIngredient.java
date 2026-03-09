package kwh.PublicCookedFood.userrecipe.domain;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_recipe_ingredient", indexes = {
        @Index(name = "idx_user_recipe_ingredient_recipe_group_sort", columnList = "recipe_id, ingredient_group, sort_order")
})
public class UserRecipeIngredient {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserRecipe recipe;

    @Enumerated(EnumType.STRING)
    @Column(name = "ingredient_group", nullable = false, length = 20)
    private UserRecipeIngredientGroup ingredientGroup;

    @Column(name = "ingredient_name", nullable = false, length = 120)
    private String ingredientName;

    @Column(name = "amount_text", nullable = false, length = 120)
    private String amountText;

    @Column(name = "sort_order", nullable = false)
    private Integer sortOrder;

    @Builder(builderMethodName = "builder")
    private static UserRecipeIngredient create(Long id,
                                               UserRecipe recipe,
                                               UserRecipeIngredientGroup ingredientGroup,
                                               String ingredientName,
                                               String amountText,
                                               Integer sortOrder) {
        UserRecipeIngredient ingredient = new UserRecipeIngredient();
        ingredient.id = id;
        ingredient.recipe = recipe;
        ingredient.ingredientGroup = ingredientGroup == null
                ? throwInvalidGroup()
                : ingredientGroup;
        ingredient.ingredientName = normalizeRequiredText(ingredientName, 120, "재료명");
        ingredient.amountText = normalizeRequiredText(amountText, 120, "재료 용량");
        ingredient.sortOrder = validateSortOrder(sortOrder);
        return ingredient;
    }

    void assignRecipe(UserRecipe recipe) {
        this.recipe = recipe;
    }

    private static UserRecipeIngredientGroup throwInvalidGroup() {
        throw new IllegalArgumentException("재료 구분은 필수입니다.");
    }

    private static Integer validateSortOrder(Integer sortOrder) {
        if (sortOrder == null || sortOrder < 0) {
            throw new IllegalArgumentException("재료 순서는 0 이상이어야 합니다.");
        }
        return sortOrder;
    }

    private static String normalizeRequiredText(String rawValue, int maxLength, String fieldLabel) {
        if (rawValue == null || rawValue.isBlank()) {
            throw new IllegalArgumentException(fieldLabel + "는 필수입니다.");
        }
        String trimmed = rawValue.trim();
        if (trimmed.length() > maxLength) {
            throw new IllegalArgumentException(fieldLabel + " 길이가 너무 깁니다.");
        }
        return trimmed;
    }
}

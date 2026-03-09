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
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;

@Entity
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@Table(name = "user_recipe_step", indexes = {
        @Index(name = "idx_user_recipe_step_recipe_step_no", columnList = "recipe_id, step_no")
})
public class UserRecipeStep {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "recipe_id", referencedColumnName = "id", nullable = false)
    @OnDelete(action = OnDeleteAction.CASCADE)
    private UserRecipe recipe;

    @Column(name = "step_no", nullable = false)
    private Integer stepNo;

    @Column(nullable = false, length = 1000)
    private String contents;

    @Column(length = 500)
    private String tip;

    @Column(name = "image_url", length = 500)
    private String imageUrl;

    @Builder(builderMethodName = "builder")
    private static UserRecipeStep create(Long id,
                                         UserRecipe recipe,
                                         Integer stepNo,
                                         String contents,
                                         String tip,
                                         String imageUrl) {
        UserRecipeStep step = new UserRecipeStep();
        step.id = id;
        step.recipe = recipe;
        step.stepNo = validateStepNo(stepNo);
        step.contents = normalizeRequiredText(contents, 1000, "조리 단계");
        step.tip = normalizeOptionalText(tip, 500, "단계 팁");
        step.imageUrl = normalizeOptionalText(imageUrl, 500, "단계 이미지");
        return step;
    }

    void assignRecipe(UserRecipe recipe) {
        this.recipe = recipe;
    }

    private static Integer validateStepNo(Integer stepNo) {
        if (stepNo == null || stepNo < 1) {
            throw new IllegalArgumentException("조리 단계 번호는 1 이상이어야 합니다.");
        }
        return stepNo;
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

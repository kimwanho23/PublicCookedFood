package kwh.PublicCookedFood.userrecipe.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.PositiveOrZero;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserRecipeIngredientRequest {

    @NotBlank(message = "재료 구분은 필수입니다.")
    private String ingredientGroup;

    @NotBlank(message = "재료명은 필수입니다.")
    @Size(max = 120, message = "재료명은 120자 이하로 입력해주세요.")
    private String ingredientName;

    @NotBlank(message = "재료 용량은 필수입니다.")
    @Size(max = 120, message = "재료 용량은 120자 이하로 입력해주세요.")
    private String amountText;

    @PositiveOrZero(message = "재료 순서는 0 이상이어야 합니다.")
    private Integer sortOrder;
}

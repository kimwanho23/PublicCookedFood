package kwh.PublicCookedFood.userrecipe.dto.request;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
public class UserRecipeWriteRequest {

    @NotBlank(message = "레시피 제목은 필수입니다.")
    @Size(max = 200, message = "레시피 제목은 200자 이하로 입력해주세요.")
    private String title;

    @Size(max = 500, message = "레시피 소개는 500자 이하로 입력해주세요.")
    private String summary;

    @NotBlank(message = "대표 이미지는 필수입니다.")
    @Size(max = 500, message = "대표 이미지 URL은 500자 이하로 입력해주세요.")
    private String thumbnailUrl;

    @Size(max = 50, message = "조리 시간은 50자 이하로 입력해주세요.")
    private String cookingTime;

    @Size(max = 50, message = "인분은 50자 이하로 입력해주세요.")
    private String servings;

    @Size(max = 20, message = "난이도는 20자 이하로 입력해주세요.")
    private String difficulty;

    @Valid
    @NotEmpty(message = "재료는 최소 1개 이상 필요합니다.")
    private List<UserRecipeIngredientRequest> ingredients = new ArrayList<>();

    @Valid
    @NotEmpty(message = "조리 단계는 최소 1개 이상 필요합니다.")
    private List<UserRecipeStepRequest> steps = new ArrayList<>();
}

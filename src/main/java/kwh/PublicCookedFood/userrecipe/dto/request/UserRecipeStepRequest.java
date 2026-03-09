package kwh.PublicCookedFood.userrecipe.dto.request;

import jakarta.validation.constraints.Positive;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserRecipeStepRequest {

    @Positive(message = "단계 번호는 1 이상이어야 합니다.")
    private Integer stepNo;

    @NotBlank(message = "조리 단계 설명은 필수입니다.")
    @Size(max = 1000, message = "조리 단계 설명은 1000자 이하로 입력해주세요.")
    private String contents;

    @Size(max = 500, message = "단계 팁은 500자 이하로 입력해주세요.")
    private String tip;

    @Size(max = 500, message = "단계 이미지 URL은 500자 이하로 입력해주세요.")
    private String imageUrl;
}

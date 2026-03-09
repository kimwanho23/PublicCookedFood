package kwh.PublicCookedFood.userrecipe.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class UserRecipeReviewUpsertRequest {

    @NotNull(message = "리뷰 평점은 필수입니다.")
    @Min(value = 1, message = "리뷰 평점은 1점 이상이어야 합니다.")
    @Max(value = 5, message = "리뷰 평점은 5점 이하여야 합니다.")
    private Integer rating;

    @Size(max = 500, message = "리뷰 내용은 500자 이하로 입력해주세요.")
    private String contents;

    @Size(max = 500, message = "리뷰 이미지 URL은 500자 이하로 입력해주세요.")
    private String imageUrl;
}

package kwh.PublicCookedFood.board.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class BoardPolicyUpdateRequest {

    @NotNull(message = "추천 임계값은 필수입니다.")
    @Min(value = 1, message = "추천 임계값은 1 이상이어야 합니다.")
    @Max(value = 10000, message = "추천 임계값은 10000 이하로 입력해 주세요.")
    private Integer featuredLikeThreshold;

    @NotNull(message = "썸네일 표시 모드는 필수입니다.")
    private BoardThumbnailDisplayMode thumbnailDisplayMode;
}

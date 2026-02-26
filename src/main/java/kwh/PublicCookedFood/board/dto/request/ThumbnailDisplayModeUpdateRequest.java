package kwh.PublicCookedFood.board.dto.request;

import jakarta.validation.constraints.NotNull;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
public class ThumbnailDisplayModeUpdateRequest {

    @NotNull(message = "썸네일 표시 모드는 필수입니다.")
    private BoardThumbnailDisplayMode thumbnailDisplayMode;
}

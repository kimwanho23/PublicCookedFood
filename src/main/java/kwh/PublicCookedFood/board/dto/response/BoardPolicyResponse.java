package kwh.PublicCookedFood.board.dto.response;

import kwh.PublicCookedFood.board.domain.BoardPolicy;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BoardPolicyResponse {

    private Integer featuredLikeThreshold;
    private String thumbnailDisplayMode;

    @Builder
    public BoardPolicyResponse(Integer featuredLikeThreshold, String thumbnailDisplayMode) {
        this.featuredLikeThreshold = featuredLikeThreshold;
        this.thumbnailDisplayMode = thumbnailDisplayMode;
    }

    public static BoardPolicyResponse from(BoardPolicy boardPolicy) {
        BoardThumbnailDisplayMode thumbnailDisplayMode = boardPolicy.getThumbnailDisplayMode() == null
                ? BoardThumbnailDisplayMode.LEFT
                : boardPolicy.getThumbnailDisplayMode();
        return BoardPolicyResponse.builder()
                .featuredLikeThreshold(boardPolicy.getFeaturedLikeThreshold())
                .thumbnailDisplayMode(thumbnailDisplayMode.name())
                .build();
    }
}

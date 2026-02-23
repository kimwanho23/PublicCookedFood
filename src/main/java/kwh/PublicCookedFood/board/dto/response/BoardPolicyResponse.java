package kwh.PublicCookedFood.board.dto.response;

import kwh.PublicCookedFood.board.domain.BoardPolicy;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class BoardPolicyResponse {

    private Integer featuredLikeThreshold;

    @Builder
    public BoardPolicyResponse(Integer featuredLikeThreshold) {
        this.featuredLikeThreshold = featuredLikeThreshold;
    }

    public static BoardPolicyResponse from(BoardPolicy boardPolicy) {
        return BoardPolicyResponse.builder()
                .featuredLikeThreshold(boardPolicy.getFeaturedLikeThreshold())
                .build();
    }
}

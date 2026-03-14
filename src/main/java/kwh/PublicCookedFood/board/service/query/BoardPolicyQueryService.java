package kwh.PublicCookedFood.board.service.query;

import kwh.PublicCookedFood.board.domain.BoardPolicy;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import kwh.PublicCookedFood.board.repository.BoardPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardPolicyQueryService {

    private final BoardPolicyRepository boardPolicyRepository;

    @Transactional(readOnly = true)
    public BoardPolicy getPolicy() {
        return boardPolicyRepository.findTopByOrderByIdAsc()
                .orElseGet(BoardPolicy::createDefault);
    }

    @Transactional(readOnly = true)
    public int getFeaturedLikeThreshold() {
        return getPolicy().getFeaturedLikeThreshold();
    }

    @Transactional(readOnly = true)
    public BoardThumbnailDisplayMode getThumbnailDisplayMode() {
        BoardThumbnailDisplayMode thumbnailDisplayMode = getPolicy().getThumbnailDisplayMode();
        return thumbnailDisplayMode == null ? BoardThumbnailDisplayMode.LEFT : thumbnailDisplayMode;
    }
}

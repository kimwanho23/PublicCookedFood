package kwh.PublicCookedFood.board.service;

import jakarta.transaction.Transactional;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import kwh.PublicCookedFood.board.domain.BoardPolicy;
import kwh.PublicCookedFood.board.repository.BoardPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BoardPolicyService {

    private final BoardPolicyRepository boardPolicyRepository;

    @Transactional
    public BoardPolicy getPolicy() {
        return boardPolicyRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> boardPolicyRepository.save(BoardPolicy.createDefault()));
    }

    @Transactional
    public int getFeaturedLikeThreshold() {
        return getPolicy().getFeaturedLikeThreshold();
    }

    @Transactional
    public BoardThumbnailDisplayMode getThumbnailDisplayMode() {
        BoardThumbnailDisplayMode thumbnailDisplayMode = getPolicy().getThumbnailDisplayMode();
        return thumbnailDisplayMode == null ? BoardThumbnailDisplayMode.LEFT : thumbnailDisplayMode;
    }

    @Transactional
    public BoardPolicy updateFeaturedLikeThreshold(int threshold) {
        BoardPolicy policy = getPolicy();
        policy.updateFeaturedLikeThreshold(threshold);
        return policy;
    }

    @Transactional
    public BoardPolicy updateThumbnailDisplayMode(BoardThumbnailDisplayMode thumbnailDisplayMode) {
        BoardPolicy policy = getPolicy();
        policy.updateThumbnailDisplayMode(thumbnailDisplayMode);
        return policy;
    }
}

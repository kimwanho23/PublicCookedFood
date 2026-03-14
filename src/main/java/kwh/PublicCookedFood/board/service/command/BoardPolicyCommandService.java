package kwh.PublicCookedFood.board.service.command;

import kwh.PublicCookedFood.board.domain.BoardPolicy;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import kwh.PublicCookedFood.board.repository.BoardPolicyRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardPolicyCommandService {

    private final BoardPolicyRepository boardPolicyRepository;

    @Transactional
    public BoardPolicy updateFeaturedLikeThreshold(int threshold) {
        BoardPolicy policy = ensurePolicy();
        policy.updateFeaturedLikeThreshold(threshold);
        return policy;
    }

    @Transactional
    public BoardPolicy updateThumbnailDisplayMode(BoardThumbnailDisplayMode thumbnailDisplayMode) {
        BoardPolicy policy = ensurePolicy();
        policy.updateThumbnailDisplayMode(thumbnailDisplayMode);
        return policy;
    }

    private BoardPolicy ensurePolicy() {
        return boardPolicyRepository.findTopByOrderByIdAsc()
                .orElseGet(() -> boardPolicyRepository.save(BoardPolicy.createDefault()));
    }
}

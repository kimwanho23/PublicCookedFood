package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.BoardPolicy;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import kwh.PublicCookedFood.board.dto.response.BoardPolicyResponse;
import kwh.PublicCookedFood.board.service.BoardPolicyService;
import kwh.PublicCookedFood.account.audit.BoardAuditPublisher;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class BoardAdminPolicyFacade {

    private final BoardPolicyService boardPolicyService;
    private final BoardAuditPublisher boardAuditPublisher;

    public BoardAdminFacade.PolicyViewData loadPolicyData() {
        BoardPolicy policy = boardPolicyService.getPolicy();
        return new BoardAdminFacade.PolicyViewData(
                BoardPolicyResponse.from(policy),
                BoardAdminFacade.MIN_FEATURED_THRESHOLD,
                BoardAdminFacade.MAX_FEATURED_THRESHOLD
        );
    }

    @Transactional
    public void updateBoardPolicy(Integer featuredLikeThreshold,
                                  BoardThumbnailDisplayMode thumbnailDisplayMode,
                                  Long actorAccountId) {
        int normalizedThreshold = normalizeFeaturedThreshold(featuredLikeThreshold);
        BoardThumbnailDisplayMode normalizedDisplayMode = thumbnailDisplayMode == null
                ? boardPolicyService.getThumbnailDisplayMode()
                : thumbnailDisplayMode;

        boardPolicyService.updateFeaturedLikeThreshold(normalizedThreshold);
        boardPolicyService.updateThumbnailDisplayMode(normalizedDisplayMode);

        if (actorAccountId != null) {
            boardAuditPublisher.boardPolicyUpdate(actorAccountId, normalizedThreshold, normalizedDisplayMode.name());
        }
    }

    public int normalizeFeaturedThreshold(Integer featuredLikeThreshold) {
        if (featuredLikeThreshold == null) {
            return boardPolicyService.getFeaturedLikeThreshold();
        }
        if (featuredLikeThreshold < BoardAdminFacade.MIN_FEATURED_THRESHOLD) {
            return BoardAdminFacade.MIN_FEATURED_THRESHOLD;
        }
        if (featuredLikeThreshold > BoardAdminFacade.MAX_FEATURED_THRESHOLD) {
            return BoardAdminFacade.MAX_FEATURED_THRESHOLD;
        }
        return featuredLikeThreshold;
    }
}

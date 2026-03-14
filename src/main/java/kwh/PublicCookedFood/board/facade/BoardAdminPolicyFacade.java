package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.BoardPolicy;
import kwh.PublicCookedFood.board.dto.response.BoardPolicyResponse;
import kwh.PublicCookedFood.board.service.command.BoardPolicyCommandService;
import kwh.PublicCookedFood.board.service.command.BoardPolicyUpdateCommand;
import kwh.PublicCookedFood.board.service.query.BoardPolicyQueryService;
import kwh.PublicCookedFood.account.audit.BoardAuditPublisher;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Objects;

@Service
@RequiredArgsConstructor
public class BoardAdminPolicyFacade {

    public static final int MIN_FEATURED_THRESHOLD = 1;
    public static final int MAX_FEATURED_THRESHOLD = 10000;

    private final BoardPolicyQueryService boardPolicyQueryService;
    private final BoardPolicyCommandService boardPolicyCommandService;
    private final BoardAuditPublisher boardAuditPublisher;

    public PolicyViewData loadPolicyData() {
        BoardPolicy policy = boardPolicyQueryService.getPolicy();
        return new PolicyViewData(
                BoardPolicyResponse.from(policy),
                MIN_FEATURED_THRESHOLD,
                MAX_FEATURED_THRESHOLD
        );
    }

    @Transactional
    public void updateBoardPolicy(BoardPolicyUpdateCommand command) {
        int normalizedThreshold = resolveFeaturedThreshold(command);
        kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode normalizedDisplayMode = resolveThumbnailDisplayMode(command);

        boardPolicyCommandService.updateFeaturedLikeThreshold(normalizedThreshold);
        boardPolicyCommandService.updateThumbnailDisplayMode(normalizedDisplayMode);
        boardAuditPublisher.boardPolicyUpdate(command.actorAccountId(), normalizedThreshold, normalizedDisplayMode.name());
    }

    private int resolveFeaturedThreshold(BoardPolicyUpdateCommand command) {
        return command.featuredLikeThreshold()
                .map(this::normalizeFeaturedThreshold)
                .orElseGet(boardPolicyQueryService::getFeaturedLikeThreshold);
    }

    private kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode resolveThumbnailDisplayMode(BoardPolicyUpdateCommand command) {
        return command.thumbnailDisplayMode()
                .orElseGet(boardPolicyQueryService::getThumbnailDisplayMode);
    }

    private int normalizeFeaturedThreshold(int featuredLikeThreshold) {
        if (featuredLikeThreshold < MIN_FEATURED_THRESHOLD) {
            return MIN_FEATURED_THRESHOLD;
        }
        return Math.min(featuredLikeThreshold, MAX_FEATURED_THRESHOLD);
    }

    @Getter
    public static final class PolicyViewData {

        private final BoardPolicyResponse policy;
        private final int minFeaturedThreshold;
        private final int maxFeaturedThreshold;

        public PolicyViewData(BoardPolicyResponse policy,
                              int minFeaturedThreshold,
                              int maxFeaturedThreshold) {
            this.policy = Objects.requireNonNull(policy, "policy");
            this.minFeaturedThreshold = minFeaturedThreshold;
            this.maxFeaturedThreshold = maxFeaturedThreshold;
        }

        public BoardPolicyResponse policy() {
            return policy;
        }

        public int minFeaturedThreshold() {
            return minFeaturedThreshold;
        }

        public int maxFeaturedThreshold() {
            return maxFeaturedThreshold;
        }

    }
}

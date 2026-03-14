package kwh.PublicCookedFood.board.service.command;

import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;

import java.util.Objects;
import java.util.Optional;

public interface BoardPolicyUpdateCommand {

    long actorAccountId();

    Optional<Integer> featuredLikeThreshold();

    Optional<BoardThumbnailDisplayMode> thumbnailDisplayMode();

    static BoardPolicyUpdateCommand fullUpdate(int featuredLikeThreshold,
                                               BoardThumbnailDisplayMode thumbnailDisplayMode,
                                               Long actorAccountId) {
        return new FullUpdate(featuredLikeThreshold, thumbnailDisplayMode, requireActorAccountId(actorAccountId));
    }

    static BoardPolicyUpdateCommand featuredThreshold(int featuredLikeThreshold,
                                                      Long actorAccountId) {
        return new FeaturedThresholdUpdate(featuredLikeThreshold, requireActorAccountId(actorAccountId));
    }

    static BoardPolicyUpdateCommand thumbnailDisplayMode(BoardThumbnailDisplayMode thumbnailDisplayMode,
                                                         Long actorAccountId) {
        return new ThumbnailDisplayModeUpdate(thumbnailDisplayMode, requireActorAccountId(actorAccountId));
    }

    static BoardPolicyUpdateCommand fromForm(Integer featuredLikeThreshold,
                                             BoardThumbnailDisplayMode thumbnailDisplayMode,
                                             Long actorAccountId) {
        long resolvedActorAccountId = requireActorAccountId(actorAccountId);
        if (featuredLikeThreshold != null && thumbnailDisplayMode != null) {
            return new FullUpdate(featuredLikeThreshold, thumbnailDisplayMode, resolvedActorAccountId);
        }
        if (featuredLikeThreshold != null) {
            return new FeaturedThresholdUpdate(featuredLikeThreshold, resolvedActorAccountId);
        }
        if (thumbnailDisplayMode != null) {
            return new ThumbnailDisplayModeUpdate(thumbnailDisplayMode, resolvedActorAccountId);
        }
        throw new IllegalArgumentException("변경할 게시판 정책이 없습니다.");
    }

    static long requireActorAccountId(Long actorAccountId) {
        long resolvedActorAccountId = Objects.requireNonNull(actorAccountId, "유효하지 않은 사용자입니다.");
        if (resolvedActorAccountId <= 0) {
            throw new IllegalArgumentException("유효하지 않은 사용자입니다.");
        }
        return resolvedActorAccountId;
    }

    final class FullUpdate implements BoardPolicyUpdateCommand {

        private final int featuredLikeThreshold;
        private final BoardThumbnailDisplayMode thumbnailDisplayMode;
        private final long actorAccountId;

        public FullUpdate(int featuredLikeThreshold,
                          BoardThumbnailDisplayMode thumbnailDisplayMode,
                          long actorAccountId) {
            if (thumbnailDisplayMode == null) {
                throw new IllegalArgumentException("썸네일 표시 방식이 비어 있습니다.");
            }
            this.featuredLikeThreshold = featuredLikeThreshold;
            this.thumbnailDisplayMode = thumbnailDisplayMode;
            this.actorAccountId = actorAccountId;
        }

        @Override
        public long actorAccountId() {
            return actorAccountId;
        }

        @Override
        public Optional<Integer> featuredLikeThreshold() {
            return Optional.of(featuredLikeThreshold);
        }

        @Override
        public Optional<BoardThumbnailDisplayMode> thumbnailDisplayMode() {
            return Optional.of(thumbnailDisplayMode);
        }

        public int getFeaturedLikeThreshold() {
            return featuredLikeThreshold;
        }

        public BoardThumbnailDisplayMode getThumbnailDisplayMode() {
            return thumbnailDisplayMode;
        }

        public long getActorAccountId() {
            return actorAccountId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof FullUpdate)) {
                return false;
            }
            FullUpdate that = (FullUpdate) other;
            return featuredLikeThreshold == that.featuredLikeThreshold
                    && actorAccountId == that.actorAccountId
                    && thumbnailDisplayMode == that.thumbnailDisplayMode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(featuredLikeThreshold, thumbnailDisplayMode, actorAccountId);
        }
    }

    final class FeaturedThresholdUpdate implements BoardPolicyUpdateCommand {

        private final int featuredLikeThreshold;
        private final long actorAccountId;

        public FeaturedThresholdUpdate(int featuredLikeThreshold,
                                       long actorAccountId) {
            this.featuredLikeThreshold = featuredLikeThreshold;
            this.actorAccountId = actorAccountId;
        }

        @Override
        public long actorAccountId() {
            return actorAccountId;
        }

        @Override
        public Optional<Integer> featuredLikeThreshold() {
            return Optional.of(featuredLikeThreshold);
        }

        @Override
        public Optional<BoardThumbnailDisplayMode> thumbnailDisplayMode() {
            return Optional.empty();
        }

        public int getFeaturedLikeThreshold() {
            return featuredLikeThreshold;
        }

        public long getActorAccountId() {
            return actorAccountId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof FeaturedThresholdUpdate)) {
                return false;
            }
            FeaturedThresholdUpdate that = (FeaturedThresholdUpdate) other;
            return featuredLikeThreshold == that.featuredLikeThreshold
                    && actorAccountId == that.actorAccountId;
        }

        @Override
        public int hashCode() {
            return Objects.hash(featuredLikeThreshold, actorAccountId);
        }
    }

    final class ThumbnailDisplayModeUpdate implements BoardPolicyUpdateCommand {

        private final BoardThumbnailDisplayMode thumbnailDisplayMode;
        private final long actorAccountId;

        public ThumbnailDisplayModeUpdate(BoardThumbnailDisplayMode thumbnailDisplayMode,
                                          long actorAccountId) {
            if (thumbnailDisplayMode == null) {
                throw new IllegalArgumentException("썸네일 표시 방식이 비어 있습니다.");
            }
            this.thumbnailDisplayMode = thumbnailDisplayMode;
            this.actorAccountId = actorAccountId;
        }

        @Override
        public long actorAccountId() {
            return actorAccountId;
        }

        @Override
        public Optional<Integer> featuredLikeThreshold() {
            return Optional.empty();
        }

        @Override
        public Optional<BoardThumbnailDisplayMode> thumbnailDisplayMode() {
            return Optional.of(thumbnailDisplayMode);
        }

        public BoardThumbnailDisplayMode getThumbnailDisplayMode() {
            return thumbnailDisplayMode;
        }

        public long getActorAccountId() {
            return actorAccountId;
        }

        @Override
        public boolean equals(Object other) {
            if (this == other) {
                return true;
            }
            if (!(other instanceof ThumbnailDisplayModeUpdate)) {
                return false;
            }
            ThumbnailDisplayModeUpdate that = (ThumbnailDisplayModeUpdate) other;
            return actorAccountId == that.actorAccountId
                    && thumbnailDisplayMode == that.thumbnailDisplayMode;
        }

        @Override
        public int hashCode() {
            return Objects.hash(thumbnailDisplayMode, actorAccountId);
        }
    }
}

package kwh.PublicCookedFood.board.service;

import java.util.Optional;

public interface LikeSaveResult {

    static Created created(Long likeId) {
        return new Created(requireLikeId(likeId));
    }

    static AlreadyExists alreadyExists() {
        return AlreadyExists.INSTANCE;
    }

    static Recovered recovered(Long likeId) {
        return new Recovered(requireLikeId(likeId));
    }

    default boolean created() {
        return this instanceof Created;
    }

    default boolean alreadyExisted() {
        return this instanceof AlreadyExists;
    }

    default Optional<Long> likeId() {
        if (this instanceof Created created) {
            return Optional.of(created.value());
        }
        if (this instanceof Recovered recovered) {
            return Optional.of(recovered.value());
        }
        return Optional.empty();
    }

    static Long requireLikeId(Long likeId) {
        if (likeId == null) {
            throw new IllegalArgumentException("좋아요 ID가 필요합니다.");
        }
        return likeId;
    }

    final class Created implements LikeSaveResult {

        private final Long value;

        private Created(Long value) {
            this.value = LikeSaveResult.requireLikeId(value);
        }

        public Long value() {
            return value;
        }
    }

    final class AlreadyExists implements LikeSaveResult {
        private static final AlreadyExists INSTANCE = new AlreadyExists();

        private AlreadyExists() {
        }
    }

    final class Recovered implements LikeSaveResult {

        private final Long value;

        private Recovered(Long value) {
            this.value = LikeSaveResult.requireLikeId(value);
        }

        public Long value() {
            return value;
        }
    }
}

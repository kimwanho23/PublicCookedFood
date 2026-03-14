package kwh.PublicCookedFood.board.service.comment;

public interface VisibleRootCommentSelection {

    boolean includes(long visibleRootIndex);

    boolean isSatisfiedAfter(long visibleRootCount);

    static VisibleRootCommentSelection none() {
        return None.INSTANCE;
    }

    static VisibleRootCommentSelection all() {
        return All.INSTANCE;
    }

    static VisibleRootCommentSelection range(long startInclusive, long endExclusive) {
        return new Range(startInclusive, endExclusive);
    }

    final class None implements VisibleRootCommentSelection {
        private static final None INSTANCE = new None();

        private None() {
        }

        @Override
        public boolean includes(long visibleRootIndex) {
            return false;
        }

        @Override
        public boolean isSatisfiedAfter(long visibleRootCount) {
            return true;
        }
    }

    final class All implements VisibleRootCommentSelection {
        private static final All INSTANCE = new All();

        private All() {
        }

        @Override
        public boolean includes(long visibleRootIndex) {
            return true;
        }

        @Override
        public boolean isSatisfiedAfter(long visibleRootCount) {
            return false;
        }
    }

    final class Range implements VisibleRootCommentSelection {
        private final long startInclusive;
        private final long endExclusive;

        public Range(long startInclusive, long endExclusive) {
            if (startInclusive < 0L) {
                throw new IllegalArgumentException("startInclusive must not be negative");
            }
            if (endExclusive <= startInclusive) {
                throw new IllegalArgumentException("endExclusive must be greater than startInclusive");
            }
            this.startInclusive = startInclusive;
            this.endExclusive = endExclusive;
        }

        public long startInclusive() {
            return startInclusive;
        }

        public long endExclusive() {
            return endExclusive;
        }

        @Override
        public boolean includes(long visibleRootIndex) {
            return visibleRootIndex >= startInclusive && visibleRootIndex < endExclusive;
        }

        @Override
        public boolean isSatisfiedAfter(long visibleRootCount) {
            return visibleRootCount >= endExclusive;
        }
    }
}

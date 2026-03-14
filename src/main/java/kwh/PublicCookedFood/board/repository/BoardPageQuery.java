package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import org.springframework.data.domain.Pageable;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

public final class BoardPageQuery {

    private final SoftDeleteState state;
    private final Order order;
    private final SearchFilter search;
    private final SectionFilter section;
    private final AuthorFilter author;
    private final VisibilityFilter visibility;
    private final FeaturedThreshold featuredThreshold;
    private final Pageable pageable;

    public BoardPageQuery(SoftDeleteState state,
                          Order order,
                          SearchFilter search,
                          SectionFilter section,
                          AuthorFilter author,
                          VisibilityFilter visibility,
                          FeaturedThreshold featuredThreshold,
                          Pageable pageable) {
        this.state = Objects.requireNonNull(state, "state");
        this.order = Objects.requireNonNull(order, "order");
        this.search = search == null ? SearchFilter.none() : search;
        this.section = section == null ? SectionFilter.all() : section;
        this.author = author == null ? AuthorFilter.any() : author;
        this.visibility = visibility == null ? VisibilityFilter.visibleToAll() : visibility;
        this.featuredThreshold = featuredThreshold == null ? FeaturedThreshold.none() : featuredThreshold;
        this.pageable = Objects.requireNonNull(pageable, "pageable");
    }

    public SoftDeleteState state() {
        return state;
    }

    public Order order() {
        return order;
    }

    public SearchFilter search() {
        return search;
    }

    public SectionFilter section() {
        return section;
    }

    public AuthorFilter author() {
        return author;
    }

    public VisibilityFilter visibility() {
        return visibility;
    }

    public FeaturedThreshold featuredThreshold() {
        return featuredThreshold;
    }

    public Pageable pageable() {
        return pageable;
    }

    public enum Order {
        RECENT,
        STATS_VIEWS,
        STATS_LIKES,
        STATS_COMMENTS,
        FEATURED;

        public boolean usesStatsJoin() {
            return this != RECENT;
        }

        public boolean requiresStatsPredicate() {
            return this == FEATURED;
        }
    }

    public interface SearchFilter {

        static SearchFilter none() {
            return None.INSTANCE;
        }

        static SearchFilter titleContaining(String value) {
            return new TitleContaining(value);
        }

        default boolean present() {
            return searchText().isPresent();
        }

        default Optional<String> searchText() {
            return Optional.empty();
        }

        final class None implements SearchFilter {
            private static final None INSTANCE = new None();

            private None() {
            }
        }

        final class TitleContaining implements SearchFilter {

            private final String value;

            private TitleContaining(String value) {
                if (value == null || value.trim().isEmpty()) {
                    throw new IllegalArgumentException("search value must not be blank");
                }
                this.value = value.trim();
            }

            public String value() {
                return value;
            }

            @Override
            public Optional<String> searchText() {
                return Optional.of(value);
            }
        }
    }

    public interface SectionFilter {

        static SectionFilter all() {
            return All.INSTANCE;
        }

        static SectionFilter selected(String sectionKey) {
            return new Selected(sectionKey);
        }

        default Optional<String> sectionKey() {
            return Optional.empty();
        }

        final class All implements SectionFilter {
            private static final All INSTANCE = new All();

            private All() {
            }
        }

        final class Selected implements SectionFilter {

            private final String value;

            private Selected(String value) {
                if (value == null || value.trim().isEmpty()) {
                    throw new IllegalArgumentException("section key must not be blank");
                }
                this.value = value.trim();
            }

            public String value() {
                return value;
            }

            @Override
            public Optional<String> sectionKey() {
                return Optional.of(value);
            }
        }
    }

    public interface AuthorFilter {

        static AuthorFilter any() {
            return Any.INSTANCE;
        }

        static AuthorFilter byAuthor(long authorId) {
            return new ByAuthor(authorId);
        }

        default Optional<Long> authorId() {
            return Optional.empty();
        }

        final class Any implements AuthorFilter {
            private static final Any INSTANCE = new Any();

            private Any() {
            }
        }

        final class ByAuthor implements AuthorFilter {

            private final long value;

            private ByAuthor(long value) {
                if (value <= 0) {
                    throw new IllegalArgumentException("authorId must be positive");
                }
                this.value = value;
            }

            public long value() {
                return value;
            }

            @Override
            public Optional<Long> authorId() {
                return Optional.of(value);
            }
        }
    }

    public static final class VisibilityFilter {

        private final boolean excludeBlocked;
        private final Collection<Long> blockedAccountIds;

        public VisibilityFilter(boolean excludeBlocked, Collection<Long> blockedAccountIds) {
            List<Long> safeBlockedAccountIds = toImmutableLongs(blockedAccountIds);
            this.excludeBlocked = excludeBlocked || !safeBlockedAccountIds.isEmpty();
            this.blockedAccountIds = safeBlockedAccountIds;
        }

        public static VisibilityFilter visibleToAll() {
            return new VisibilityFilter(false, Collections.<Long>emptyList());
        }

        public boolean excludeBlocked() {
            return excludeBlocked;
        }

        public Collection<Long> blockedAccountIds() {
            return blockedAccountIds;
        }
    }

    public interface FeaturedThreshold {

        static FeaturedThreshold none() {
            return None.INSTANCE;
        }

        static FeaturedThreshold atLeast(long likes) {
            return new MinimumLikes(likes);
        }

        default Optional<Long> minimumLikes() {
            return Optional.empty();
        }

        final class None implements FeaturedThreshold {
            private static final None INSTANCE = new None();

            private None() {
            }
        }

        final class MinimumLikes implements FeaturedThreshold {

            private final long likes;

            private MinimumLikes(long likes) {
                if (likes <= 0) {
                    throw new IllegalArgumentException("featured likes threshold must be positive");
                }
                this.likes = likes;
            }

            public long likes() {
                return likes;
            }

            @Override
            public Optional<Long> minimumLikes() {
                return Optional.of(likes);
            }
        }
    }

    private static List<Long> toImmutableLongs(Collection<Long> blockedAccountIds) {
        if (blockedAccountIds == null || blockedAccountIds.isEmpty()) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(new ArrayList<Long>(blockedAccountIds));
    }
}

package kwh.PublicCookedFood.board.service.query;

import kwh.PublicCookedFood.board.service.support.BoardVisibilityCriteria;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Objects;
import java.util.Optional;

public final class BoardListCriteria {

    private final SearchFilter searchFilter;
    private final SectionFilter sectionFilter;
    private final BoardListOrder order;
    private final boolean featuredPage;
    private final Long authorId;
    private final Pageable pageable;
    private final BoardVisibilityCriteria visibility;

    public BoardListCriteria(SearchFilter searchFilter,
                             SectionFilter sectionFilter,
                             BoardListOrder order,
                             boolean featuredPage,
                             Long authorId,
                             Pageable pageable,
                             BoardVisibilityCriteria visibility) {
        BoardListOrder resolvedOrder = order == null ? BoardListOrder.RECENT : order;
        this.searchFilter = searchFilter == null ? SearchFilter.none() : searchFilter;
        this.sectionFilter = sectionFilter == null ? SectionFilter.all() : sectionFilter;
        this.order = resolvedOrder;
        this.featuredPage = featuredPage;
        this.authorId = normalizePositive(authorId);
        this.pageable = resolvePageable(pageable, featuredPage, resolvedOrder);
        this.visibility = visibility == null ? BoardVisibilityCriteria.of(null) : visibility;
    }

    public static BoardListCriteria forAuthor(Pageable pageable,
                                              Long authorId,
                                              BoardVisibilityCriteria visibility) {
        return new BoardListCriteria(
                SearchFilter.none(),
                SectionFilter.all(),
                BoardListOrder.RECENT,
                false,
                authorId,
                pageable,
                visibility
        );
    }

    public SearchFilter searchFilter() {
        return searchFilter;
    }

    public SectionFilter sectionFilter() {
        return sectionFilter;
    }

    public BoardListOrder order() {
        return order;
    }

    public boolean featuredPage() {
        return featuredPage;
    }

    public Long authorId() {
        return authorId;
    }

    public Pageable pageable() {
        return pageable;
    }

    public BoardVisibilityCriteria visibility() {
        return visibility;
    }

    public boolean hasSearch() {
        return searchFilter.present();
    }

    public Optional<BoardSearchKeyword> searchKeyword() {
        return searchFilter.keyword();
    }

    public Optional<BoardSectionKey> sectionKey() {
        return sectionFilter.sectionKey();
    }

    public String orderByParam() {
        if (featuredPage) {
            return null;
        }
        return order.paramValue();
    }

    public String boardListPath() {
        return featuredPage ? "/boards/featured" : "/boards";
    }

    public Optional<String> searchText() {
        return searchKeyword().map(BoardSearchKeyword::value);
    }

    public Optional<String> sectionKeyValue() {
        return sectionKey().map(BoardSectionKey::value);
    }

    private static Pageable resolvePageable(Pageable pageable, boolean featuredPage, BoardListOrder order) {
        Pageable safePageable = pageable == null ? PageRequest.of(0, 15) : pageable;
        if (featuredPage) {
            return safePageable;
        }
        if (order.usesStatsOrdering()) {
            return PageRequest.of(safePageable.getPageNumber(), safePageable.getPageSize());
        }
        return PageRequest.of(safePageable.getPageNumber(), safePageable.getPageSize(), order.sort());
    }

    private static Long normalizePositive(Long value) {
        if (value == null || value.longValue() <= 0L) {
            return null;
        }
        return value;
    }

    public interface SearchFilter {

        static SearchFilter none() {
            return None.INSTANCE;
        }

        static SearchFilter search(BoardSearchKeyword keyword) {
            return new Keyword(keyword);
        }

        default boolean present() {
            return keyword().isPresent();
        }

        default Optional<BoardSearchKeyword> keyword() {
            return Optional.empty();
        }

        final class None implements SearchFilter {
            private static final None INSTANCE = new None();

            private None() {
            }
        }

        final class Keyword implements SearchFilter {

            private final BoardSearchKeyword value;

            private Keyword(BoardSearchKeyword value) {
                this.value = Objects.requireNonNull(value, "value");
            }

            public BoardSearchKeyword value() {
                return value;
            }

            @Override
            public Optional<BoardSearchKeyword> keyword() {
                return Optional.of(value);
            }
        }
    }

    public interface SectionFilter {

        static SectionFilter all() {
            return All.INSTANCE;
        }

        static SectionFilter section(BoardSectionKey sectionKey) {
            return new Selected(sectionKey);
        }

        default boolean present() {
            return sectionKey().isPresent();
        }

        default Optional<BoardSectionKey> sectionKey() {
            return Optional.empty();
        }

        final class All implements SectionFilter {
            private static final All INSTANCE = new All();

            private All() {
            }
        }

        final class Selected implements SectionFilter {

            private final BoardSectionKey value;

            private Selected(BoardSectionKey value) {
                this.value = Objects.requireNonNull(value, "value");
            }

            public BoardSectionKey value() {
                return value;
            }

            @Override
            public Optional<BoardSectionKey> sectionKey() {
                return Optional.of(value);
            }
        }
    }
}

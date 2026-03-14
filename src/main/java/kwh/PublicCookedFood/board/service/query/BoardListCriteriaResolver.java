package kwh.PublicCookedFood.board.service.query;

import kwh.PublicCookedFood.board.service.support.BoardVisibilityCriteria;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.facade.BoardViewer;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.Objects;
import java.util.Set;

@Component
@RequiredArgsConstructor
public class BoardListCriteriaResolver {

    static final String QUERY_INVALID_MESSAGE = "검색 조건이 유효하지 않아 기본 목록을 표시합니다.";
    static final String SECTION_INVALID_MESSAGE = "선택한 게시판 탭을 찾을 수 없어 기본 목록을 표시합니다.";

    private final BoardSectionQueryService boardSectionQueryService;
    private final AccountBlockService accountBlockService;

    public Resolution resolve(Pageable pageable,
                              String searchText,
                              String sectionKey,
                              String orderBy,
                              boolean featuredPage,
                              boolean hasBindingErrors,
                              BoardViewer viewer) {
        Objects.requireNonNull(viewer, "viewer");
        NormalizedBoardSearchQuery normalizedQuery = NormalizedBoardSearchQuery.from(searchText, sectionKey, orderBy);
        BoardListCriteria.SearchFilter search = normalizedQuery.search();
        BoardListCriteria.SectionFilter section = normalizedQuery.section();
        BoardListOrder order = normalizedQuery.order();
        BoardListQueryNotice queryNotice = BoardListQueryNotice.none();

        if (hasBindingErrors) {
            search = BoardListCriteria.SearchFilter.none();
            section = BoardListCriteria.SectionFilter.all();
            order = BoardListOrder.RECENT;
            queryNotice = BoardListQueryNotice.message(QUERY_INVALID_MESSAGE);
        }

        if (section.present()
                && !boardSectionQueryService.existsActiveSection(requiredSectionKey(section).value())) {
            section = BoardListCriteria.SectionFilter.all();
            queryNotice = BoardListQueryNotice.message(SECTION_INVALID_MESSAGE);
        }

        BoardListCriteria criteria = new BoardListCriteria(
                search,
                section,
                order,
                featuredPage,
                null,
                pageable,
                BoardVisibilityCriteria.of(resolveRestrictedAccountIds(viewer))
        );
        return new Resolution(criteria, queryNotice);
    }

    private Set<Long> resolveRestrictedAccountIds(BoardViewer viewer) {
        return viewer.maybeAccountId()
                .map(accountBlockService::getViewRestrictedAccountIds)
                .orElseGet(Collections::emptySet);
    }

    private BoardSectionKey requiredSectionKey(BoardListCriteria.SectionFilter section) {
        return section.sectionKey()
                .orElseThrow(() -> new IllegalStateException("section filter must contain sectionKey"));
    }

    public static final class Resolution {

        private final BoardListCriteria criteria;
        private final BoardListQueryNotice queryNotice;

        public Resolution(BoardListCriteria criteria, BoardListQueryNotice queryNotice) {
            this.criteria = criteria;
            this.queryNotice = queryNotice;
        }

        public BoardListCriteria criteria() {
            return criteria;
        }

        public BoardListQueryNotice queryNotice() {
            return queryNotice;
        }
    }

    private static final class NormalizedBoardSearchQuery {

        private final BoardListCriteria.SearchFilter search;
        private final BoardListCriteria.SectionFilter section;
        private final BoardListOrder order;

        private NormalizedBoardSearchQuery(BoardListCriteria.SearchFilter search,
                                           BoardListCriteria.SectionFilter section,
                                           BoardListOrder order) {
            this.search = search;
            this.section = section;
            this.order = order;
        }

        static NormalizedBoardSearchQuery from(String searchText,
                                               String sectionKey,
                                               String orderBy) {
            return new NormalizedBoardSearchQuery(
                    BoardSearchKeyword.from(searchText)
                            .<BoardListCriteria.SearchFilter>map(BoardListCriteria.SearchFilter::search)
                            .orElseGet(BoardListCriteria.SearchFilter::none),
                    BoardSectionKey.from(sectionKey)
                            .<BoardListCriteria.SectionFilter>map(BoardListCriteria.SectionFilter::section)
                            .orElseGet(BoardListCriteria.SectionFilter::all),
                    BoardListOrder.from(normalizeBlank(orderBy))
            );
        }

        BoardListCriteria.SearchFilter search() {
            return search;
        }

        BoardListCriteria.SectionFilter section() {
            return section;
        }

        BoardListOrder order() {
            return order;
        }

        private static String normalizeBlank(String value) {
            if (value == null) {
                return null;
            }
            String trimmed = value.trim();
            if (trimmed.isEmpty()) {
                return null;
            }
            return trimmed;
        }
    }
}

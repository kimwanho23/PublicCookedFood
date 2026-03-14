package kwh.PublicCookedFood.board.service.query;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardPageQuery;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.service.support.BoardVisibilityCriteria;
import kwh.PublicCookedFood.metrics.popular.BoardPopularSnapshotService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.Collection;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardListQueryService {

    private final BoardRepository boardRepository;
    private final BoardPolicyQueryService boardPolicyQueryService;
    private final BoardPopularSnapshotService boardPopularSnapshotService;

    public Page<Board> load(BoardListCriteria criteria) {
        Objects.requireNonNull(criteria, "게시글 목록 조회 조건이 비어 있습니다.");
        if (criteria.featuredPage()) {
            return loadFeatured(criteria);
        }
        if (criteria.order().usesStatsOrdering()) {
            return loadByStatsOrder(criteria);
        }
        return loadRecent(criteria);
    }

    private Page<Board> loadRecent(BoardListCriteria criteria) {
        return boardRepository.findPageWithAccount(toPageQuery(criteria, BoardPageQuery.Order.RECENT, criteria.pageable()));
    }

    private Page<Board> loadByStatsOrder(BoardListCriteria criteria) {
        BoardPageQuery query = toPageQuery(criteria, toStatsOrder(criteria.order()), criteria.pageable());
        try {
            return boardRepository.findPageWithAccount(query);
        } catch (DataAccessException e) {
            log.warn("Failed to query board list ordered by stats; falling back to recent ordering. order={}, sectionKey={}, authorId={}",
                    criteria.order(), criteria.sectionKeyValue().orElse(null), criteria.authorId(), e);
            Pageable fallbackPageable = withFallbackSort(criteria.pageable());
            return boardRepository.findPageWithAccount(toPageQuery(criteria, BoardPageQuery.Order.RECENT, fallbackPageable));
        }
    }

    private Page<Board> loadFeatured(BoardListCriteria criteria) {
        BoardVisibilityCriteria visibility = criteria.visibility();
        String sectionKey = criteria.sectionKeyValue().orElse(null);
        if (!criteria.hasSearch() && canServeFeaturedFromSnapshot(criteria.authorId(), visibility.restrictedAccountIds())) {
            Optional<Page<Board>> snapshotPage =
                    boardPopularSnapshotService.loadFeaturedRankingPage(criteria.pageable(), sectionKey);
            if (snapshotPage.isPresent()) {
                return snapshotPage.get();
            }
        }

        long threshold = boardPolicyQueryService.getFeaturedLikeThreshold();
        return boardRepository.findPageWithAccount(toFeaturedQuery(criteria, threshold));
    }

    private boolean canServeFeaturedFromSnapshot(Long authorId, Collection<Long> blockedAccountIds) {
        if (authorId != null) {
            return false;
        }
        return blockedAccountIds == null || blockedAccountIds.isEmpty();
    }

    private Pageable withFallbackSort(Pageable pageable) {
        int pageNumber = pageable == null ? 0 : pageable.getPageNumber();
        int pageSize = pageable == null ? 15 : pageable.getPageSize();
        Sort fallbackSort = Sort.by(
                Sort.Order.desc("regTime")
        );
        return PageRequest.of(pageNumber, pageSize, fallbackSort);
    }

    private BoardPageQuery toPageQuery(BoardListCriteria criteria,
                                       BoardPageQuery.Order order,
                                       Pageable pageable) {
        return new BoardPageQuery(
                SoftDeleteState.ACTIVE,
                order,
                criteria.searchKeyword()
                        .<BoardPageQuery.SearchFilter>map(keyword -> BoardPageQuery.SearchFilter.titleContaining(keyword.value()))
                        .orElseGet(BoardPageQuery.SearchFilter::none),
                criteria.sectionKey()
                        .<BoardPageQuery.SectionFilter>map(sectionKey -> BoardPageQuery.SectionFilter.selected(sectionKey.value()))
                        .orElseGet(BoardPageQuery.SectionFilter::all),
                criteria.authorId() == null
                        ? BoardPageQuery.AuthorFilter.any()
                        : BoardPageQuery.AuthorFilter.byAuthor(criteria.authorId()),
                new BoardPageQuery.VisibilityFilter(
                        criteria.visibility().excludeRestricted(),
                        criteria.visibility().restrictedAccountIds()
                ),
                BoardPageQuery.FeaturedThreshold.none(),
                pageable
        );
    }

    private BoardPageQuery toFeaturedQuery(BoardListCriteria criteria, long threshold) {
        BoardPageQuery baseQuery = toPageQuery(criteria, BoardPageQuery.Order.FEATURED, criteria.pageable());
        return new BoardPageQuery(
                baseQuery.state(),
                baseQuery.order(),
                baseQuery.search(),
                baseQuery.section(),
                baseQuery.author(),
                baseQuery.visibility(),
                BoardPageQuery.FeaturedThreshold.atLeast(threshold),
                baseQuery.pageable()
        );
    }

    private BoardPageQuery.Order toStatsOrder(BoardListOrder order) {
        if (order == BoardListOrder.VIEWS) {
            return BoardPageQuery.Order.STATS_VIEWS;
        }
        if (order == BoardListOrder.LIKES) {
            return BoardPageQuery.Order.STATS_LIKES;
        }
        if (order == BoardListOrder.COMMENTS) {
            return BoardPageQuery.Order.STATS_COMMENTS;
        }
        throw new IllegalStateException("stats ordering is not supported for recent");
    }
}

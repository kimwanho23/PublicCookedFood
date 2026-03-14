package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.application.query.BoardCardViewAssembler;
import kwh.PublicCookedFood.board.application.query.view.BoardCardView;
import kwh.PublicCookedFood.board.application.query.view.BoardListPageView;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.dto.request.BoardSearchQuery;
import kwh.PublicCookedFood.board.dto.response.BoardSectionView;
import kwh.PublicCookedFood.board.service.query.BoardListCriteria;
import kwh.PublicCookedFood.board.service.query.BoardListCriteriaResolver;
import kwh.PublicCookedFood.board.service.query.BoardListQueryService;
import kwh.PublicCookedFood.board.service.query.BoardPolicyQueryService;
import kwh.PublicCookedFood.board.service.query.BoardSectionQueryService;
import kwh.PublicCookedFood.board.service.support.BoardThumbnailExtractor;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummary;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummaryResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class BoardListQueryFacade {

    private final BoardListCriteriaResolver boardListCriteriaResolver;
    private final BoardThumbnailExtractor boardThumbnailExtractor;
    private final BoardStatsSummaryResolver boardStatsSummaryResolver;
    private final BoardSectionQueryService boardSectionQueryService;
    private final BoardPolicyQueryService boardPolicyQueryService;
    private final BoardListQueryService boardListQueryService;
    private final BoardCardViewAssembler boardCardViewAssembler;

    public BoardListPageView loadBoardList(Pageable pageable,
                                           BoardSearchQuery query,
                                           boolean featuredPage,
                                           boolean hasBindingErrors,
                                           BoardViewer viewer) {
        BoardListCriteriaResolver.Resolution resolution = boardListCriteriaResolver.resolve(
                pageable,
                query == null ? null : query.getSearch(),
                query == null ? null : query.getSection(),
                query == null ? null : query.getOrderBy(),
                featuredPage,
                hasBindingErrors,
                viewer
        );
        BoardListCriteria criteria = resolution.criteria();
        Page<Board> boardList = boardListQueryService.load(criteria);
        BoardThumbnailExtractor.ThumbnailData thumbnailData = boardThumbnailExtractor.extract(boardList.getContent());
        Map<Long, BoardStatsSummary> boardStatsMap = boardStatsSummaryResolver.resolve(boardList.getContent());
        int featuredLikeThreshold = boardPolicyQueryService.getFeaturedLikeThreshold();

        return new BoardListPageView(
                boardCardViewAssembler.toPage(boardList, boardStatsMap, thumbnailData, featuredLikeThreshold),
                criteria.pageable(),
                criteria.searchText().orElse(null),
                criteria.sectionKeyValue().orElse(null),
                criteria.orderByParam(),
                boardPolicyQueryService.getThumbnailDisplayMode().name(),
                boardSectionQueryService.getActiveSections().stream()
                        .map(BoardSectionView::from)
                        .toList(),
                criteria.boardListPath(),
                criteria.featuredPage(),
                featuredLikeThreshold,
                criteria.featuredPage() ? "\uCD94\uCC9C \uAC8C\uC2DC\uBB3C" : "\uAC8C\uC2DC\uD310",
                resolution.queryNotice()
        );
    }

}

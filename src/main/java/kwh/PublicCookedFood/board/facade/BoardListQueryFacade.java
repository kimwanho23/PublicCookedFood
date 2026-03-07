package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.service.BoardSectionService;
import kwh.PublicCookedFood.board.service.BoardService;
import kwh.PublicCookedFood.common.dto.request.BoardSearchQuery;
import kwh.PublicCookedFood.metrics.view.ViewCounterService;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import lombok.RequiredArgsConstructor;
import org.jsoup.Jsoup;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BoardListQueryFacade {

    private static final String QUERY_INVALID_MESSAGE = "검색 조건이 유효하지 않아 기본 목록을 표시합니다.";
    private static final String SECTION_INVALID_MESSAGE = "선택한 게시판 탭을 찾을 수 없어 기본 목록을 표시합니다.";

    private final BoardService boardService;
    private final BoardSectionService boardSectionService;
    private final AccountBlockService accountBlockService;
    private final ViewCounterService viewCounterService;

    public BoardFacade.BoardListViewData loadBoardList(Pageable pageable,
                                                       BoardSearchQuery query,
                                                       boolean featuredPage,
                                                       boolean hasBindingErrors,
                                                       Long viewerAccountId) {
        String search = query == null ? null : query.normalizedSearch();
        String section = query == null ? null : query.normalizedSection();
        String orderBy = normalizeOrderBy(query == null ? null : query.normalizedOrderBy(), featuredPage);
        String queryErrorMsg = null;

        if (hasBindingErrors) {
            search = null;
            section = null;
            orderBy = null;
            queryErrorMsg = QUERY_INVALID_MESSAGE;
        }

        if (section != null && !boardSectionService.existsActiveSection(section)) {
            section = null;
            queryErrorMsg = SECTION_INVALID_MESSAGE;
        }

        Pageable listPageable = resolveListPageable(pageable, featuredPage, orderBy);
        Set<Long> blockedAccountIds = accountBlockService.getViewRestrictedAccountIds(viewerAccountId);
        Page<Board> boardList = getBoardInfo(listPageable, search, section, featuredPage, orderBy, blockedAccountIds);

        BoardThumbnailData thumbnailData = extractBoardThumbnailData(boardList.getContent());
        Map<Long, Long> boardViewMap = resolveBoardViewMap(boardList.getContent());
        String boardListPath = featuredPage ? "/boards/featured" : "/boards";

        return new BoardFacade.BoardListViewData(
                boardList,
                listPageable,
                search,
                section,
                orderBy,
                boardViewMap,
                thumbnailData.thumbnailUrlByBoardId(),
                thumbnailData.hasImageByBoardId(),
                boardService.getThumbnailDisplayMode().name(),
                boardSectionService.getActiveSections(),
                boardListPath,
                featuredPage,
                boardService.getFeaturedLikeThreshold(),
                featuredPage ? "\uCD94\uCC9C \uAC8C\uC2DC\uBB3C" : "\uAC8C\uC2DC\uD310",
                queryErrorMsg
        );
    }

    private Page<Board> getBoardInfo(Pageable pageable,
                                     String search,
                                     String section,
                                     boolean featuredPage,
                                     String orderBy,
                                     Set<Long> blockedAccountIds) {
        boolean hasSearch = search != null && !search.isEmpty();
        if (featuredPage) {
            return hasSearch
                    ? boardService.findFeaturedByKeyword(search, section, null, blockedAccountIds, pageable)
                    : boardService.getFeaturedBoardList(pageable, section, null, blockedAccountIds);
        }

        if ("views".equals(orderBy)) {
            return hasSearch
                    ? boardService.findByKeywordOrderByViews(search, section, null, blockedAccountIds, pageable)
                    : boardService.getBoardListOrderByViews(pageable, section, null, blockedAccountIds);
        }

        return hasSearch
                ? boardService.findByKeyword(search, section, null, blockedAccountIds, pageable)
                : boardService.getBoardList(pageable, section, null, blockedAccountIds);
    }

    private Pageable resolveListPageable(Pageable pageable, boolean featuredPage, String orderBy) {
        if (featuredPage) {
            return pageable;
        }
        String normalizedOrderBy = (orderBy == null || orderBy.isBlank()) ? "recent" : orderBy;
        if ("views".equals(normalizedOrderBy)) {
            return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize());
        }
        Sort sort = switch (normalizedOrderBy) {
            case "likes" -> Sort.by(Sort.Order.desc("likeCount"));
            case "comments" -> Sort.by(Sort.Order.desc("commentCount"));
            default -> Sort.by(Sort.Order.desc("regTime"));
        };
        return PageRequest.of(pageable.getPageNumber(), pageable.getPageSize(), sort);
    }

    private String normalizeOrderBy(String orderBy, boolean featuredPage) {
        if (featuredPage || orderBy == null) {
            return null;
        }
        return switch (orderBy) {
            case "views", "likes", "comments" -> orderBy;
            default -> null;
        };
    }

    private BoardThumbnailData extractBoardThumbnailData(Iterable<Board> boards) {
        if (boards == null) {
            return new BoardThumbnailData(Map.of(), Map.of());
        }

        Map<Long, String> thumbnailUrlByBoardId = new HashMap<>();
        Map<Long, Boolean> hasImageByBoardId = new HashMap<>();

        for (Board board : boards) {
            if (board == null || board.getId() == null) {
                continue;
            }
            String thumbnailUrl = extractFirstImageUrl(board.getContents());
            if (thumbnailUrl != null) {
                thumbnailUrlByBoardId.put(board.getId(), thumbnailUrl);
                hasImageByBoardId.put(board.getId(), true);
            } else {
                hasImageByBoardId.put(board.getId(), false);
            }
        }

        return new BoardThumbnailData(thumbnailUrlByBoardId, hasImageByBoardId);
    }

    private String extractFirstImageUrl(String htmlContent) {
        if (htmlContent == null || htmlContent.isBlank()) {
            return null;
        }
        String src = Jsoup.parse(htmlContent)
                .select("img[src]")
                .stream()
                .map(element -> element.attr("src"))
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .filter(value -> !value.startsWith("data:"))
                .findFirst()
                .orElse(null);
        return src == null || src.isBlank() ? null : src;
    }

    private Map<Long, Long> resolveBoardViewMap(Iterable<Board> boards) {
        if (boards == null) {
            return Map.of();
        }

        Set<Long> boardIds = new LinkedHashSet<>();
        for (Board board : boards) {
            if (board == null || board.getId() == null) {
                continue;
            }
            boardIds.add(board.getId());
        }
        if (boardIds.isEmpty()) {
            return Map.of();
        }
        return viewCounterService.getBoardViewCounts(boardIds);
    }

    private record BoardThumbnailData(Map<Long, String> thumbnailUrlByBoardId,
                                      Map<Long, Boolean> hasImageByBoardId) {
    }
}

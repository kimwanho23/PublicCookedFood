package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.application.query.BoardCardViewAssembler;
import kwh.PublicCookedFood.board.application.query.view.BoardCardView;
import kwh.PublicCookedFood.board.application.query.view.BoardScrapListView;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.service.BoardScrapService;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummary;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummaryResolver;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Map;
import java.util.Set;

@Service
@RequiredArgsConstructor
public class BoardScrapQueryFacade {

    private final BoardScrapService boardScrapService;
    private final AccountBlockService accountBlockService;
    private final BoardStatsSummaryResolver boardStatsSummaryResolver;
    private final BoardCardViewAssembler boardCardViewAssembler;

    public BoardScrapListView loadMyScrappedBoards(Long accountId) {
        Set<Long> blockedAccountIds = accountBlockService.getViewRestrictedAccountIds(accountId);
        List<Board> scrappedBoards = boardScrapService.getMyScrappedBoards(accountId, blockedAccountIds);
        Map<Long, BoardStatsSummary> boardStatsMap = boardStatsSummaryResolver.resolve(scrappedBoards);
        List<BoardCardView> boardCards = boardCardViewAssembler.toList(scrappedBoards, boardStatsMap);
        return new BoardScrapListView(boardCards);
    }
}

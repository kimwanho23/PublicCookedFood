package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.board.application.query.BoardCardViewAssembler;
import kwh.PublicCookedFood.board.application.query.view.BoardCardView;
import kwh.PublicCookedFood.board.application.query.view.BoardScrapListView;
import kwh.PublicCookedFood.account.service.AccountBlockService;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.service.BoardScrapService;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummary;
import kwh.PublicCookedFood.board.service.support.BoardStatsSummaryResolver;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardScrapQueryFacadeUnitTest {

    @Mock
    private BoardScrapService boardScrapService;

    @Mock
    private AccountBlockService accountBlockService;

    @Mock
    private BoardStatsSummaryResolver boardStatsSummaryResolver;

    private BoardScrapQueryFacade boardScrapQueryFacade;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        boardScrapQueryFacade = new BoardScrapQueryFacade(
                boardScrapService,
                accountBlockService,
                boardStatsSummaryResolver,
                new BoardCardViewAssembler()
        );
    }

    @Test
    void loadMyScrappedBoards_wrapsBoardsWithStatsSummary() {
        Board board = Board.builder().id(7L).build();

        when(accountBlockService.getViewRestrictedAccountIds(3L)).thenReturn(Set.of());
        when(boardScrapService.getMyScrappedBoards(3L, Set.of())).thenReturn(java.util.List.of(board));
        when(boardStatsSummaryResolver.resolve(java.util.List.of(board)))
                .thenReturn(java.util.Map.of(7L, new BoardStatsSummary(10L, 20L, 30L)));

        BoardScrapListView viewData = boardScrapQueryFacade.loadMyScrappedBoards(3L);

        BoardCardView card = viewData.boards().get(0);
        assertThat(card.boardId()).isEqualTo(7L);
        assertThat(card.views()).isEqualTo(10L);
        assertThat(card.likes()).isEqualTo(20L);
        assertThat(card.commentsCount()).isEqualTo(30L);
    }
}

package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.account.audit.BoardAuditPublisher;
import kwh.PublicCookedFood.board.domain.BoardThumbnailDisplayMode;
import kwh.PublicCookedFood.board.service.command.BoardPolicyCommandService;
import kwh.PublicCookedFood.board.service.query.BoardPolicyQueryService;
import kwh.PublicCookedFood.board.service.command.BoardPolicyUpdateCommand;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardAdminPolicyFacadeUnitTest {

    @Mock
    private BoardPolicyQueryService boardPolicyQueryService;

    @Mock
    private BoardPolicyCommandService boardPolicyCommandService;

    @Mock
    private BoardAuditPublisher boardAuditPublisher;

    private BoardAdminPolicyFacade boardAdminPolicyFacade;

    @BeforeEach
    void setUp() {
        boardAdminPolicyFacade = new BoardAdminPolicyFacade(
                boardPolicyQueryService,
                boardPolicyCommandService,
                boardAuditPublisher
        );
    }

    @Test
    void updateBoardPolicy_fillsMissingFieldsFromCurrentPolicy() {
        when(boardPolicyQueryService.getFeaturedLikeThreshold()).thenReturn(7);

        boardAdminPolicyFacade.updateBoardPolicy(
                BoardPolicyUpdateCommand.fromForm(null, BoardThumbnailDisplayMode.HOVER, 99L)
        );

        verify(boardPolicyCommandService).updateFeaturedLikeThreshold(7);
        verify(boardPolicyCommandService).updateThumbnailDisplayMode(BoardThumbnailDisplayMode.HOVER);
        verify(boardAuditPublisher).boardPolicyUpdate(99L, 7, BoardThumbnailDisplayMode.HOVER.name());
    }
}

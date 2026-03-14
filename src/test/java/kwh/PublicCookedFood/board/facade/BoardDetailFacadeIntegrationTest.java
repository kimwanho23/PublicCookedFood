package kwh.PublicCookedFood.board.facade;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardReportReason;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.repository.BoardReportRepository;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.service.command.BoardSectionCommandService;
import kwh.PublicCookedFood.board.service.command.BoardReportCreateCommand;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BoardDetailFacadeIntegrationTest {

    @Autowired
    private BoardDetailFacade boardDetailFacade;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardReportRepository boardReportRepository;

    @Autowired
    private BoardSectionCommandService boardSectionCommandService;

    @AfterEach
    void cleanUp() {
        boardReportRepository.deleteAll();
        boardRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void reportBoard_returnsFailureResultWhenTransactionIsMarkedRollbackOnly() {
        Board board = createBoard();

        BoardDetailFacade.OperationResult result = boardDetailFacade.reportBoard(
                new BoardReportCreateCommand(
                        board.getId(),
                        board.getAccount().getId(),
                        BoardReportReason.SPAM,
                        null
                )
        );

        assertThat(result.success()).isFalse();
        assertThat(result.requiredMessage()).contains("본인 게시글은 신고할 수 없습니다.");
        assertThat(boardReportRepository.countByBoardId(board.getId())).isZero();
    }

    private Board createBoard() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Account account = accountRepository.saveAndFlush(Account.builder()
                .email("board-detail-" + suffix + "@test.com")
                .name("detail-" + suffix)
                .authority(Role.USER)
                .loginMethod("Current")
                .build());
        BoardSection section = boardSectionCommandService.ensureDefaultSection();
        return boardRepository.saveAndFlush(Board.builder()
                .title("before")
                .contents("<p>before</p>")
                .account(account)
                .section(section)
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(false)
                .build());
    }
}

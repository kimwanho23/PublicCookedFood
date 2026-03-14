package kwh.PublicCookedFood.board.service.command;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.common.error.AppException;
import kwh.PublicCookedFood.common.error.CommonErrorCode;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@SpringBootTest
class BoardEditConcurrencyIntegrationTest {

    @Autowired
    private BoardCommandService boardCommandService;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BoardSectionCommandService boardSectionCommandService;

    @Autowired
    private PlatformTransactionManager transactionManager;

    private TransactionTemplate transactionTemplate;

    @BeforeEach
    void setUp() {
        transactionTemplate = new TransactionTemplate(transactionManager);
    }

    @AfterEach
    void cleanUp() {
        boardRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void update_rejectsStaleFormVersionAfterAnotherSave() {
        Board board = createBoard();
        Long submittedVersion = reloadBoard(board.getId()).getVersion();

        boardCommandService.update(new BoardUpdateCommand(
                board.getId(),
                board.getAccount().getId(),
                submittedVersion,
                "first edit",
                "<p>first edit</p>",
                board.getSection().getId()
        ));

        assertThatThrownBy(() -> boardCommandService.update(new BoardUpdateCommand(
                board.getId(),
                board.getAccount().getId(),
                submittedVersion,
                "stale edit",
                "<p>stale edit</p>",
                board.getSection().getId()
        )))
                .isInstanceOf(AppException.class)
                .extracting("errorCode")
                .isEqualTo(CommonErrorCode.REQUEST_CONFLICT);
    }

    @Test
    void concurrentManagedEdits_allowOnlyOneCommit() throws Exception {
        Board board = createBoard();
        Long boardId = board.getId();
        Long initialVersion = reloadBoard(boardId).getVersion();

        ExecutorService executor = Executors.newFixedThreadPool(2);
        CountDownLatch loaded = new CountDownLatch(2);
        CountDownLatch flushStart = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        futures.add(executor.submit(() -> editBoardInSeparateTransaction(boardId, "first", loaded, flushStart)));
        futures.add(executor.submit(() -> editBoardInSeparateTransaction(boardId, "second", loaded, flushStart)));

        assertThat(loaded.await(5, TimeUnit.SECONDS)).isTrue();
        flushStart.countDown();

        int successCount = 0;
        int failureCount = 0;
        Throwable failure = null;
        for (Future<?> future : futures) {
            try {
                future.get(10, TimeUnit.SECONDS);
                successCount++;
            } catch (ExecutionException e) {
                failureCount++;
                failure = e.getCause();
            }
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();
        assertThat(successCount).isEqualTo(1);
        assertThat(failureCount).isEqualTo(1);
        assertThat(hasCauseAssignableTo(failure, OptimisticLockingFailureException.class)).isTrue();

        Board updatedBoard = reloadBoard(boardId);
        assertThat(updatedBoard.getVersion()).isEqualTo(initialVersion + 1);
        assertThat(updatedBoard.getTitle()).isIn("first", "second");
    }

    private void editBoardInSeparateTransaction(Long boardId,
                                                String title,
                                                CountDownLatch loaded,
                                                CountDownLatch flushStart) {
        transactionTemplate.executeWithoutResult(status -> {
            Board managed = boardRepository.findByIdWithAccountAndState(boardId, SoftDeleteState.ACTIVE)
                    .orElseThrow();
            loaded.countDown();
            await(flushStart);
            managed.edit(title, "<p>" + title + "</p>", managed.getSection());
            boardRepository.flush();
        });
    }

    private Board createBoard() {
        String suffix = UUID.randomUUID().toString().substring(0, 8);
        Account account = accountRepository.saveAndFlush(Account.builder()
                .email("board-edit-" + suffix + "@test.com")
                .name("editor-" + suffix)
                .authority(Role.USER)
                .loginMethod("Current")
                .build());
        BoardSection section = boardSectionCommandService.ensureDefaultSection();
        Board board = Board.builder()
                .title("before")
                .contents("<p>before</p>")
                .account(account)
                .section(section)
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(false)
                .build();
        return boardRepository.saveAndFlush(board);
    }

    private Board reloadBoard(Long boardId) {
        return boardRepository.findByIdWithAccountAndState(boardId, SoftDeleteState.ACTIVE)
                .orElseThrow();
    }

    private void await(CountDownLatch latch) {
        try {
            if (!latch.await(5, TimeUnit.SECONDS)) {
                throw new IllegalStateException("동시성 테스트 대기 시간이 초과되었습니다.");
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("동시성 테스트 대기 중 인터럽트가 발생했습니다.", e);
        }
    }

    private boolean hasCauseAssignableTo(Throwable throwable, Class<? extends Throwable> type) {
        Throwable current = throwable;
        while (current != null) {
            if (type.isInstance(current)) {
                return true;
            }
            current = current.getCause();
        }
        return false;
    }
}

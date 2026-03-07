package kwh.PublicCookedFood.board.service;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BoardViewCountConcurrencyIntegrationTest {

    @Autowired
    private BoardService boardService;

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private AccountRepository accountRepository;

    @AfterEach
    void cleanUp() {
        boardRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void updateViews_increasesExactlyAsMuchAsConcurrentRequests() throws Exception {
        Long boardId = createBoardWithZeroViews().getId();

        int requestCount = 200;
        int poolSize = 16;
        ExecutorService executor = Executors.newFixedThreadPool(poolSize);
        CountDownLatch start = new CountDownLatch(1);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < requestCount; i++) {
            futures.add(executor.submit(() -> {
                start.await();
                boardService.updateViews(boardId);
                return null;
            }));
        }

        start.countDown();

        for (Future<?> future : futures) {
            future.get(10, TimeUnit.SECONDS);
        }

        executor.shutdown();
        assertThat(executor.awaitTermination(10, TimeUnit.SECONDS)).isTrue();

        Long actualViews = boardRepository.findById(boardId)
                .orElseThrow()
                .getViews();
        assertThat(actualViews).isEqualTo((long) requestCount);
    }

    @Test
    void updateViews_doesNotIncreaseWhenBoardIsHiddenByReport() {
        Board board = createBoardWithZeroViews();
        board.updateHiddenByReport(true);
        boardRepository.saveAndFlush(board);

        boardService.updateViews(board.getId());

        Long actualViews = boardRepository.findById(board.getId())
                .orElseThrow()
                .getViews();
        assertThat(actualViews).isZero();
    }

    private Board createBoardWithZeroViews() {
        Account account = accountRepository.saveAndFlush(Account.builder()
                .email("views-" + UUID.randomUUID() + "@test.com")
                .name("view-tester")
                .authority(Role.USER)
                .loginMethod("Current")
                .build());

        Board board = Board.builder()
                .title("view count test")
                .contents("content")
                .account(account)
                .views(0L)
                .likeCount(0L)
                .commentCount(0L)
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(false)
                .build();

        return boardRepository.saveAndFlush(board);
    }
}

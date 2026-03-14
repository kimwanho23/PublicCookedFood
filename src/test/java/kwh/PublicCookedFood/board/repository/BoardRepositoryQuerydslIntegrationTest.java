package kwh.PublicCookedFood.board.repository;

import kwh.PublicCookedFood.account.domain.Account;
import kwh.PublicCookedFood.account.domain.Role;
import kwh.PublicCookedFood.account.repository.AccountRepository;
import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardSectionRepository;
import kwh.PublicCookedFood.board.domain.BoardStats;
import kwh.PublicCookedFood.board.repository.BoardStatsRepository;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class BoardRepositoryQuerydslIntegrationTest {

    @Autowired
    private BoardRepository boardRepository;

    @Autowired
    private BoardStatsRepository boardStatsRepository;

    @Autowired
    private AccountRepository accountRepository;

    @Autowired
    private BoardSectionRepository boardSectionRepository;

    @AfterEach
    void cleanUp() {
        boardStatsRepository.deleteAll();
        boardRepository.deleteAll();
        boardSectionRepository.deleteAll();
        accountRepository.deleteAll();
    }

    @Test
    void findAllByStateWithAccountOrderByLikeStats_prefersBoardStatsOrdering() {
        Account author = saveAccount("querydsl-order");
        Board boardWithHigherStats = saveBoard(author, "stats-first");
        Board boardWithLowerStats = saveBoard(author, "stats-second");

        saveBoardStats(boardWithHigherStats.getId(), 10L, 3L, 5L);
        saveBoardStats(boardWithLowerStats.getId(), 2L, 8L, 1L);

        Page<Board> result = boardRepository.findPageWithAccount(pageQuery(BoardPageQuery.Order.STATS_LIKES, null, null, null, List.of()));

        assertThat(result.getContent())
                .extracting(Board::getId)
                .containsExactly(boardWithHigherStats.getId(), boardWithLowerStats.getId());
    }

    @Test
    void findAllByStateWithAccountOrderByLikeStats_treatsMissingStatsAsZero() {
        Account author = saveAccount("querydsl-order-missing-stats");
        Board boardWithStats = saveBoard(author, "stats-backed");
        Board boardWithoutStats = saveBoard(author, "no-stats");

        saveBoardStats(boardWithStats.getId(), 5L, 1L, 1L);

        Page<Board> result = boardRepository.findPageWithAccount(pageQuery(BoardPageQuery.Order.STATS_LIKES, null, null, null, List.of()));

        assertThat(result.getContent())
                .extracting(Board::getId)
                .containsSequence(boardWithStats.getId(), boardWithoutStats.getId());
    }

    @Test
    void findFeaturedByStateWithAccount_usesBoardStatsThreshold() {
        Account author = saveAccount("querydsl-featured");
        Board eligibleByStatsOnly = saveBoard(author, "eligible");
        Board excludedByStatsOnly = saveBoard(author, "excluded");

        saveBoardStats(eligibleByStatsOnly.getId(), 7L, 6L, 2L);
        saveBoardStats(excludedByStatsOnly.getId(), 2L, 8L, 4L);

        Page<Board> result = boardRepository.findPageWithAccount(featuredQuery(5L, null, List.of()));

        assertThat(result.getContent())
                .extracting(Board::getId)
                .containsExactly(eligibleByStatsOnly.getId());
    }

    @Test
    void findFeaturedByStateWithAccount_ignoresLegacyLikeCountWithoutStatsRow() {
        Account author = saveAccount("querydsl-featured-legacy");
        Board eligibleByStats = saveBoard(author, "eligible-stats");
        saveBoard(author, "no-stats");

        saveBoardStats(eligibleByStats.getId(), 6L, 1L, 1L);

        Page<Board> result = boardRepository.findPageWithAccount(featuredQuery(5L, null, List.of()));

        assertThat(result.getContent())
                .extracting(Board::getId)
                .containsExactly(eligibleByStats.getId());
    }

    @Test
    void findTopBoardIdsForSnapshot_filtersByThresholdAndSectionKeyUsingStats() {
        Account author = saveAccount("querydsl-snapshot");
        BoardSection free = boardSectionRepository.saveAndFlush(BoardSection.createDefault("free", "자유"));
        BoardSection notice = boardSectionRepository.saveAndFlush(BoardSection.createDefault("notice", "공지"));

        Board included = saveBoard(author, free, "included");
        Board wrongSection = saveBoard(author, notice, "wrong-section");
        Board belowThreshold = saveBoard(author, free, "below-threshold");

        saveBoardStats(included.getId(), 8L, 2L, 4L);
        saveBoardStats(wrongSection.getId(), 10L, 2L, 3L);
        saveBoardStats(belowThreshold.getId(), 2L, 9L, 7L);

        List<Long> boardIds = boardRepository.findTopBoardIdsForSnapshot(new BoardSnapshotQuery(
                SoftDeleteState.ACTIVE,
                BoardPageQuery.FeaturedThreshold.atLeast(5L),
                BoardPageQuery.SectionFilter.selected("free"),
                PageRequest.of(0, 10)
        ));

        assertThat(boardIds).containsExactly(included.getId());
    }

    @Test
    void findTopByStateAndRegTimeAfterOrderByPopularity_prefersStatsOrdering() {
        Account author = saveAccount("querydsl-popular");
        Board higherStats = saveBoard(author, "higher");
        Board lowerStats = saveBoard(author, "lower");

        saveBoardStats(higherStats.getId(), 12L, 6L, 3L);
        saveBoardStats(lowerStats.getId(), 2L, 10L, 8L);

        List<Board> boards = boardRepository.findTopByStateAndRegTimeAfterOrderByPopularity(
                SoftDeleteState.ACTIVE,
                LocalDateTime.now().minusDays(1),
                PageRequest.of(0, 10)
        );

        assertThat(boards)
                .extracting(Board::getId)
                .containsSequence(higherStats.getId(), lowerStats.getId());
    }

    private Account saveAccount(String prefix) {
        String suffix = UUID.randomUUID().toString().replace("-", "").substring(0, 8);
        return accountRepository.saveAndFlush(Account.builder()
                .email(prefix + "-" + suffix + "@test.com")
                .name((prefix + "-" + suffix).substring(0, Math.min(20, prefix.length() + suffix.length() + 1)))
                .authority(Role.USER)
                .loginMethod("Current")
                .build());
    }

    private Board saveBoard(Account account, String title) {
        return boardRepository.saveAndFlush(Board.builder()
                .title(title)
                .contents("content")
                .account(account)
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(false)
                .build());
    }

    private Board saveBoard(Account account, BoardSection section, String title) {
        return boardRepository.saveAndFlush(Board.builder()
                .title(title)
                .contents("content")
                .account(account)
                .section(section)
                .state(SoftDeleteState.ACTIVE)
                .hiddenByReport(false)
                .build());
    }

    private void saveBoardStats(Long boardId, long totalLikes, long totalComments, long totalViews) {
        boardStatsRepository.saveAndFlush(BoardStats.builder()
                .boardId(boardId)
                .totalViews(totalViews)
                .totalLikes(totalLikes)
                .totalComments(totalComments)
                .score(BigDecimal.ZERO)
                .updatedAt(LocalDateTime.now())
                .build());
    }

    private BoardPageQuery pageQuery(BoardPageQuery.Order order,
                                     String search,
                                     String sectionKey,
                                     Long authorId,
                                     List<Long> blockedAccountIds) {
        return new BoardPageQuery(
                SoftDeleteState.ACTIVE,
                order,
                search == null ? BoardPageQuery.SearchFilter.none() : BoardPageQuery.SearchFilter.titleContaining(search),
                sectionKey == null ? BoardPageQuery.SectionFilter.all() : BoardPageQuery.SectionFilter.selected(sectionKey),
                authorId == null ? BoardPageQuery.AuthorFilter.any() : BoardPageQuery.AuthorFilter.byAuthor(authorId),
                blockedAccountIds.isEmpty()
                        ? BoardPageQuery.VisibilityFilter.visibleToAll()
                        : new BoardPageQuery.VisibilityFilter(true, blockedAccountIds),
                BoardPageQuery.FeaturedThreshold.none(),
                PageRequest.of(0, 10)
        );
    }

    private BoardPageQuery featuredQuery(long threshold,
                                         String sectionKey,
                                         List<Long> blockedAccountIds) {
        BoardPageQuery baseQuery = pageQuery(BoardPageQuery.Order.FEATURED, null, sectionKey, null, blockedAccountIds);
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
}

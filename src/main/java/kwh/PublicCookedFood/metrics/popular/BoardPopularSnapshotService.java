package kwh.PublicCookedFood.metrics.popular;

import kwh.PublicCookedFood.board.domain.Board;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Slf4j
public class BoardPopularSnapshotService {

    public static final String FEATURED_RANKING_TYPE = "FEATURED";

    private final BoardPopularSnapshotRepository snapshotRepository;
    private final BoardRepository boardRepository;

    @Transactional(readOnly = true)
    public Optional<Page<Board>> loadFeaturedRankingPage(Pageable pageable, String sectionKey) {
        String normalizedSectionKey = normalizeSectionKey(sectionKey);
        try {
            LocalDateTime now = LocalDateTime.now();
            long total = snapshotRepository.countActiveVisibleBySlot(
                    FEATURED_RANKING_TYPE,
                    normalizedSectionKey,
                    now,
                    SoftDeleteState.ACTIVE
            );
            if (total <= 0) {
                return Optional.empty();
            }

            List<Long> orderedIds = snapshotRepository.findActiveVisibleBoardIdsBySlot(
                    FEATURED_RANKING_TYPE,
                    normalizedSectionKey,
                    now,
                    SoftDeleteState.ACTIVE,
                    pageable
            );
            if (orderedIds.isEmpty()) {
                return Optional.of(new PageImpl<>(List.of(), pageable, total));
            }

            List<Board> boards = boardRepository.findAllByIdInWithAccountAndState(orderedIds, SoftDeleteState.ACTIVE);
            Map<Long, Board> boardById = new LinkedHashMap<>();
            for (Board board : boards) {
                if (board == null || board.getId() == null) {
                    continue;
                }
                boardById.put(board.getId(), board);
            }

            List<Board> orderedBoards = new ArrayList<>();
            for (Long boardId : orderedIds) {
                Board board = boardById.get(boardId);
                if (board != null) {
                    orderedBoards.add(board);
                }
            }

            return Optional.of(new PageImpl<>(orderedBoards, pageable, total));
        } catch (DataAccessException e) {
            log.warn("Failed to load board featured ranking; fallback to DB query. sectionKey={}", normalizedSectionKey, e);
            return Optional.empty();
        }
    }

    @Transactional
    public void replaceFeaturedRanking(String sectionKey,
                                        List<Long> rankedBoardIds,
                                        LocalDateTime generatedAt,
                                        LocalDateTime expiresAt) {
        String normalizedSectionKey = normalizeSectionKey(sectionKey);
        snapshotRepository.deleteBySlot(FEATURED_RANKING_TYPE, normalizedSectionKey);

        if (rankedBoardIds == null || rankedBoardIds.isEmpty()) {
            return;
        }

        List<BoardPopularSnapshot> rows = new ArrayList<>();
        int rank = 1;
        for (Long boardId : rankedBoardIds) {
            if (boardId == null) {
                continue;
            }
            rows.add(BoardPopularSnapshot.builder()
                    .rankingType(FEATURED_RANKING_TYPE)
                    .sectionKey(normalizedSectionKey)
                    .rankNo(rank)
                    .boardId(boardId)
                    .score(BigDecimal.ZERO)
                    .generatedAt(generatedAt)
                    .expiresAt(expiresAt)
                    .build());
            rank++;
        }

        if (!rows.isEmpty()) {
            snapshotRepository.saveAll(rows);
        }
    }

    @Transactional
    public int deleteExpiredRankings() {
        return snapshotRepository.deleteExpired(LocalDateTime.now());
    }

    private String normalizeSectionKey(String sectionKey) {
        if (sectionKey == null) {
            return "";
        }
        String trimmed = sectionKey.trim();
        return trimmed.isEmpty() ? "" : trimmed;
    }
}

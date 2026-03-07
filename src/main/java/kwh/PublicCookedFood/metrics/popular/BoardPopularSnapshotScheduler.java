package kwh.PublicCookedFood.metrics.popular;

import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.board.domain.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.service.BoardPolicyService;
import kwh.PublicCookedFood.board.service.BoardSectionService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class BoardPopularSnapshotScheduler {

    private final BoardRepository boardRepository;
    private final BoardPolicyService boardPolicyService;
    private final BoardSectionService boardSectionService;
    private final BoardPopularSnapshotService boardPopularSnapshotService;

    @Value("${app.popular.board.snapshot.enabled:false}")
    private boolean enabled;

    @Value("${app.popular.board.snapshot.top-n:200}")
    private int topN;

    @Value("${app.popular.board.snapshot.ttl-minutes:15}")
    private int ttlMinutes;

    @Scheduled(fixedDelayString = "${app.popular.board.snapshot.interval-ms:300000}")
    public void generateFeaturedSnapshots() {
        if (!enabled) {
            return;
        }

        long threshold = boardPolicyService.getFeaturedLikeThreshold();
        int normalizedTopN = Math.max(1, topN);
        int normalizedTtlMinutes = Math.max(1, ttlMinutes);
        LocalDateTime generatedAt = LocalDateTime.now();
        LocalDateTime expiresAt = generatedAt.plusMinutes(normalizedTtlMinutes);

        Set<String> sectionKeys = new LinkedHashSet<>();
        sectionKeys.add("");
        List<BoardSection> sections = boardSectionService.getActiveSections();
        if (sections == null) {
            sections = List.of();
        }
        for (BoardSection section : sections) {
            if (section == null || section.getSectionKey() == null) {
                continue;
            }
            String key = section.getSectionKey().trim();
            if (!key.isEmpty()) {
                sectionKeys.add(key);
            }
        }

        int updatedSlots = 0;
        for (String sectionKey : sectionKeys) {
            List<Long> boardIds = boardRepository.findTopBoardIdsForSnapshot(
                    SoftDeleteState.ACTIVE,
                    threshold,
                    sectionKey,
                    PageRequest.of(0, normalizedTopN)
            );
            boardPopularSnapshotService.replaceFeaturedRanking(sectionKey, boardIds, generatedAt, expiresAt);
            updatedSlots++;
        }

        int deletedExpired = boardPopularSnapshotService.deleteExpiredRankings();
        log.debug("Generated board featured rankings. slots={}, expiredDeleted={}", updatedSlots, deletedExpired);
    }
}

package kwh.PublicCookedFood.metrics.popular;

import kwh.PublicCookedFood.board.domain.BoardSection;
import kwh.PublicCookedFood.common.persistence.SoftDeleteState;
import kwh.PublicCookedFood.board.repository.BoardPageQuery;
import kwh.PublicCookedFood.board.repository.BoardRepository;
import kwh.PublicCookedFood.board.repository.BoardSnapshotQuery;
import kwh.PublicCookedFood.board.service.query.BoardPolicyQueryService;
import kwh.PublicCookedFood.board.service.query.BoardSectionQueryService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class BoardPopularSnapshotSchedulerUnitTest {

    @Mock
    private BoardRepository boardRepository;

    @Mock
    private BoardPolicyQueryService boardPolicyQueryService;

    @Mock
    private BoardSectionQueryService boardSectionQueryService;

    @Mock
    private BoardPopularSnapshotService boardPopularSnapshotService;

    @InjectMocks
    private BoardPopularSnapshotScheduler scheduler;

    @Test
    void generateFeaturedSnapshots_returnsImmediatelyWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "enabled", false);

        scheduler.generateFeaturedSnapshots();

        verify(boardPolicyQueryService, never()).getFeaturedLikeThreshold();
    }

    @Test
    void generateFeaturedSnapshots_rebuildsGlobalAndSectionSlots() {
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        ReflectionTestUtils.setField(scheduler, "topN", 50);
        ReflectionTestUtils.setField(scheduler, "ttlMinutes", 15);

        when(boardPolicyQueryService.getFeaturedLikeThreshold()).thenReturn(5);
        BoardSection section = BoardSection.builder()
                .id(2L)
                .sectionKey("general")
                .sectionName("일반")
                .displayOrder(1)
                .active(true)
                .build();
        when(boardSectionQueryService.getActiveSections()).thenReturn(List.of(section));

        when(boardRepository.findTopBoardIdsForSnapshot(argThat(query ->
                query != null
                        && query.state() == SoftDeleteState.ACTIVE
                        && query.featuredThreshold().minimumLikes().map(value -> value == 5L).orElse(false)
                        && query.section().sectionKey().isEmpty()
                        && query.pageable().equals(PageRequest.of(0, 50))
        )))
                .thenReturn(List.of(10L));
        when(boardRepository.findTopBoardIdsForSnapshot(argThat(query ->
                query != null
                        && query.state() == SoftDeleteState.ACTIVE
                        && query.featuredThreshold().minimumLikes().map(value -> value == 5L).orElse(false)
                        && query.section().sectionKey().map("general"::equals).orElse(false)
                        && query.pageable().equals(PageRequest.of(0, 50))
        )))
                .thenReturn(List.of(20L, 30L));

        scheduler.generateFeaturedSnapshots();

        verify(boardPopularSnapshotService).replaceFeaturedRanking(eq(""), eq(List.of(10L)), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(boardPopularSnapshotService).replaceFeaturedRanking(eq("general"), eq(List.of(20L, 30L)), any(LocalDateTime.class), any(LocalDateTime.class));
        verify(boardPopularSnapshotService).deleteExpiredRankings();

        ArgumentCaptor<LocalDateTime> generatedCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        ArgumentCaptor<LocalDateTime> expiresCaptor = ArgumentCaptor.forClass(LocalDateTime.class);
        verify(boardPopularSnapshotService).replaceFeaturedRanking(eq(""), eq(List.of(10L)), generatedCaptor.capture(), expiresCaptor.capture());
        assertThat(expiresCaptor.getValue()).isAfter(generatedCaptor.getValue());
    }
}

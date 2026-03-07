package kwh.PublicCookedFood.metrics.reco;

import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.metrics.view.RecipeStatsRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Pageable;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeRecoSnapshotServiceUnitTest {

    @Mock
    private RecipeRecoSnapshotRepository snapshotRepository;

    @Mock
    private Recipe_INFO_Repository recipeInfoRepository;

    @Mock
    private RecipeStatsRepository recipeStatsRepository;

    @InjectMocks
    private RecipeRecoSnapshotService service;

    @Test
    void loadRecommendations_returnsOrderedItems() {
        ReflectionTestUtils.setField(service, "fallbackEnabled", true);
        LocalDate today = LocalDate.now();
        RecipeRecoSnapshot rank1 = RecipeRecoSnapshot.builder()
                .slotDate(today)
                .slotType(RecipeRecoSnapshotService.SLOT_LUNCH)
                .rankNo(1)
                .recipeId(2L)
                .score(BigDecimal.valueOf(10))
                .generatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        RecipeRecoSnapshot rank2 = RecipeRecoSnapshot.builder()
                .slotDate(today)
                .slotType(RecipeRecoSnapshotService.SLOT_LUNCH)
                .rankNo(2)
                .recipeId(1L)
                .score(BigDecimal.valueOf(8))
                .generatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(snapshotRepository.findActiveBySlot(eq(today), eq(RecipeRecoSnapshotService.SLOT_LUNCH), any(), any(Pageable.class)))
                .thenReturn(List.of(rank1, rank2));

        Recipe_INFO recipe1 = Recipe_INFO.builder().recipeID(1L).recipeNMKO("one").imgURL("/1.png").build();
        Recipe_INFO recipe2 = Recipe_INFO.builder().recipeID(2L).recipeNMKO("two").imgURL("/2.png").build();
        when(recipeInfoRepository.findAllByRecipeIDIn(List.of(2L, 1L))).thenReturn(List.of(recipe1, recipe2));

        List<RecipeRecoSnapshotService.RecipeRecommendationItem> result = service.loadRecommendations("lunch", 5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).recipeId()).isEqualTo(2L);
        assertThat(result.get(1).recipeId()).isEqualTo(1L);
        assertThat(result.get(0).score()).isEqualByComparingTo("10");
        assertThat(result.get(1).score()).isEqualByComparingTo("8");
    }

    @Test
    void replaceSlot_replacesRowsByRank() {
        ReflectionTestUtils.setField(service, "fallbackEnabled", true);
        LocalDate slotDate = LocalDate.now();
        LocalDateTime generatedAt = LocalDateTime.now();
        LocalDateTime expiresAt = generatedAt.plusMinutes(30);

        service.replaceSlot(
                slotDate,
                RecipeRecoSnapshotService.SLOT_DINNER,
                List.of(
                        new RecipeRecoSnapshotService.RecommendationCandidate(7L, BigDecimal.valueOf(10)),
                        new RecipeRecoSnapshotService.RecommendationCandidate(8L, BigDecimal.valueOf(9))
                ),
                generatedAt,
                expiresAt
        );

        verify(snapshotRepository).deleteBySlot(slotDate, RecipeRecoSnapshotService.SLOT_DINNER);
        ArgumentCaptor<List<RecipeRecoSnapshot>> captor = ArgumentCaptor.forClass(List.class);
        verify(snapshotRepository).saveAll(captor.capture());

        List<RecipeRecoSnapshot> saved = captor.getValue();
        assertThat(saved).hasSize(2);
        assertThat(saved.get(0).getRankNo()).isEqualTo(1);
        assertThat(saved.get(1).getRankNo()).isEqualTo(2);
    }

    @Test
    void loadRecommendations_deduplicatesRecipeIdsAndKeepsTopRankScore() {
        ReflectionTestUtils.setField(service, "fallbackEnabled", true);
        LocalDate today = LocalDate.now();
        RecipeRecoSnapshot rank1 = RecipeRecoSnapshot.builder()
                .slotDate(today)
                .slotType(RecipeRecoSnapshotService.SLOT_LUNCH)
                .rankNo(1)
                .recipeId(2L)
                .score(BigDecimal.valueOf(10))
                .generatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        RecipeRecoSnapshot rank2Duplicate = RecipeRecoSnapshot.builder()
                .slotDate(today)
                .slotType(RecipeRecoSnapshotService.SLOT_LUNCH)
                .rankNo(2)
                .recipeId(2L)
                .score(BigDecimal.valueOf(9))
                .generatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();
        RecipeRecoSnapshot rank3 = RecipeRecoSnapshot.builder()
                .slotDate(today)
                .slotType(RecipeRecoSnapshotService.SLOT_LUNCH)
                .rankNo(3)
                .recipeId(1L)
                .score(BigDecimal.valueOf(8))
                .generatedAt(LocalDateTime.now())
                .expiresAt(LocalDateTime.now().plusMinutes(10))
                .build();

        when(snapshotRepository.findActiveBySlot(eq(today), eq(RecipeRecoSnapshotService.SLOT_LUNCH), any(), any(Pageable.class)))
                .thenReturn(List.of(rank1, rank2Duplicate, rank3));
        Recipe_INFO recipe1 = Recipe_INFO.builder().recipeID(1L).recipeNMKO("one").imgURL("/1.png").build();
        Recipe_INFO recipe2 = Recipe_INFO.builder().recipeID(2L).recipeNMKO("two").imgURL("/2.png").build();
        when(recipeInfoRepository.findAllByRecipeIDIn(List.of(2L, 1L))).thenReturn(List.of(recipe1, recipe2));

        List<RecipeRecoSnapshotService.RecipeRecommendationItem> result = service.loadRecommendations("LUNCH", 5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).recipeId()).isEqualTo(2L);
        assertThat(result.get(0).score()).isEqualByComparingTo("10");
        assertThat(result.get(1).recipeId()).isEqualTo(1L);
        assertThat(result.get(1).score()).isEqualByComparingTo("8");
    }

    @Test
    void loadRecommendations_usesScoreFallbackWhenSnapshotMissing() {
        ReflectionTestUtils.setField(service, "fallbackEnabled", true);
        LocalDate today = LocalDate.now();
        when(snapshotRepository.findActiveBySlot(eq(today), eq(RecipeRecoSnapshotService.SLOT_LUNCH), any(), any(Pageable.class)))
                .thenReturn(List.of());
        when(recipeStatsRepository.findTopRecipeScoresForSnapshot(any()))
                .thenReturn(List.of(scoreRow(2L, "12"), scoreRow(1L, "10")));
        Recipe_INFO recipe1 = Recipe_INFO.builder().recipeID(1L).recipeNMKO("one").imgURL("/1.png").build();
        Recipe_INFO recipe2 = Recipe_INFO.builder().recipeID(2L).recipeNMKO("two").imgURL("/2.png").build();
        when(recipeInfoRepository.findAllByRecipeIDIn(List.of(2L, 1L))).thenReturn(List.of(recipe1, recipe2));

        List<RecipeRecoSnapshotService.RecipeRecommendationItem> result = service.loadRecommendations("lunch", 5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).recipeId()).isEqualTo(2L);
        assertThat(result.get(0).score()).isEqualByComparingTo("12");
        assertThat(result.get(1).recipeId()).isEqualTo(1L);
    }

    @Test
    void loadRecommendations_returnsEmptyWhenSnapshotMissingAndFallbackDisabled() {
        ReflectionTestUtils.setField(service, "fallbackEnabled", false);
        LocalDate today = LocalDate.now();
        when(snapshotRepository.findActiveBySlot(eq(today), eq(RecipeRecoSnapshotService.SLOT_LUNCH), any(), any(Pageable.class)))
                .thenReturn(List.of());

        List<RecipeRecoSnapshotService.RecipeRecommendationItem> result = service.loadRecommendations("lunch", 5);

        assertThat(result).isEmpty();
        verify(recipeStatsRepository, never()).findTopRecipeScoresForSnapshot(any());
    }

    @Test
    void loadRecommendations_usesScoreFallbackWhenSnapshotQueryFails() {
        ReflectionTestUtils.setField(service, "fallbackEnabled", true);
        when(snapshotRepository.findActiveBySlot(any(LocalDate.class), eq(RecipeRecoSnapshotService.SLOT_LUNCH), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("query failed"));
        when(recipeStatsRepository.findTopRecipeScoresForSnapshot(any()))
                .thenReturn(List.of(scoreRow(2L, "12"), scoreRow(1L, "10")));
        Recipe_INFO recipe1 = Recipe_INFO.builder().recipeID(1L).recipeNMKO("one").imgURL("/1.png").build();
        Recipe_INFO recipe2 = Recipe_INFO.builder().recipeID(2L).recipeNMKO("two").imgURL("/2.png").build();
        when(recipeInfoRepository.findAllByRecipeIDIn(List.of(2L, 1L))).thenReturn(List.of(recipe1, recipe2));

        List<RecipeRecoSnapshotService.RecipeRecommendationItem> result = service.loadRecommendations("lunch", 5);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).recipeId()).isEqualTo(2L);
        assertThat(result.get(0).score()).isEqualByComparingTo("12");
        assertThat(result.get(1).recipeId()).isEqualTo(1L);
    }

    @Test
    void loadRecommendations_returnsEmptyWhenSnapshotQueryFailsAndFallbackDisabled() {
        ReflectionTestUtils.setField(service, "fallbackEnabled", false);
        when(snapshotRepository.findActiveBySlot(any(LocalDate.class), eq(RecipeRecoSnapshotService.SLOT_LUNCH), any(), any(Pageable.class)))
                .thenThrow(new RuntimeException("query failed"));

        List<RecipeRecoSnapshotService.RecipeRecommendationItem> result = service.loadRecommendations("lunch", 5);

        assertThat(result).isEmpty();
        verify(recipeStatsRepository, never()).findTopRecipeScoresForSnapshot(any());
    }

    private RecipeStatsRepository.RecipeScoreProjection scoreRow(Long recipeId, String score) {
        return new RecipeStatsRepository.RecipeScoreProjection() {
            @Override
            public Long getRecipeId() {
                return recipeId;
            }

            @Override
            public BigDecimal getScore() {
                return new BigDecimal(score);
            }
        };
    }
}

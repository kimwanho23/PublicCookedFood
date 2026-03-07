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
import org.springframework.data.domain.PageRequest;
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
class RecipeRecoSnapshotSchedulerUnitTest {

    @Mock
    private RecipeStatsRepository recipeStatsRepository;

    @Mock
    private RecipeRecoSnapshotService recipeRecoSnapshotService;

    @Mock
    private Recipe_INFO_Repository recipeInfoRepository;

    @InjectMocks
    private RecipeRecoSnapshotScheduler scheduler;

    @Test
    void generateRecipeRecommendations_returnsImmediatelyWhenDisabled() {
        ReflectionTestUtils.setField(scheduler, "enabled", false);

        scheduler.generateRecipeRecommendations();

        verify(recipeStatsRepository, never()).findTopRecipeScoresForSnapshot(any(PageRequest.class));
    }

    @Test
    void generateRecipeRecommendations_replacesLunchAndDinnerSlots() {
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        ReflectionTestUtils.setField(scheduler, "topN", 20);
        ReflectionTestUtils.setField(scheduler, "ttlMinutes", 30);
        ReflectionTestUtils.setField(scheduler, "slotBonus", new BigDecimal("0.05"));
        ReflectionTestUtils.setField(scheduler, "lunchKeywordsRaw", "밥,국,찌개,면,볶음,덮밥");
        ReflectionTestUtils.setField(scheduler, "dinnerKeywordsRaw", "구이,찜,탕,전골,조림,볶음");

        when(recipeStatsRepository.findTopRecipeScoresForSnapshot(PageRequest.of(0, 100)))
                .thenReturn(List.of(scoreRow(1L, "12.5"), scoreRow(2L, "11.0")));
        when(recipeInfoRepository.findAllByRecipeIDIn(List.of(1L, 2L)))
                .thenReturn(List.of(
                        recipeInfo(1L, "토마토샐러드", "샐러드"),
                        recipeInfo(2L, "치즈샐러드", "샐러드")
                ));

        scheduler.generateRecipeRecommendations();

        ArgumentCaptor<List<RecipeRecoSnapshotService.RecommendationCandidate>> lunchCandidatesCaptor =
                ArgumentCaptor.forClass(List.class);
        verify(recipeRecoSnapshotService).replaceSlot(
                any(LocalDate.class),
                eq(RecipeRecoSnapshotService.SLOT_LUNCH),
                lunchCandidatesCaptor.capture(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(recipeRecoSnapshotService).replaceSlot(
                any(LocalDate.class),
                eq(RecipeRecoSnapshotService.SLOT_DINNER),
                any(List.class),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(recipeRecoSnapshotService).deleteExpiredSnapshots();

        List<RecipeRecoSnapshotService.RecommendationCandidate> lunchCandidates = lunchCandidatesCaptor.getValue();
        assertThat(lunchCandidates).hasSize(2);
        assertThat(lunchCandidates.get(0).recipeId()).isEqualTo(1L);
        assertThat(lunchCandidates.get(0).score()).isEqualByComparingTo("12.5");
        assertThat(lunchCandidates.get(1).recipeId()).isEqualTo(2L);
        assertThat(lunchCandidates.get(1).score()).isEqualByComparingTo("11.0");
    }

    @Test
    void generateRecipeRecommendations_appliesDifferentSlotBonuses() {
        ReflectionTestUtils.setField(scheduler, "enabled", true);
        ReflectionTestUtils.setField(scheduler, "topN", 20);
        ReflectionTestUtils.setField(scheduler, "ttlMinutes", 30);
        ReflectionTestUtils.setField(scheduler, "slotBonus", new BigDecimal("0.05"));
        ReflectionTestUtils.setField(scheduler, "lunchKeywordsRaw", "밥,국,찌개,면,볶음,덮밥");
        ReflectionTestUtils.setField(scheduler, "dinnerKeywordsRaw", "구이,찜,탕,전골,조림,볶음");

        when(recipeStatsRepository.findTopRecipeScoresForSnapshot(PageRequest.of(0, 100)))
                .thenReturn(List.of(scoreRow(1L, "10"), scoreRow(2L, "10")));
        when(recipeInfoRepository.findAllByRecipeIDIn(List.of(1L, 2L)))
                .thenReturn(List.of(
                        recipeInfo(1L, "제육덮밥", "밥"),
                        recipeInfo(2L, "고등어구이", "구이")
                ));

        scheduler.generateRecipeRecommendations();

        ArgumentCaptor<List<RecipeRecoSnapshotService.RecommendationCandidate>> lunchCaptor =
                ArgumentCaptor.forClass(List.class);
        ArgumentCaptor<List<RecipeRecoSnapshotService.RecommendationCandidate>> dinnerCaptor =
                ArgumentCaptor.forClass(List.class);

        verify(recipeRecoSnapshotService).replaceSlot(
                any(LocalDate.class),
                eq(RecipeRecoSnapshotService.SLOT_LUNCH),
                lunchCaptor.capture(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );
        verify(recipeRecoSnapshotService).replaceSlot(
                any(LocalDate.class),
                eq(RecipeRecoSnapshotService.SLOT_DINNER),
                dinnerCaptor.capture(),
                any(LocalDateTime.class),
                any(LocalDateTime.class)
        );

        List<RecipeRecoSnapshotService.RecommendationCandidate> lunchCandidates = lunchCaptor.getValue();
        List<RecipeRecoSnapshotService.RecommendationCandidate> dinnerCandidates = dinnerCaptor.getValue();
        assertThat(lunchCandidates.get(0).recipeId()).isEqualTo(1L);
        assertThat(dinnerCandidates.get(0).recipeId()).isEqualTo(2L);
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

    private Recipe_INFO recipeInfo(Long recipeId, String recipeName, String typeName) {
        return Recipe_INFO.builder()
                .recipeID(recipeId)
                .recipeNMKO(recipeName)
                .tyNM(typeName)
                .build();
    }
}

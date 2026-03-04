package kwh.PublicCookedFood.food.service;

import kwh.PublicCookedFood.food.repository.RecipeAiDocRepository;
import kwh.PublicCookedFood.food.dto.response.RecipeAiAskResponse;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataAccessResourceFailureException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RecipeAiServiceUnitTest {

    @Mock
    private RecipeAiDocRepository recipeAiDocRepository;

    @Mock
    private RecipeAiLlmService recipeAiLlmService;

    @InjectMocks
    private RecipeAiService recipeAiService;

    @Test
    void getRecommendationReadiness_returnsReadyWhenDocumentsExist() {
        when(recipeAiDocRepository.countAll()).thenReturn(123L);
        when(recipeAiDocRepository.findFirstRecipeId()).thenReturn(Optional.of(10001L));

        RecipeAiService.RecommendationReadiness readiness = recipeAiService.getRecommendationReadiness();

        assertThat(readiness.ready()).isTrue();
        assertThat(readiness.documentCount()).isEqualTo(123L);
        assertThat(readiness.sampleRecipeId()).isEqualTo(10001L);
        assertThat(readiness.message()).isNull();
    }

    @Test
    void getRecommendationReadiness_returnsNotReadyWhenDocumentsAreEmpty() {
        when(recipeAiDocRepository.countAll()).thenReturn(0L);
        when(recipeAiDocRepository.findFirstRecipeId()).thenReturn(Optional.empty());

        RecipeAiService.RecommendationReadiness readiness = recipeAiService.getRecommendationReadiness();

        assertThat(readiness.ready()).isFalse();
        assertThat(readiness.documentCount()).isZero();
        assertThat(readiness.sampleRecipeId()).isNull();
        assertThat(readiness.message()).contains("추천 가능한 레시피 문서가 없습니다");
    }

    @Test
    void getRecommendationReadiness_returnsNotReadyWhenViewQueryFails() {
        when(recipeAiDocRepository.countAll())
                .thenThrow(new DataAccessResourceFailureException("recipe_ai_doc unavailable"));

        RecipeAiService.RecommendationReadiness readiness = recipeAiService.getRecommendationReadiness();

        assertThat(readiness.ready()).isFalse();
        assertThat(readiness.documentCount()).isZero();
        assertThat(readiness.sampleRecipeId()).isNull();
        assertThat(readiness.message()).contains("recipe_ai_doc 뷰를 조회할 수 없습니다");
    }

    @Test
    void askRecipeAsync_usesRuleBasedAnswerWhenLlmUnavailable() {
        RecipeAiDocRepository.RecipeAiDoc doc = createDoc(5001L);
        when(recipeAiDocRepository.findByRecipeId(5001L)).thenReturn(Optional.of(doc));
        when(recipeAiLlmService.generateAnswer(doc.aiDocument(), "재료 알려줘"))
                .thenReturn(Optional.empty());

        RecipeAiAskResponse response = recipeAiService.askRecipeAsync(5001L, "재료 알려줘").join();

        assertThat(response.modelGenerated()).isFalse();
        assertThat(response.answer()).contains("주요 재료");
        assertThat(response.recipeId()).isEqualTo(5001L);
    }

    @Test
    void askRecipeAsync_marksModelGeneratedTrueWhenLlmSucceeds() {
        RecipeAiDocRepository.RecipeAiDoc doc = createDoc(5002L);
        when(recipeAiDocRepository.findByRecipeId(5002L)).thenReturn(Optional.of(doc));
        when(recipeAiLlmService.generateAnswer(doc.aiDocument(), "이 레시피 요약해줘"))
                .thenReturn(Optional.of("LLM 응답입니다."));

        RecipeAiAskResponse response = recipeAiService.askRecipeAsync(5002L, "이 레시피 요약해줘").join();

        assertThat(response.modelGenerated()).isTrue();
        assertThat(response.answer()).isEqualTo("LLM 응답입니다.");
        assertThat(response.recipeId()).isEqualTo(5002L);
    }

    private RecipeAiDocRepository.RecipeAiDoc createDoc(Long recipeId) {
        return new RecipeAiDocRepository.RecipeAiDoc(
                1L,
                recipeId,
                "김치찌개",
                "매콤한 국물 요리",
                "한국",
                "찌개",
                "보통",
                "30분",
                30,
                "2인분",
                2,
                "350kcal",
                350,
                3,
                4,
                "주재료: 김치 | 부재료: 돼지고기",
                "1. 볶기 | 2. 끓이기",
                "이름: 김치찌개"
        );
    }
}

package kwh.PublicCookedFood.food.facade;

import kwh.PublicCookedFood.config.AuthThrottleService;
import kwh.PublicCookedFood.config.properties.OpenAiProperties;
import kwh.PublicCookedFood.food.dto.response.RecipeAiStatusResponse;
import kwh.PublicCookedFood.food.service.RecipeAiService;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RecipeAiFacadeUnitTest {

    @Test
    void getStatus_exposesReadinessAndOpenAiFlag() {
        RecipeAiService recipeAiService = mock(RecipeAiService.class);
        AuthThrottleService authThrottleService = mock(AuthThrottleService.class);
        OpenAiProperties openAiProperties = new OpenAiProperties(
                true,
                "https://api.openai.com/v1",
                "gpt-4o-mini",
                "test-key",
                0.2d
        );
        RecipeAiFacade recipeAiFacade = new RecipeAiFacade(recipeAiService, authThrottleService, openAiProperties);

        when(recipeAiService.getRecommendationReadiness())
                .thenReturn(RecipeAiService.RecommendationReadiness.ready(55L, 9001L));

        RecipeAiStatusResponse status = recipeAiFacade.getStatus();

        assertThat(status.recommendationReady()).isTrue();
        assertThat(status.recipeDocumentCount()).isEqualTo(55L);
        assertThat(status.sampleRecipeId()).isEqualTo(9001L);
        assertThat(status.openAiEnabled()).isTrue();
        assertThat(status.message()).isNull();
        assertThat(status.checkedAt()).isNotNull();
    }

    @Test
    void getStatus_exposesNotReadyMessage() {
        RecipeAiService recipeAiService = mock(RecipeAiService.class);
        AuthThrottleService authThrottleService = mock(AuthThrottleService.class);
        OpenAiProperties openAiProperties = new OpenAiProperties(
                false,
                "https://api.openai.com/v1",
                "gpt-4o-mini",
                "",
                0.2d
        );
        RecipeAiFacade recipeAiFacade = new RecipeAiFacade(recipeAiService, authThrottleService, openAiProperties);

        when(recipeAiService.getRecommendationReadiness())
                .thenReturn(RecipeAiService.RecommendationReadiness.notReady(0L, null, "뷰 조회 실패"));

        RecipeAiStatusResponse status = recipeAiFacade.getStatus();

        assertThat(status.recommendationReady()).isFalse();
        assertThat(status.recipeDocumentCount()).isZero();
        assertThat(status.sampleRecipeId()).isNull();
        assertThat(status.openAiEnabled()).isFalse();
        assertThat(status.message()).isEqualTo("뷰 조회 실패");
    }
}

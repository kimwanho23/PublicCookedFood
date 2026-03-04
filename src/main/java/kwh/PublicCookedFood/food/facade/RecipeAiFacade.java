package kwh.PublicCookedFood.food.facade;

import jakarta.servlet.http.HttpServletRequest;
import kwh.PublicCookedFood.config.AuthThrottleService;
import kwh.PublicCookedFood.config.properties.OpenAiProperties;
import kwh.PublicCookedFood.food.dto.request.RecipeAiRecommendRequest;
import kwh.PublicCookedFood.food.dto.response.RecipeAiAskResponse;
import kwh.PublicCookedFood.food.dto.response.RecipeAiRecommendResponse;
import kwh.PublicCookedFood.food.dto.response.RecipeAiStatusResponse;
import kwh.PublicCookedFood.food.service.RecipeAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

import java.time.LocalDateTime;
import java.util.concurrent.CompletableFuture;

@Service
@RequiredArgsConstructor
public class RecipeAiFacade {

    private final RecipeAiService recipeAiService;
    private final AuthThrottleService authThrottleService;
    private final OpenAiProperties openAiProperties;

    public CompletableFuture<RecipeAiRecommendResponse> recommend(HttpServletRequest request,
                                                                  RecipeAiRecommendRequest recommendRequest) {
        enforceAiRateLimit(request, "recommend");
        return recipeAiService.recommendAsync(recommendRequest);
    }

    public CompletableFuture<RecipeAiAskResponse> ask(HttpServletRequest request,
                                                      Long recipeId,
                                                      String question) {
        enforceAiRateLimit(request, "ask");
        return recipeAiService.askRecipeAsync(recipeId, question);
    }

    public RecipeAiStatusResponse getStatus() {
        RecipeAiService.RecommendationReadiness readiness = recipeAiService.getRecommendationReadiness();
        return new RecipeAiStatusResponse(
                LocalDateTime.now(),
                readiness.ready(),
                readiness.documentCount(),
                readiness.sampleRecipeId(),
                Boolean.TRUE.equals(openAiProperties.enabled()),
                readiness.message()
        );
    }

    private void enforceAiRateLimit(HttpServletRequest request, String action) {
        if (authThrottleService.tryConsumeAiAttempt(request, action)) {
            return;
        }
        long retryAfter = authThrottleService.getAiRetryAfterSeconds(request, action);
        String message = retryAfter <= 0
                ? "요청이 너무 많습니다. 잠시 후 다시 시도해주세요."
                : "요청이 너무 많습니다. 약 " + retryAfter + "초 후 다시 시도해주세요.";
        throw new ResponseStatusException(HttpStatus.TOO_MANY_REQUESTS, message);
    }
}

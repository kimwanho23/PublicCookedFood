package kwh.PublicCookedFood.food.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kwh.PublicCookedFood.config.AuthThrottleService;
import kwh.PublicCookedFood.food.dto.request.RecipeAiAskRequest;
import kwh.PublicCookedFood.food.dto.request.RecipeAiRecommendRequest;
import kwh.PublicCookedFood.food.dto.response.RecipeAiAskResponse;
import kwh.PublicCookedFood.food.dto.response.RecipeAiRecommendResponse;
import kwh.PublicCookedFood.food.service.RecipeAiService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.server.ResponseStatusException;

import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/ai/recipes")
@Tag(name = "Recipe AI API")
public class RecipeAiController {

    private final RecipeAiService recipeAiService;
    private final AuthThrottleService authThrottleService;

    @PostMapping("/recommend")
    public CompletableFuture<ResponseEntity<RecipeAiRecommendResponse>> recommend(
            HttpServletRequest httpServletRequest,
            @Valid @RequestBody RecipeAiRecommendRequest request
    ) {
        enforceAiRateLimit(httpServletRequest, "recommend");
        return recipeAiService.recommendAsync(request)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/{recipeId}/ask")
    public CompletableFuture<ResponseEntity<RecipeAiAskResponse>> ask(
            HttpServletRequest httpServletRequest,
            @PathVariable @Positive(message = "레시피 ID는 양수여야 합니다.") Long recipeId,
            @Valid @RequestBody RecipeAiAskRequest request
    ) {
        enforceAiRateLimit(httpServletRequest, "ask");
        return recipeAiService.askRecipeAsync(recipeId, request.normalizedQuestion())
                .thenApply(ResponseEntity::ok);
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

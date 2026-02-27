package kwh.PublicCookedFood.food.controller;

import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Positive;
import kwh.PublicCookedFood.food.dto.request.RecipeAiAskRequest;
import kwh.PublicCookedFood.food.dto.request.RecipeAiRecommendRequest;
import kwh.PublicCookedFood.food.dto.response.RecipeAiAskResponse;
import kwh.PublicCookedFood.food.dto.response.RecipeAiRecommendResponse;
import kwh.PublicCookedFood.food.facade.RecipeAiFacade;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.concurrent.CompletableFuture;

@RestController
@RequiredArgsConstructor
@Validated
@RequestMapping("/api/ai/recipes")
@Tag(name = "Recipe AI API")
public class RecipeAiController {

    private final RecipeAiFacade recipeAiFacade;

    @PostMapping("/recommend")
    public CompletableFuture<ResponseEntity<RecipeAiRecommendResponse>> recommend(
            HttpServletRequest httpServletRequest,
            @Valid @RequestBody RecipeAiRecommendRequest request
    ) {
        return recipeAiFacade.recommend(httpServletRequest, request)
                .thenApply(ResponseEntity::ok);
    }

    @PostMapping("/{recipeId}/ask")
    public CompletableFuture<ResponseEntity<RecipeAiAskResponse>> ask(
            HttpServletRequest httpServletRequest,
            @PathVariable @Positive(message = "레시피 ID는 양수여야 합니다.") Long recipeId,
            @Valid @RequestBody RecipeAiAskRequest request
    ) {
        return recipeAiFacade.ask(httpServletRequest, recipeId, request.normalizedQuestion())
                .thenApply(ResponseEntity::ok);
    }
}

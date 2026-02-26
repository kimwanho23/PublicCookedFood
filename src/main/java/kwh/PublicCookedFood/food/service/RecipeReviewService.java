package kwh.PublicCookedFood.food.service;

import jakarta.transaction.Transactional;
import kwh.PublicCookedFood.food.dto.response.RecipeRankingResponse;
import kwh.PublicCookedFood.food.dto.response.RecipeReviewResponse;
import kwh.PublicCookedFood.food.dto.response.RecipeReviewSummaryResponse;
import kwh.PublicCookedFood.food.entity.RecipeReview;
import kwh.PublicCookedFood.food.entity.Recipe_INFO;
import kwh.PublicCookedFood.food.repository.RecipeReviewRepository;
import kwh.PublicCookedFood.food.repository.Recipe_INFO_Repository;
import kwh.PublicCookedFood.user.domain.Users;
import kwh.PublicCookedFood.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class RecipeReviewService {

    private static final int MAX_REVIEW_CONTENT_LENGTH = 500;

    private final RecipeReviewRepository recipeReviewRepository;
    private final Recipe_INFO_Repository recipeInfoRepository;
    private final UserRepository userRepository;

    @Transactional
    public void upsertReview(Long recipeId, Long userId, Integer rating, String contents) {
        if (recipeId == null || userId == null) {
            throw new IllegalArgumentException("리뷰 요청 값이 올바르지 않습니다.");
        }

        Recipe_INFO recipe = recipeInfoRepository.findByRecipeID(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("레시피 정보를 찾을 수 없습니다."));
        Users user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("사용자 정보를 찾을 수 없습니다."));
        String normalizedContents = normalizeReviewContents(contents);

        recipeReviewRepository.findByRecipeAndUser(recipe, user)
                .ifPresentOrElse(
                        review -> review.update(rating, normalizedContents),
                        () -> recipeReviewRepository.save(RecipeReview.builder()
                                .recipe(recipe)
                                .user(user)
                                .rating(rating)
                                .contents(normalizedContents)
                                .build())
                );
    }

    @Transactional
    public RecipeReviewSummaryResponse getSummary(Long recipeId) {
        Recipe_INFO recipe = recipeInfoRepository.findByRecipeID(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("레시피 정보를 찾을 수 없습니다."));
        long reviewCount = recipeReviewRepository.countByRecipe(recipe);
        double averageRating = recipeReviewRepository.findAverageRatingByRecipe(recipe);
        return new RecipeReviewSummaryResponse(round(averageRating), reviewCount);
    }

    @Transactional
    public List<RecipeReviewResponse> getRecentReviews(Long recipeId) {
        Recipe_INFO recipe = recipeInfoRepository.findByRecipeID(recipeId)
                .orElseThrow(() -> new IllegalArgumentException("레시피 정보를 찾을 수 없습니다."));
        return recipeReviewRepository.findTop20ByRecipeOrderByRegTimeDesc(recipe).stream()
                .map(review -> new RecipeReviewResponse(
                        review.getUser().getId(),
                        review.getUser().getName(),
                        review.getRating(),
                        review.getContents(),
                        review.getRegTime()))
                .toList();
    }

    @Transactional
    public RecipeReviewResponse getMyReview(Long recipeId, Long userId) {
        if (recipeId == null || userId == null) {
            return null;
        }
        Recipe_INFO recipe = recipeInfoRepository.findByRecipeID(recipeId)
                .orElse(null);
        Users user = userRepository.findById(userId)
                .orElse(null);
        if (recipe == null || user == null) {
            return null;
        }
        return recipeReviewRepository.findByRecipeAndUser(recipe, user)
                .map(review -> new RecipeReviewResponse(
                        review.getUser().getId(),
                        review.getUser().getName(),
                        review.getRating(),
                        review.getContents(),
                        review.getRegTime()))
                .orElse(null);
    }

    @Transactional
    public List<RecipeRankingResponse> getTopReviewRankings(LocalDateTime since, int limit) {
        LocalDateTime baseline = since == null ? LocalDateTime.now().minusDays(7) : since;
        int normalizedLimit = limit <= 0 ? 10 : Math.min(limit, 30);
        return recipeReviewRepository.findTopReviewRankingsSince(baseline, PageRequest.of(0, normalizedLimit)).stream()
                .map(row -> new RecipeRankingResponse(
                        row.getRecipe().getRecipeID(),
                        row.getRecipe().getRecipeNMKO(),
                        row.getReviewCount(),
                        round(row.getAvgRating())))
                .toList();
    }

    private double round(Double value) {
        if (value == null) {
            return 0.0d;
        }
        return Math.round(value * 10.0d) / 10.0d;
    }

    private String normalizeReviewContents(String contents) {
        if (contents == null) {
            return null;
        }
        String trimmed = contents.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        if (trimmed.length() > MAX_REVIEW_CONTENT_LENGTH) {
            throw new IllegalArgumentException("리뷰 내용은 500자 이하로 입력해주세요.");
        }
        return trimmed;
    }
}

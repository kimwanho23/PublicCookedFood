package kwh.PublicCookedFood.food.service;

import kwh.PublicCookedFood.food.dto.request.RecipeAiRecommendRequest;
import kwh.PublicCookedFood.food.dto.response.RecipeAiAskResponse;
import kwh.PublicCookedFood.food.dto.response.RecipeAiRecommendItemResponse;
import kwh.PublicCookedFood.food.dto.response.RecipeAiRecommendResponse;
import kwh.PublicCookedFood.food.repository.RecipeAiDocRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataAccessException;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class RecipeAiService {

    private final RecipeAiDocRepository recipeAiDocRepository;
    private final RecipeAiLlmService recipeAiLlmService;

    @Async("recipeAiExecutor")
    public CompletableFuture<RecipeAiRecommendResponse> recommendAsync(RecipeAiRecommendRequest request) {
        List<RecipeAiDocRepository.RecipeAiDoc> docs = loadAllDocs();

        List<ScoredRecipe> scoredRecipes = docs.stream()
                .map(doc -> scoreRecipe(doc, request))
                .filter(Optional::isPresent)
                .map(Optional::get)
                .sorted(Comparator.comparingInt(ScoredRecipe::score).reversed()
                        .thenComparing(scored -> scored.doc().recipeId()))
                .toList();

        int limit = request.normalizedLimit();
        List<RecipeAiRecommendItemResponse> items = scoredRecipes.stream()
                .limit(limit)
                .map(this::toRecommendItem)
                .toList();

        RecipeAiRecommendResponse response = new RecipeAiRecommendResponse(
                LocalDateTime.now(),
                request.normalizedQuery(),
                scoredRecipes.size(),
                items.size(),
                items
        );

        return CompletableFuture.completedFuture(response);
    }

    @Async("recipeAiExecutor")
    public CompletableFuture<RecipeAiAskResponse> askRecipeAsync(Long recipeId, String question) {
        String normalizedQuestion = normalize(question);
        if (normalizedQuestion == null) {
            throw new IllegalArgumentException("질문을 입력해주세요.");
        }

        RecipeAiDocRepository.RecipeAiDoc doc = findDocByRecipeId(recipeId);
        Optional<String> llmAnswer = recipeAiLlmService.generateAnswer(doc.aiDocument(), normalizedQuestion);

        boolean modelGenerated = llmAnswer.isPresent();
        String answer = llmAnswer.orElseGet(() -> buildRuleBasedAnswer(doc, normalizedQuestion));

        RecipeAiAskResponse response = new RecipeAiAskResponse(
                LocalDateTime.now(),
                doc.recipeId(),
                doc.recipeName(),
                normalizedQuestion,
                answer,
                modelGenerated
        );

        return CompletableFuture.completedFuture(response);
    }

    public RecommendationReadiness getRecommendationReadiness() {
        try {
            long docCount = recipeAiDocRepository.countAll();
            Long sampleRecipeId = recipeAiDocRepository.findFirstRecipeId().orElse(null);
            if (docCount <= 0) {
                return RecommendationReadiness.notReady(
                        0L,
                        null,
                        "추천 가능한 레시피 문서가 없습니다. 레시피 데이터 적재 상태를 확인해주세요."
                );
            }
            return RecommendationReadiness.ready(docCount, sampleRecipeId);
        } catch (DataAccessException e) {
            log.warn("Failed to inspect recipe_ai_doc readiness.", e);
            return RecommendationReadiness.notReady(
                    0L,
                    null,
                    "recipe_ai_doc 뷰를 조회할 수 없습니다. Flyway 마이그레이션(V13)과 DB 권한을 확인해주세요."
            );
        }
    }

    private List<RecipeAiDocRepository.RecipeAiDoc> loadAllDocs() {
        try {
            return recipeAiDocRepository.findAll();
        } catch (DataAccessException e) {
            log.error("Failed to load recipe_ai_doc view.", e);
            throw new IllegalStateException("recipe_ai_doc 뷰를 조회할 수 없습니다. Flyway 마이그레이션(V13)을 먼저 적용해주세요.");
        }
    }

    private RecipeAiDocRepository.RecipeAiDoc findDocByRecipeId(Long recipeId) {
        try {
            return recipeAiDocRepository.findByRecipeId(recipeId)
                    .orElseThrow(() -> new IllegalArgumentException("레시피를 찾을 수 없습니다."));
        } catch (DataAccessException e) {
            log.error("Failed to query recipe_ai_doc by recipeId={}", recipeId, e);
            throw new IllegalStateException("recipe_ai_doc 뷰를 조회할 수 없습니다. Flyway 마이그레이션(V13)을 먼저 적용해주세요.");
        }
    }

    private Optional<ScoredRecipe> scoreRecipe(RecipeAiDocRepository.RecipeAiDoc doc,
                                               RecipeAiRecommendRequest request) {
        if (doc == null || doc.recipeId() == null) {
            return Optional.empty();
        }

        List<String> includeIngredients = request.normalizedIncludeIngredients();
        List<String> excludeIngredients = request.normalizedExcludeIngredients();
        List<String> preferredTypes = request.normalizedPreferredTypes();
        List<String> preferredNations = request.normalizedPreferredNations();
        String preferredLevel = request.normalizedPreferredLevel();
        List<String> queryTerms = request.normalizedQueryTerms();

        String searchable = buildSearchableText(doc);
        String ingredientSearchable = buildIngredientSearchText(doc);

        List<String> excludedIngredients = matchTerms(excludeIngredients, ingredientSearchable);
        if (!excludedIngredients.isEmpty()) {
            return Optional.empty();
        }

        int score = 0;
        List<String> reasons = new ArrayList<>();
        int positiveSignalCount = 0;

        List<String> includeHits = matchTerms(includeIngredients, ingredientSearchable);
        if (!includeIngredients.isEmpty()) {
            if (includeHits.isEmpty()) {
                return Optional.empty();
            }
            score += includeHits.size() * 30;
            reasons.add("포함 재료 일치: " + joinLimited(includeHits, 3));
            positiveSignalCount += includeHits.size();
        }

        List<String> queryTermHits = matchTerms(queryTerms, searchable);
        if (!queryTermHits.isEmpty()) {
            score += queryTermHits.size() * 12;
            reasons.add("키워드 일치: " + joinLimited(queryTermHits, 3));
            positiveSignalCount += queryTermHits.size();
        }

        if (containsNormalized(preferredTypes, doc.typeName())) {
            score += 14;
            reasons.add("선호 분류 일치");
            positiveSignalCount += 1;
        }

        if (containsNormalized(preferredNations, doc.nationName())) {
            score += 12;
            reasons.add("선호 국가 분류 일치");
            positiveSignalCount += 1;
        }

        if (preferredLevel != null && preferredLevel.equals(normalizeLower(doc.levelName()))) {
            score += 8;
            reasons.add("선호 난이도 일치");
            positiveSignalCount += 1;
        }

        Integer maxCookingMinutes = request.getMaxCookingMinutes();
        if (maxCookingMinutes != null) {
            if (doc.cookingTimeMinutes() == null || doc.cookingTimeMinutes() > maxCookingMinutes) {
                return Optional.empty();
            }
            score += 6;
            reasons.add("조리시간 조건 충족");
        }

        Integer maxCalorieKcal = request.getMaxCalorieKcal();
        if (maxCalorieKcal != null) {
            if (doc.calorieKcal() == null || doc.calorieKcal() > maxCalorieKcal) {
                return Optional.empty();
            }
            score += 4;
            reasons.add("칼로리 조건 충족");
        }

        Integer servings = request.getServings();
        if (servings != null) {
            if (doc.servingsCount() == null || doc.servingsCount() < servings) {
                return Optional.empty();
            }
            score += 4;
            reasons.add("인분 조건 충족");
        }

        boolean hasSoftCriteria = !includeIngredients.isEmpty()
                || !queryTerms.isEmpty()
                || !preferredTypes.isEmpty()
                || !preferredNations.isEmpty()
                || preferredLevel != null;
        if (hasSoftCriteria && positiveSignalCount == 0) {
            return Optional.empty();
        }

        int ingredientCountScore = Math.min(valueOrZero(doc.ingredientCount()), 20) / 4;
        int stepCountScore = Math.min(valueOrZero(doc.stepCount()), 10) / 2;
        score += ingredientCountScore + stepCountScore;

        if (score <= 0) {
            return Optional.empty();
        }

        if (reasons.isEmpty()) {
            reasons.add("요청 조건과 유사한 레시피");
        }

        String reason = String.join(" / ", reasons);
        return Optional.of(new ScoredRecipe(doc, score, reason));
    }

    private RecipeAiRecommendItemResponse toRecommendItem(ScoredRecipe scoredRecipe) {
        RecipeAiDocRepository.RecipeAiDoc doc = scoredRecipe.doc();
        return new RecipeAiRecommendItemResponse(
                doc.recipeId(),
                doc.recipeName(),
                doc.summary(),
                doc.nationName(),
                doc.typeName(),
                doc.levelName(),
                doc.cookingTimeText(),
                doc.cookingTimeMinutes(),
                doc.servingsText(),
                doc.servingsCount(),
                doc.calorieText(),
                doc.calorieKcal(),
                scoredRecipe.score(),
                scoredRecipe.reason(),
                "/recipes/" + doc.recipeId()
        );
    }

    private String buildRuleBasedAnswer(RecipeAiDocRepository.RecipeAiDoc doc, String question) {
        String normalizedQuestion = question.toLowerCase(Locale.ROOT);

        if (containsAny(normalizedQuestion, "재료", "ingredient", "뭐가 들어", "들어가")) {
            return "주요 재료는 " + summarizeItems(doc.ingredientLines(), 8)
                    + " 입니다.";
        }

        if (containsAny(normalizedQuestion, "순서", "과정", "어떻게", "조리", "step")) {
            return "조리 순서는 " + summarizeItems(doc.stepLines(), 3)
                    + " 순으로 진행하면 됩니다.";
        }

        if (containsAny(normalizedQuestion, "시간", "얼마나", "몇 분")) {
            return "조리 시간은 " + safeValue(doc.cookingTimeText(), "정보 없음") + " 입니다.";
        }

        if (containsAny(normalizedQuestion, "난이도", "쉬운", "어려운")) {
            return "난이도는 " + safeValue(doc.levelName(), "정보 없음")
                    + " 입니다.";
        }

        if (containsAny(normalizedQuestion, "칼로리", "kcal")) {
            return "칼로리는 " + safeValue(doc.calorieText(), "정보 없음") + " 입니다.";
        }

        if (containsAny(normalizedQuestion, "인분", "몇 명", "몇인")) {
            return "권장 분량은 " + safeValue(doc.servingsText(), "정보 없음") + " 입니다.";
        }

        return "\"" + safeValue(doc.recipeName(), "해당 레시피") + "\"는 "
                + safeValue(doc.summary(), "요약 정보가 없습니다.")
                + " 조리 시간은 " + safeValue(doc.cookingTimeText(), "정보 없음")
                + ", 난이도는 " + safeValue(doc.levelName(), "정보 없음")
                + " 입니다. 핵심 재료는 " + summarizeItems(doc.ingredientLines(), 5)
                + " 입니다.";
    }

    private String summarizeItems(String source, int limit) {
        if (source == null || source.isBlank()) {
            return "정보가 없습니다";
        }
        List<String> items = List.of(source.split("\\s*\\|\\s*"));
        List<String> normalized = items.stream()
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .limit(limit)
                .toList();

        if (normalized.isEmpty()) {
            return "정보가 없습니다";
        }

        String joined = String.join(", ", normalized);
        if (items.size() > limit) {
            return joined + " 등";
        }
        return joined;
    }

    private String buildSearchableText(RecipeAiDocRepository.RecipeAiDoc doc) {
        return String.join(" ",
                        safeValue(doc.recipeName(), ""),
                        safeValue(doc.summary(), ""),
                        safeValue(doc.nationName(), ""),
                        safeValue(doc.typeName(), ""),
                        safeValue(doc.levelName(), ""),
                        safeValue(doc.ingredientLines(), ""),
                        safeValue(doc.stepLines(), ""))
                .toLowerCase(Locale.ROOT);
    }

    private String buildIngredientSearchText(RecipeAiDocRepository.RecipeAiDoc doc) {
        return safeValue(doc.ingredientLines(), "").toLowerCase(Locale.ROOT);
    }

    private List<String> matchTerms(List<String> terms, String searchable) {
        if (terms == null || terms.isEmpty()) {
            return List.of();
        }
        return terms.stream()
                .filter(term -> term != null && !term.isBlank())
                .map(term -> term.toLowerCase(Locale.ROOT))
                .filter(searchable::contains)
                .distinct()
                .toList();
    }

    private boolean containsNormalized(List<String> normalizedCandidates, String value) {
        if (normalizedCandidates == null || normalizedCandidates.isEmpty()) {
            return false;
        }
        String normalizedValue = normalizeLower(value);
        if (normalizedValue == null) {
            return false;
        }
        return normalizedCandidates.contains(normalizedValue);
    }

    private String normalizeLower(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.isEmpty()) {
            return null;
        }
        return trimmed.toLowerCase(Locale.ROOT);
    }

    private String joinLimited(List<String> values, int max) {
        if (values == null || values.isEmpty()) {
            return "";
        }
        return values.stream().limit(max).collect(Collectors.joining(", "));
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : Math.max(0, value);
    }

    private boolean containsAny(String source, String... tokens) {
        if (source == null || source.isBlank() || tokens == null) {
            return false;
        }
        for (String token : tokens) {
            if (token != null && !token.isBlank() && source.contains(token)) {
                return true;
            }
        }
        return false;
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private String safeValue(String value, String fallback) {
        if (value == null || value.isBlank()) {
            return fallback;
        }
        return value.trim();
    }

    private record ScoredRecipe(
            RecipeAiDocRepository.RecipeAiDoc doc,
            int score,
            String reason
    ) {
    }

    public record RecommendationReadiness(
            boolean ready,
            long documentCount,
            Long sampleRecipeId,
            String message
    ) {
        public static RecommendationReadiness ready(long documentCount, Long sampleRecipeId) {
            return new RecommendationReadiness(true, Math.max(documentCount, 0L), sampleRecipeId, null);
        }

        public static RecommendationReadiness notReady(long documentCount, Long sampleRecipeId, String message) {
            return new RecommendationReadiness(false, Math.max(documentCount, 0L), sampleRecipeId, message);
        }
    }
}

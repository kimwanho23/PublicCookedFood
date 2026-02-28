package kwh.PublicCookedFood.food.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.regex.Pattern;

@Getter
@Setter
@NoArgsConstructor
public class RecipeAiRecommendRequest {

    private static final Pattern TERM_SPLIT_PATTERN = Pattern.compile("\\s+");

    @Size(max = 20, message = "포함 재료는 최대 20개까지 입력할 수 있습니다.")
    private List<@Size(max = 50, message = "포함 재료는 50자 이하로 입력해주세요.") String> includeIngredients;

    @Size(max = 20, message = "제외 재료는 최대 20개까지 입력할 수 있습니다.")
    private List<@Size(max = 50, message = "제외 재료는 50자 이하로 입력해주세요.") String> excludeIngredients;

    @Size(max = 10, message = "선호 분류는 최대 10개까지 입력할 수 있습니다.")
    private List<@Size(max = 50, message = "선호 분류는 50자 이하로 입력해주세요.") String> preferredTypes;

    @Size(max = 10, message = "선호 국가 분류는 최대 10개까지 입력할 수 있습니다.")
    private List<@Size(max = 50, message = "선호 국가 분류는 50자 이하로 입력해주세요.") String> preferredNations;

    @Size(max = 30, message = "선호 난이도는 30자 이하로 입력해주세요.")
    private String preferredLevel;

    @Min(value = 1, message = "최대 조리시간은 1분 이상이어야 합니다.")
    @Max(value = 600, message = "최대 조리시간은 600분 이하로 입력해주세요.")
    private Integer maxCookingMinutes;

    @Min(value = 0, message = "최대 칼로리는 0 이상이어야 합니다.")
    @Max(value = 5000, message = "최대 칼로리는 5000kcal 이하로 입력해주세요.")
    private Integer maxCalorieKcal;

    @Min(value = 1, message = "인분은 1 이상이어야 합니다.")
    @Max(value = 20, message = "인분은 20 이하로 입력해주세요.")
    private Integer servings;

    @Size(max = 200, message = "요청 문장은 200자 이하로 입력해주세요.")
    private String query;

    @Min(value = 1, message = "추천 개수는 1 이상이어야 합니다.")
    @Max(value = 20, message = "추천 개수는 20 이하로 입력해주세요.")
    private Integer limit;

    public List<String> getIncludeIngredients() {
        return includeIngredients == null ? null : Collections.unmodifiableList(includeIngredients);
    }

    public void setIncludeIngredients(List<String> includeIngredients) {
        this.includeIngredients = toMutableCopy(includeIngredients);
    }

    public List<String> normalizedIncludeIngredients() {
        return normalizeList(includeIngredients);
    }

    public List<String> getExcludeIngredients() {
        return excludeIngredients == null ? null : Collections.unmodifiableList(excludeIngredients);
    }

    public void setExcludeIngredients(List<String> excludeIngredients) {
        this.excludeIngredients = toMutableCopy(excludeIngredients);
    }

    public List<String> normalizedExcludeIngredients() {
        return normalizeList(excludeIngredients);
    }

    public List<String> getPreferredTypes() {
        return preferredTypes == null ? null : Collections.unmodifiableList(preferredTypes);
    }

    public void setPreferredTypes(List<String> preferredTypes) {
        this.preferredTypes = toMutableCopy(preferredTypes);
    }

    public List<String> normalizedPreferredTypes() {
        return normalizeList(preferredTypes);
    }

    public List<String> getPreferredNations() {
        return preferredNations == null ? null : Collections.unmodifiableList(preferredNations);
    }

    public void setPreferredNations(List<String> preferredNations) {
        this.preferredNations = toMutableCopy(preferredNations);
    }

    public List<String> normalizedPreferredNations() {
        return normalizeList(preferredNations);
    }

    public String normalizedPreferredLevel() {
        return normalize(preferredLevel);
    }

    public String normalizedQuery() {
        return normalize(query);
    }

    public List<String> normalizedQueryTerms() {
        String normalized = normalizedQuery();
        if (normalized == null) {
            return List.of();
        }
        return TERM_SPLIT_PATTERN.splitAsStream(normalized)
                .map(String::trim)
                .filter(term -> !term.isEmpty())
                .map(term -> term.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    public int normalizedLimit() {
        if (limit == null) {
            return 5;
        }
        return Math.min(Math.max(limit, 1), 20);
    }

    private String normalize(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private List<String> normalizeList(List<String> values) {
        if (values == null) {
            return List.of();
        }
        return values.stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isEmpty())
                .map(value -> value.toLowerCase(Locale.ROOT))
                .distinct()
                .toList();
    }

    private List<String> toMutableCopy(List<String> values) {
        if (values == null) {
            return null;
        }
        return new ArrayList<>(values);
    }
}
